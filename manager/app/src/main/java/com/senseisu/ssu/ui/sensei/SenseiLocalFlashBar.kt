package com.senseisu.ssu.ui.sensei

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senseisu.ssu.ui.util.getRootShell

/**
 * 本机刷入：选镜像 → 用 su 调 assets/ssu-flash-local.sh 写当前设备分区。
 * 前提：设备已有 root（本 App 的驱动，或其他管理器）。
 */
@Composable
fun SenseiLocalFlashBar() {
    val context = LocalContext.current
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    fun flash(uri: android.net.Uri) {
        busy = true
        log = "开始本机刷入…"
        showLog = true
        Thread {
            val lines = mutableListOf<String>()
            runCatching {
                // 1. 拷出镜像到 cache
                val img = java.io.File(context.cacheDir, "ssu-local.img")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    img.outputStream().use { input.copyTo(it) }
                } ?: error("读取镜像失败")

                // 2. 释放脚本
                val script = java.io.File(context.cacheDir, "ssu-flash-local.sh")
                context.assets.open("ssu-flash-local.sh").use { input ->
                    script.outputStream().use { input.copyTo(it) }
                }

                // 3. 用 root 执行
                val shell = getRootShell(true)
                shell.newJob()
                    .add("sh ${script.absolutePath} '${img.absolutePath}' init_boot 2>&1")
                    .to(lines, null)
                    .exec()
            }.onFailure { lines += "失败：${it.message}" }
            log = lines.joinToString("\n").ifBlank { "没有输出" }
            busy = false
        }.start()
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { flash(it) } }

    if (showLog) {
        AlertDialog(
            onDismissRequest = { showLog = false },
            confirmButton = {
                TextButton(onClick = { showLog = false }) { Text("关闭") }
            },
            title = { Text(if (busy) "刷入中…" else "刷入结果") },
            text = {
                Column(Modifier.heightIn(max = 360.dp)) {
                    Text(log, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) },
            enabled = !busy,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(if (busy) "刷入中…" else "本机刷入", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = { showLog = true },
            shape = RoundedCornerShape(14.dp),
        ) { Text("日志") }
    }
}
