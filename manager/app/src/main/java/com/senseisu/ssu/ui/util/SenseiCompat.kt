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

/**
 * SUSFS 状态检测（三种形态）
 *
 * 1. LKM 模块：本基座由 ksud 加载 susfs_guard_lkm，特征为 /sys/module/susfs_guard_lkm
 * 2. 内核内置：/proc/susfs_version 存在且不含模块标记
 * 3. 未检测
 */
suspend fun detectSusfsMode(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    // 1) 先看 LKM 模块
    val lkm = runCatching {
        com.topjohnwu.superuser.Shell.cmd("test -d /sys/module/susfs_guard_lkm && echo yes").exec()
            .out.any { it.trim() == "yes" }
    }.getOrDefault(false)
    if (lkm) return@withContext "lkm"

    // 2) 再看内核内置
    val ver = runCatching {
        com.topjohnwu.superuser.Shell.cmd("cat /proc/susfs_version 2>/dev/null").exec()
            .out.joinToString(" ").trim()
    }.getOrDefault("")
    if (ver.isNotEmpty()) return@withContext "gki"

    ""
}

/** SUSFS 版本行，供 UI 直接显示 */
suspend fun susfsStatusText(): String {
    return when (detectSusfsMode()) {
        "lkm" -> "LKM 模块"
        "gki" -> {
            val v = runCatching { getSuSFSVersion() }.getOrDefault("")
            val line = v.lineSequence().map { it.trim() }
                .firstOrNull { it.isNotEmpty() && !it.contains("not found", true) }
            line?.let { "内置（$it）" } ?: "内置"
        }
        else -> "未检测"
    }
}

/** 读取内核扩展值（旧树的 build_stamp 扩展）；新基座没有该扩展，返回 null */
suspend fun getExtensionValue(extension: String): Long? = null

/** 设备名：优先用 Build.MODEL（KSU 风格显示营销名需映射表，这里先直出） */
fun resolveDeviceName(context: android.content.Context): String = android.os.Build.MODEL.orEmpty()
