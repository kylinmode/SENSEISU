use std::ffi::{CStr, CString};
use anyhow::{Context, Result, anyhow, ensure};
use libc::{syscall, SYS_reboot, c_char};
use log::{info, warn};

// Constants from susfsd.c
const KSU_INSTALL_MAGIC1: u64 = 0xDEADBEEF;
const SUSFS_MAGIC: u64 = 0xFAFAFAFA;

const CMD_SUSFS_SHOW_VERSION: u64 = 0x555e1;
const CMD_SUSFS_SHOW_ENABLED_FEATURES: u64 = 0x555e2;
const CMD_SUSFS_SHOW_VARIANT: u64 = 0x555e3;

const SUSFS_ENABLED_FEATURES_SIZE: usize = 8192;
const SUSFS_MAX_VERSION_BUFSIZE: usize = 16;
const SUSFS_MAX_VARIANT_BUFSIZE: usize = 16;

const CMD_SUSFS_ADD_SUS_PATH: u64 = 0x55550;
const CMD_SUSFS_SET_UNAME: u64 = 0x55590;
const CMD_SUSFS_ENABLE_LOG: u64 = 0x555a0;
const CMD_SUSFS_SET_CMDLINE_OR_BOOTCONFIG: u64 = 0x555b0;
const CMD_SUSFS_HIDE_SUS_MNTS_FOR_NON_SU_PROCS: u64 = 0x55561;
const CMD_SUSFS_ENABLE_AVC_LOG_SPOOFING: u64 = 0x60010;

const SUSFS_MAX_LEN_PATHNAME: usize = 256;
const SUSFS_MAX_UTS_LEN: usize = 64;
const SUSFS_FAKE_CMDLINE_SIZE: usize = 8192;

const ERR_CMD_NOT_SUPPORTED: i32 = 126;
const AUTOLOAD_CONFIG_MODULE_ID: &str = "internal.ksud.susfsd";
const AUTOLOAD_ENABLED_CONFIG_KEY: &str = "autoload.enabled";
const AUTOLOAD_FALLBACK_KMI_CONFIG_KEY: &str = "autoload.fallback_kmi";

struct AutoLoadConfig {
    enabled: bool,
    fallback_kmi: Option<String>,
}

fn validate_kmi(kmi: &str) -> Result<()> {
    ensure!(
        !kmi.is_empty()
            && kmi.len() <= 64
            && kmi.bytes().all(|byte| {
                byte.is_ascii_alphanumeric() || matches!(byte, b'.' | b'_' | b'-')
            }),
        "Invalid KMI '{kmi}'"
    );
    Ok(())
}

fn read_autoload_config() -> Result<AutoLoadConfig> {
    let enabled = crate::module_config::get_config_value(
        AUTOLOAD_CONFIG_MODULE_ID,
        AUTOLOAD_ENABLED_CONFIG_KEY,
        crate::module_config::ConfigType::Persist,
    )?;
    let fallback_kmi = crate::module_config::get_config_value(
        AUTOLOAD_CONFIG_MODULE_ID,
        AUTOLOAD_FALLBACK_KMI_CONFIG_KEY,
        crate::module_config::ConfigType::Persist,
    )?;
    if let Some(kmi) = &fallback_kmi {
        validate_kmi(kmi)?;
    }

    Ok(AutoLoadConfig {
        enabled: enabled
            .as_deref()
            .is_some_and(crate::module_config::parse_bool_config),
        fallback_kmi,
    })
}

pub fn autoload_enabled() -> Result<bool> {
    Ok(read_autoload_config()?.enabled)
}

pub fn set_autoload(enabled: bool, kmi: Option<&str>) -> Result<()> {
    if !enabled {
        crate::module_config::set_config_value(
            AUTOLOAD_CONFIG_MODULE_ID,
            AUTOLOAD_ENABLED_CONFIG_KEY,
            "false",
            crate::module_config::ConfigType::Persist,
        )
        .context("Failed to disable SUSFS auto-load")?;
        println!("SUSFS auto-load disabled");
        return Ok(());
    }

    anyhow::ensure!(
        cfg!(target_arch = "aarch64"),
        "SUSFS guard LKM is only available for arm64"
    );

    let kmi = match kmi {
        Some(kmi) => kmi.to_owned(),
        None => crate::boot_patch::get_current_kmi().context(
            "Failed to detect kernel KMI; specify it with `ksud susfs autoload enable --kmi <KMI>`",
        )?,
    };
    validate_kmi(&kmi)?;
    let asset_name = format!("{kmi}_susfs_guard_lkm.ko");
    crate::assets::get_asset_data(&asset_name)
        .with_context(|| format!("SUSFS guard asset is unavailable for KMI {kmi}"))?;

    crate::module_config::set_config_value(
        AUTOLOAD_CONFIG_MODULE_ID,
        AUTOLOAD_FALLBACK_KMI_CONFIG_KEY,
        &kmi,
        crate::module_config::ConfigType::Persist,
    )
    .context("Failed to save SUSFS auto-load config")?;
    crate::module_config::set_config_value(
        AUTOLOAD_CONFIG_MODULE_ID,
        AUTOLOAD_ENABLED_CONFIG_KEY,
        "true",
        crate::module_config::ConfigType::Persist,
    )
    .context("Failed to save SUSFS auto-load config")?;

    println!("SUSFS auto-load enabled (KMI fallback: {kmi})");
    Ok(())
}

fn detected_susfs_version() -> Option<String> {
    let mut cmd = SusfsVersion {
        version: [0; SUSFS_MAX_VERSION_BUFSIZE],
        err: ERR_CMD_NOT_SUPPORTED,
    };
    unsafe {
        syscall(
            SYS_reboot,
            KSU_INSTALL_MAGIC1,
            SUSFS_MAGIC,
            CMD_SUSFS_SHOW_VERSION,
            &mut cmd as *mut _,
        );
    }
    if cmd.err != 0 {
        return None;
    }

    let length = cmd
        .version
        .iter()
        .position(|byte| *byte == 0)
        .unwrap_or(cmd.version.len());
    (length != 0).then(|| String::from_utf8_lossy(&cmd.version[..length]).into_owned())
}

pub fn load_if_autoload_enabled() -> Result<()> {
    let config = read_autoload_config()?;
    if !config.enabled {
        return Ok(());
    }
    if !cfg!(target_arch = "aarch64") {
        warn!("SUSFS auto-load is enabled, but the SUSFS LKM is only available for arm64");
        return Ok(());
    }
    if let Some(version) = detected_susfs_version() {
        info!("SUSFS {version} is already available; skipping guard LKM load");
        return Ok(());
    }

    let kmi = crate::boot_patch::get_current_kmi()
        .ok()
        .or(config.fallback_kmi);
    let Some(kmi) = kmi else {
        warn!("SUSFS auto-load is enabled, but KMI detection failed and no fallback KMI is saved");
        return Ok(());
    };

    if let Err(error) = load(Some(&kmi)) {
        warn!("Failed to auto-load SUSFS guard LKM for KMI {kmi}: {error:#}");
    }
    Ok(())
}

pub fn load(kmi: Option<&str>) -> Result<()> {
    anyhow::ensure!(
        cfg!(target_arch = "aarch64"),
        "SUSFS guard LKM is only available for arm64"
    );

    if let Some(version) = detected_susfs_version() {
        info!("SUSFS {version} is already available; skipping guard LKM load");
        println!("SUSFS {version} is already loaded");
        return Ok(());
    }

    let kmi = match kmi {
        Some(kmi) => kmi.to_owned(),
        None => crate::boot_patch::get_current_kmi().context(
            "Failed to detect kernel KMI; specify it explicitly with `ksud susfs load --kmi <KMI>`",
        )?,
    };

    let asset_name = format!("{kmi}_susfs_guard_lkm.ko");
    let module_data = crate::assets::get_asset_data(&asset_name)
        .with_context(|| format!("Failed to load SUSFS guard asset {asset_name}"))?;
    let params = CString::new("").expect("empty module parameters are valid");

    info!("Loading SUSFS guard LKM for KMI {kmi}");
    ksuinit::load_module(&module_data, &params).context("Failed to load SUSFS guard LKM")?;
    println!("Loaded SUSFS guard LKM for KMI {kmi}");
    Ok(())
}

pub fn unload() -> Result<()> {
    rustix::system::delete_module(c"susfs_guard_lkm", 0)
        .context("Failed to unload SUSFS guard LKM")?;
    println!("Unloaded SUSFS guard LKM");
    Ok(())
}

#[repr(C)]
struct SusfsVersion {
    version: [u8; SUSFS_MAX_VERSION_BUFSIZE],
    err: i32,
}

#[repr(C)]
struct SusfsVariant {
    variant: [u8; SUSFS_MAX_VARIANT_BUFSIZE],
    err: i32,
}

#[repr(C)]
struct SusfsEnabledFeatures {
    features: [u8; SUSFS_ENABLED_FEATURES_SIZE],
    err: i32,
}

pub fn show_version() -> Result<()> {
    let mut cmd = SusfsVersion {
        version: [0; SUSFS_MAX_VERSION_BUFSIZE],
        err: ERR_CMD_NOT_SUPPORTED,
    };

    unsafe {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, SUSFS_MAGIC, CMD_SUSFS_SHOW_VERSION, &mut cmd as *mut _);
    }

    check_unsupported(cmd.err, CMD_SUSFS_SHOW_VERSION)?;

    if cmd.err == 0 {
        let version = unsafe { CStr::from_ptr(cmd.version.as_ptr() as *const c_char) }.to_string_lossy();
        println!("{}", version);
        Ok(())
    } else {
        Err(anyhow!("Invalid (Error: {})", cmd.err))
    }
}

pub fn show_variant() -> Result<()> {
    let mut cmd = SusfsVariant {
        variant: [0; SUSFS_MAX_VARIANT_BUFSIZE],
        err: ERR_CMD_NOT_SUPPORTED,
    };

    unsafe {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, SUSFS_MAGIC, CMD_SUSFS_SHOW_VARIANT, &mut cmd as *mut _);
    }

    check_unsupported(cmd.err, CMD_SUSFS_SHOW_VARIANT)?;

    if cmd.err == 0 {
        let variant = unsafe { CStr::from_ptr(cmd.variant.as_ptr() as *const c_char) }.to_string_lossy();
        println!("{}", variant);
        Ok(())
    } else {
        Err(anyhow!("Invalid (Error: {})", cmd.err))
    }
}

pub fn show_features(check_only: bool) -> Result<()> {
    let mut cmd = SusfsEnabledFeatures {
        features: [0; SUSFS_ENABLED_FEATURES_SIZE],
        err: ERR_CMD_NOT_SUPPORTED,
    };

    unsafe {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, SUSFS_MAGIC, CMD_SUSFS_SHOW_ENABLED_FEATURES, &mut cmd as *mut _);
    }

    check_unsupported(cmd.err, CMD_SUSFS_SHOW_ENABLED_FEATURES)?;

    let features_cstr = unsafe { CStr::from_ptr(cmd.features.as_ptr() as *const c_char) };
    let has_features = cmd.err == 0 && !features_cstr.to_bytes().is_empty();

    if check_only {
        if has_features {
            println!("Supported");
            Ok(())
        } else {
            Err(anyhow!("Unsupported"))
        }
    } else if has_features {
        print!("{}", features_cstr.to_string_lossy());
        Ok(())
    } else {
        Err(anyhow!("Invalid (Error: {})", cmd.err))
    }
}

fn check_unsupported(err: i32, cmd: u64) -> Result<()> {
    if err == ERR_CMD_NOT_SUPPORTED {
        return Err(anyhow!("CMD: '0x{:x}', SUSFS operation not supported, please enable it in kernel", cmd));
    }
    Ok(())
}


// ── 配置项（与内核 supercall 对接）───────────────────────────
const CFG_SUS_PATHS: &str = "paths";
const CFG_UNAME_RELEASE: &str = "uname_release";
const CFG_UNAME_VERSION: &str = "uname_version";
const CFG_CMDLINE: &str = "cmdline";
const CFG_AVC_LOG_SPOOF: &str = "avc_log_spoof";
const CFG_LOG_ENABLED: &str = "log_enabled";
const CFG_HIDE_SUS_MNTS: &str = "hide_sus_mnts";

#[repr(C)]
struct SusPath {
    path: [u8; SUSFS_MAX_LEN_PATHNAME],
    err: i32,
}

#[repr(C)]
struct SusUname {
    release: [u8; SUSFS_MAX_UTS_LEN + 1],
    version: [u8; SUSFS_MAX_UTS_LEN + 1],
    err: i32,
}

#[repr(C)]
struct SusBool {
    enabled: bool,
    err: i32,
}

#[repr(C)]
struct SusCmdline {
    fake: [u8; SUSFS_FAKE_CMDLINE_SIZE],
    err: i32,
}

const SUSFS_CONF_PATH: &str = "/data/ssu/susfs.conf";
const SUSFS_CONF_LEGACY: &str = "/data/adb/ssu/susfs.conf";

fn conf_path() -> &'static str { SUSFS_CONF_PATH }

/// 读取本地配置文件（key=value 逐行，支持 paths 多行用 | 连接）
fn read_conf() -> std::collections::BTreeMap<String, String> {
    let mut map = std::collections::BTreeMap::new();
    // 首次运行：把旧位置（/data/adb/ssu）的配置迁过来
    if !std::path::Path::new(conf_path()).exists() {
        if let Ok(old) = std::fs::read_to_string(SUSFS_CONF_LEGACY) {
            if let Some(dir) = std::path::Path::new(conf_path()).parent() {
                std::fs::create_dir_all(dir).ok();
            }
            std::fs::write(conf_path(), &old).ok();
        }
    }
    let Ok(text) = std::fs::read_to_string(conf_path()) else { return map };
    for line in text.lines() {
        let line = line.trim();
        if line.is_empty() || line.starts_with('#') { continue; }
        if let Some((k, v)) = line.split_once('=') {
            map.insert(k.trim().to_owned(), v.trim().to_owned());
        }
    }
    map
}

fn write_conf(map: &std::collections::BTreeMap<String, String>) -> Result<()> {
    if let Some(dir) = std::path::Path::new(conf_path()).parent() {
        std::fs::create_dir_all(dir).ok();
    }
    let mut out = String::from("# SenseiSU SUSFS 配置（管理器编辑，ksud susfs apply 生效）\n");
    for (k, v) in map { out.push_str(&format!("{k}={v}\n")); }
    std::fs::write(conf_path(), out).context("写入 susfs.conf 失败")?;
    Ok(())
}

fn read_cfg(key: &str) -> Option<String> {
    read_conf().get(key).cloned().filter(|v| !v.is_empty())
}

fn write_cfg(key: &str, value: &str) -> Result<()> {
    let mut map = read_conf();
    map.insert(key.to_owned(), value.to_owned());
    write_conf(&map)
}

fn cfg_bool(key: &str) -> bool {
    read_cfg(key)
        .as_deref()
        .is_some_and(crate::module_config::parse_bool_config)
}

fn add_sus_path_syscall(path: &str) -> Result<()> {
    ensure!(!path.is_empty() && path.len() < SUSFS_MAX_LEN_PATHNAME, "路径过长或为空");
    let mut cmd = SusPath { path: [0; SUSFS_MAX_LEN_PATHNAME], err: ERR_CMD_NOT_SUPPORTED };
    let bytes = path.as_bytes();
    cmd.path[..bytes.len()].copy_from_slice(bytes);
    unsafe {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, SUSFS_MAGIC, CMD_SUSFS_ADD_SUS_PATH, &mut cmd as *mut _);
    }
    check_unsupported(cmd.err, CMD_SUSFS_ADD_SUS_PATH)?;
    ensure!(cmd.err == 0, "内核拒绝该路径（错误码 {}）", cmd.err);
    Ok(())
}

fn set_uname_syscall(release: &str, version: &str) -> Result<()> {
    let mut cmd = SusUname {
        release: [0; SUSFS_MAX_UTS_LEN + 1],
        version: [0; SUSFS_MAX_UTS_LEN + 1],
        err: ERR_CMD_NOT_SUPPORTED,
    };
    let r = release.as_bytes();
    let v = version.as_bytes();
    ensure!(r.len() <= SUSFS_MAX_UTS_LEN, "release 不能超过 64 字节");
    ensure!(v.len() <= SUSFS_MAX_UTS_LEN, "version 不能超过 64 字节");
    cmd.release[..r.len()].copy_from_slice(r);
    cmd.version[..v.len()].copy_from_slice(v);
    unsafe {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, SUSFS_MAGIC, CMD_SUSFS_SET_UNAME, &mut cmd as *mut _);
    }
    check_unsupported(cmd.err, CMD_SUSFS_SET_UNAME)?;
    ensure!(cmd.err == 0, "设置 uname 失败（错误码 {}）", cmd.err);
    Ok(())
}

fn set_cmdline_syscall(value: &str) -> Result<()> {
    ensure!(value.len() < SUSFS_FAKE_CMDLINE_SIZE, "cmdline 过长");
    let mut cmd = SusCmdline { fake: [0; SUSFS_FAKE_CMDLINE_SIZE], err: ERR_CMD_NOT_SUPPORTED };
    let b = value.as_bytes();
    cmd.fake[..b.len()].copy_from_slice(b);
    unsafe {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, SUSFS_MAGIC, CMD_SUSFS_SET_CMDLINE_OR_BOOTCONFIG, &mut cmd as *mut _);
    }
    check_unsupported(cmd.err, CMD_SUSFS_SET_CMDLINE_OR_BOOTCONFIG)?;
    ensure!(cmd.err == 0, "设置 cmdline 失败（错误码 {}）", cmd.err);
    Ok(())
}

fn set_bool_syscall(cmd_id: u64, enabled: bool, label: &str) -> Result<()> {
    let mut cmd = SusBool { enabled, err: ERR_CMD_NOT_SUPPORTED };
    unsafe {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, SUSFS_MAGIC, cmd_id, &mut cmd as *mut _);
    }
    check_unsupported(cmd.err, cmd_id)?;
    ensure!(cmd.err == 0, "设置 {label} 失败（错误码 {}）", cmd.err);
    Ok(())
}

// ── 对管理器的公开操作 ──────────────────────────────────────
pub fn list_paths() -> Result<()> {
    for path in read_cfg(CFG_SUS_PATHS).unwrap_or_default().split('|').filter(|l| !l.is_empty()) {
        println!("{path}");
    }
    Ok(())
}

pub fn add_path(path: &str) -> Result<()> {
    let path = path.trim();
    ensure!(path.starts_with('/'), "路径必须以 / 开头");
    let mut list: Vec<String> = read_cfg(CFG_SUS_PATHS)
        .unwrap_or_default()
        .split('|')
        .filter(|l| !l.is_empty())
        .map(str::to_owned)
        .collect();
    if !list.iter().any(|p| p == path) {
        list.push(path.to_owned());
        write_cfg(CFG_SUS_PATHS, &list.join("|"))?;
    }
    add_sus_path_syscall(path)?;
    println!("已隐藏：{path}");
    Ok(())
}

pub fn remove_path(path: &str) -> Result<()> {
    let path = path.trim();
    let list: Vec<String> = read_cfg(CFG_SUS_PATHS)
        .unwrap_or_default()
        .split('|')
        .filter(|l| !l.is_empty() && *l != path)
        .map(str::to_owned)
        .collect();
    write_cfg(CFG_SUS_PATHS, &list.join("|"))?;
    println!("已从配置移除（重启后生效）：{path}");
    Ok(())
}

pub fn set_uname(release: &str, version: &str) -> Result<()> {
    write_cfg(CFG_UNAME_RELEASE, release)?;
    write_cfg(CFG_UNAME_VERSION, version)?;
    set_uname_syscall(release, version)?;
    println!("uname 伪装已更新");
    Ok(())
}

pub fn set_cmdline(value: &str) -> Result<()> {
    write_cfg(CFG_CMDLINE, value)?;
    set_cmdline_syscall(value)?;
    println!("cmdline 伪装已更新");
    Ok(())
}

pub fn set_avc_log_spoof(enabled: bool) -> Result<()> {
    write_cfg(CFG_AVC_LOG_SPOOF, if enabled { "true" } else { "false" })?;
    set_bool_syscall(CMD_SUSFS_ENABLE_AVC_LOG_SPOOFING, enabled, "AVC 日志伪装")?;
    println!("AVC 日志伪装：{}", if enabled { "开" } else { "关" });
    Ok(())
}

pub fn set_log_enabled(enabled: bool) -> Result<()> {
    write_cfg(CFG_LOG_ENABLED, if enabled { "true" } else { "false" })?;
    set_bool_syscall(CMD_SUSFS_ENABLE_LOG, enabled, "SUSFS 日志")?;
    println!("SUSFS 日志：{}", if enabled { "开" } else { "关" });
    Ok(())
}

pub fn set_hide_sus_mnts(enabled: bool) -> Result<()> {
    write_cfg(CFG_HIDE_SUS_MNTS, if enabled { "true" } else { "false" })?;
    set_bool_syscall(CMD_SUSFS_HIDE_SUS_MNTS_FOR_NON_SU_PROCS, enabled, "挂载隐藏")?;
    println!("非 root 进程挂载隐藏：{}", if enabled { "开" } else { "关" });
    Ok(())
}

/// 把已保存的配置全部下发到内核（开机 & 手动应用）
pub fn apply_saved_config() -> Result<()> {
    if detected_susfs_version().is_none() {
        return Ok(());
    }
    if let Some(paths) = read_cfg(CFG_SUS_PATHS) {
        for path in paths.split('|').filter(|l| !l.is_empty()) {
            let _ = add_sus_path_syscall(path);
        }
    }
    let rel = read_cfg(CFG_UNAME_RELEASE).unwrap_or_default();
    let ver = read_cfg(CFG_UNAME_VERSION).unwrap_or_default();
    if !rel.is_empty() {
        let _ = set_uname_syscall(&rel, &ver);
    }
    if let Some(cmdline) = read_cfg(CFG_CMDLINE).filter(|c| !c.is_empty()) {
        let _ = set_cmdline_syscall(&cmdline);
    }
    let _ = set_bool_syscall(CMD_SUSFS_ENABLE_AVC_LOG_SPOOFING, cfg_bool(CFG_AVC_LOG_SPOOF), "AVC 日志伪装");
    let _ = set_bool_syscall(CMD_SUSFS_ENABLE_LOG, cfg_bool(CFG_LOG_ENABLED), "SUSFS 日志");
    let _ = set_bool_syscall(CMD_SUSFS_HIDE_SUS_MNTS_FOR_NON_SU_PROCS, cfg_bool(CFG_HIDE_SUS_MNTS), "挂载隐藏");
    Ok(())
}

/// 输出管理器需要的配置快照
pub fn show_config() -> Result<()> {
    println!("paths={}", read_cfg(CFG_SUS_PATHS).unwrap_or_default());
    println!("uname_release={}", read_cfg(CFG_UNAME_RELEASE).unwrap_or_default());
    println!("uname_version={}", read_cfg(CFG_UNAME_VERSION).unwrap_or_default());
    println!("cmdline={}", read_cfg(CFG_CMDLINE).unwrap_or_default());
    println!("avc_log_spoof={}", cfg_bool(CFG_AVC_LOG_SPOOF));
    println!("log_enabled={}", cfg_bool(CFG_LOG_ENABLED));
    println!("hide_sus_mnts={}", cfg_bool(CFG_HIDE_SUS_MNTS));
    Ok(())
}
