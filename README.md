# SenseiSU (SSU)

面向 Android 的 root 管理器，基于 KernelSU 构建，支持 **LKM 模块**与**内核内置**两种集成方式。

An Android root manager built on KernelSU, supporting both **LKM (module)** and **built-in (kernel)** integration.

> 独立分支项目 / Independent fork — GPL-2.0. 上游致谢见 [NOTICE](NOTICE)。

---

## 中文

### 特性

- 内核级 `su` 授权，按应用粒度配置权限
- 挂载视图隐藏、SELinux 规则隐藏、AVC 日志伪装
- 支持第三方 SUSFS 内核模块（LKM 模式）
- 六种 KMI 适配：`5.10 / 5.15 / 6.1 / 6.6 / 6.12 / 6.18`
- Sensei 风格界面（自绘主题、立绘、状态卡片）

### 支持的 KMI

| KMI | 内核版本 |
|---|---|
| android12-5.10 | 5.10.x |
| android13-5.15 | 5.15.x |
| android14-6.1 | 6.1.x |
| android15-6.6 | 6.6.x |
| android16-6.12 | 6.12.x |
| android17-6.18 | 6.18.x |

同一 KMI 下一个 `ko` 覆盖该系列所有小版本（加载时自动适配 vermagic）。

### 安装

**方式一：修补镜像（推荐）**

1. 从官方固件提取 `init_boot.img`（Android 13+）或 `boot.img`
2. 打开 SenseiSU → 安装 → 选择文件并修补
3. 选择对应 KMI
4. 导出后用 fastboot 刷入

```sh
fastboot flash init_boot ssu_patched.img
fastboot reboot
```

**方式二：刷入 LKM 模块**

把 `ssu-<kmi>.ko` 打包成模块 zip，在管理器「模块」页安装后重启。

### SUSFS

LKM 模式下可直接使用社区提供的 SUSFS 内核模块（`susfs_guard_lkm`），
刷入后管理器首页会显示 `SUSFS: LKM 模块`。

### 构建

需要 JDK 21、Android SDK（platform 36 / build-tools 37.0.0）、NDK 29、Rust stable。

```bash
# 静态 ksuinit（必须静态，否则无法开机）
./scripts/ssu-ksuinit.sh aarch64-linux-android

# 管理器 APK
cd manager && ./gradlew :app:assembleRelease

# 内核模块（LKM）
make -C <kernel-src> ARCH=arm64 LLVM=1 O=out KSU_EXPECTED_SIZE=0x308 \
     KSU_EXPECTED_HASH=<你的证书哈希> drivers/kernelsu/kernelsu.ko
```

### 致谢

本项目的内核驱动与用户态工具受益于以下开源项目，版权归其各自作者所有：

- KernelSU
- KernelSU-Next
- SukiSU

本项目为独立分支，与上述项目无隶属关系。

最后，感谢所有不计报酬、只为分享而写代码的人。

---

## English

### Features

- Kernel-level `su` with per-app policy
- Mount view hiding, SELinux rule hiding, AVC log spoofing
- Works with third-party SUSFS kernel modules (LKM mode)
- Six KMIs: `5.10 / 5.15 / 6.1 / 6.6 / 6.12 / 6.18`
- Sensei-themed UI (custom theme, artwork, status card)

### Supported KMIs

| KMI | Kernel |
|---|---|
| android12-5.10 | 5.10.x |
| android13-5.15 | 5.15.x |
| android14-6.1 | 6.1.x |
| android15-6.6 | 6.6.x |
| android16-6.12 | 6.12.x |
| android17-6.18 | 6.18.x |

One `ko` per KMI covers every minor release of that series; vermagic is adapted at load time.

### Installation

**Option 1 — patch boot image (recommended)**

1. Extract `init_boot.img` (Android 13+) or `boot.img` from stock firmware
2. Open SenseiSU → Install → Select file and patch
3. Pick the matching KMI
4. Flash the result

```sh
fastboot flash init_boot ssu_patched.img
fastboot reboot
```

**Option 2 — LKM module**

Package `ssu-<kmi>.ko` as a module zip, install it from the Modules page, reboot.

### SUSFS

In LKM mode you can use community SUSFS kernel modules (`susfs_guard_lkm`).
Once loaded, the manager home screen shows `SUSFS: LKM module`.

### Building

Requires JDK 21, Android SDK (platform 36 / build-tools 37.0.0), NDK 29, Rust stable.

```bash
./scripts/ssu-ksuinit.sh aarch64-linux-android   # must be static
cd manager && ./gradlew :app:assembleRelease
```

### Credits

Kernel driver and userspace tooling build upon the following projects,
each copyrighted by its own authors:

- KernelSU
- KernelSU-Next
- SukiSU

This is an independent fork, not affiliated with the projects above.

Finally, thanks to everyone who writes and shares code without asking for payment.

---

## 许可 / License

GPL-2.0 — 见 [LICENSE](LICENSE) / see [LICENSE](LICENSE).
