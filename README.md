# SenseiSU

面向 Android 的 root 管理器，支持 **LKM**（模块加载）与 **GKI 整包**
（内核内置）两种集成方式。

> 独立分支项目，GPL-2.0 授权。上游致谢见 [NOTICE](NOTICE)。

## 特性

- 内核级 su 授权，按应用粒度配置权限
- 挂载视图隐藏、SELinux 规则隐藏、AVC 日志伪装
- 管理器包名严格校验（只接受 ASCII 可见字符，拒绝冒充）
- **SUSFS 支持**（LKM 模式，通过 [独立模块](https://github.com/kylinmode/SUSFS-Guard-LKM) 加载）

## 安装

### 方式一：修补镜像（无需 root）

1. 从官方固件提取 `init_boot.img`（Android 13+）或 `boot.img`
2. 打开 SenseiSU → 安装 → 选择文件并修补
3. 选择对应 KMI
4. 修补完成后导出，用 fastboot 刷入

```sh
fastboot flash init_boot ssu_patched.img
fastboot reboot
```

### 方式二：AnyKernel3 整包

```sh
fastboot flash boot SSU-<kmi>.zip
```

也可通过管理器的「一键刷入」完成。刷之前请备份原始镜像以便回滚。

## SUSFS 支持（LKM 模式）

SenseiSU 采用 **LKM** 方式运行，SUSFS 不在内核里编译，而是以**独立内核模块**的形式加载：

```
SenseiSU 驱动（kernelsu.ko）
    └── susfs_guard_lkm.ko   ← 按 KMI 单独编译，作为普通模块刷入
```

这意味着：

| 特性 | 说明 |
|---|---|
| 内核保持原厂 | 不需要编译或替换整个内核，OTA 后只重刷 `init_boot` |
| SUSFS 按需启用 | 不装模块就没有 SUSFS，装了才接管挂载/路径隐藏 |
| 按 KMI 分发 | 每个内核版本一个包，装错版本会拒绝加载 |

### 获取 SUSFS 模块

模块由 **独立仓库构建与分发**，源码与产物都在这里：

> **[kylinmode/SUSFS-Guard-LKM](https://github.com/kylinmode/SUSFS-Guard-LKM)**

下载地址（按内核选一个）：

<https://github.com/kylinmode/SUSFS-Guard-LKM/releases>

| 内核 | 对应包 |
|---|---|
| 5.10（Android 12/13） | `SUSFS-Guard-android12-5.10.zip` / `android13-5.10.zip` |
| 5.15（Android 13/14） | `SUSFS-Guard-android13-5.15.zip` / `android14-5.15.zip` |
| 6.1（Android 14） | `SUSFS-Guard-android14-6.1.zip` |
| 6.6（Android 15） | `SUSFS-Guard-android15-6.6.zip` |
| 6.12（Android 16） | `SUSFS-Guard-android16-6.12.zip` |
| 6.18（Android 17） | `SUSFS-Guard-android17-6.18.zip` |

### 安装

1. 管理器 → 模块 → 从本地安装
2. 选对应 KMI 的 zip
3. 重启
4. 首页显示 `SUSFS: LKM 模块` 即生效

也可以在设置页的「加载 SUSFS 内核模块」里手动加载 / 打开开机自动加载。

### 状态怎么看

| 显示 | 含义 |
|---|---|
| `SUSFS: LKM 模块` | 已通过独立模块加载，正常 |
| `SUSFS: 内核内置` | 走的是 GKI 整包路线 |
| `未检测到` | 没有加载 SUSFS，隐藏类功能不生效 |

## 构建

需要：

- JDK 21
- Android SDK（platform 37、build-tools 37.0.0）
- Android NDK 29.0.14206865
- Rust（stable，`aarch64-linux-android` target）
- 对应 KMI 的内核源码

```bash
# 管理器 APK
SSU_STORE_PASSWORD=你的口令 ./scripts/ssu-build-apk.sh

# LKM 模块（单个 KMI / 全部）
./scripts/ssu-kmod.sh <kmi>
./scripts/ssu-build-all-kmi.sh

# GKI 整包（AnyKernel3）
./scripts/ssu-gki-ak3.sh build+package <kmi> <内核源码路径>
```

详细步骤见 `docs/`。

## 项目结构

```
kernel/     内核驱动（C）
userspace/  ksud、ksuinit（Rust）
manager/    管理器（Kotlin + Compose）
scripts/    构建与打包脚本
docs/       文档
```

## 致谢

本项目的内核驱动与用户态工具在实现上受益于以下开源项目：

- [KernelSU](https://github.com/tiann/KernelSU) — su 授权模型与模块系统
- [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) — kprobe 钩子架构与扩展框架
- [SukiSU](https://github.com/SukiSU-Ultra/SukiSU-Ultra) — 隐藏特性与构建流程

上述项目的版权归其各自作者所有，相关代码依 GPL-2.0 使用。
SenseiSU 为独立分支，与上述项目无隶属关系。

---

最后，感谢 GitHub 上所有不计报酬、只为分享而写代码的人。
没有你们，这个项目不会存在。

## 许可

GPL-2.0，见 [LICENSE](LICENSE)。
