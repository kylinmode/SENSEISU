# SenseiSU

面向 Android 的 root 管理器，支持 **LKM**（模块加载）与 **GKI 整包**
（内核内置）两种集成方式。

> 独立分支项目，GPL-2.0 授权。上游致谢见 [NOTICE](NOTICE)。

## 特性

- 内核级 su 授权，按应用粒度配置权限
- 挂载视图隐藏、SELinux 规则隐藏、AVC 日志伪装
- 管理器包名严格校验（只接受 ASCII 可见字符，拒绝冒充）

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
