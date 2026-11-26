package com.senseisu.ssu.ui.util

import com.topjohnwu.superuser.Shell

/**
 * Sensei UI 需要的兼容层。
 *
 * 旧树（SukiSU-Nova）有这些工具函数，KernelSU-Next 没有，
 * 这里按同样的语义补上，避免 UI 代码大改。
 */

/** 取 root shell；globalMnt = true 时用 mount-master（-g） */
fun getRootShell(globalMnt: Boolean = false): Shell = createRootShell(globalMnt)

/** 同步获取当前内核 KMI，形如 android15-6.6 */
fun getCurrentKmiBlocking(): String {
    val release = System.getProperty("os.version").orEmpty()
    val ver = Regex("([0-9]+\\.[0-9]+)").find(release)?.groupValues?.get(1) ?: "6.6"
    val android = when {
        ver.startsWith("6.18") -> "android17"
        ver.startsWith("6.12") -> "android16"
        ver.startsWith("6.6") -> "android15"
        ver.startsWith("6.1") -> "android14"
        ver.startsWith("5.15") -> "android13"
        else -> "android12"
    }
    return "$android-$ver"
}

/** SUSFS 运行模式：未检测 / 内置 / LKM */
fun detectSusfsMode(): String {
    val out = runCatching {
        Shell.cmd("cat /proc/susfs_version 2>/dev/null || true").exec().out.joinToString(" ").trim()
    }.getOrNull().orEmpty()
    if (out.isEmpty()) return "未检测"
    return if (out.contains("builtin", true) || out.contains("kernel", true)) "内置" else "LKM"
}

/** 读取内核扩展值（旧树的 build_stamp 扩展）；新基座没有该扩展，返回 null */
suspend fun getExtensionValue(extension: String): Long? = null

/** 设备名：优先用 Build.MODEL（KSU 风格显示营销名需映射表，这里先直出） */
fun resolveDeviceName(context: android.content.Context): String = android.os.Build.MODEL.orEmpty()
