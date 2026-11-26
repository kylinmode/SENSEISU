package com.senseisu.ssu.ui.sensei

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senseisu.ssu.ui.util.getRootShell

/** 本机刷入页：选镜像 → su 执行 assets/ssu-flash-local.sh 写当前分区 */
@Composable
fun SenseiLocalFlashContent() {
    val context = LocalContext.current
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var imgName by remember { mutableStateOf<String?>(null) }

    fun flash(uri: Uri) {
        busy = true
        log = "开始本机刷入…"
        Thread {
            val lines = mutableListOf<String>()
            runCatching {
                val img = java.io.File(context.cacheDir, "ssu-local.img")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    img.outputStream().use { input.copyTo(it) }
                } ?: error("读取镜像失败")

                val script = java.io.File(context.cacheDir, "ssu-flash-local.sh")
                context.assets.open("ssu-flash-local.sh").use { input ->
                    script.outputStream().use { input.copyTo(it) }
                }

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

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            imgName = uri.lastPathSegment ?: "已选择镜像"
            flash(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("本机刷入", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B2B44))
                Text(
                    "选一个已修补好的 boot / init_boot 镜像，直接用 root 写入当前设备分区。\n" +
                        "前提：设备已有 root（本 App 驱动或其他管理器均可）。",
                    fontSize = 13.sp,
                    color = Color(0xFF5B6B80),
                )
                imgName?.let {
                    Text("已选：$it", fontSize = 12.sp, color = Color(0xFF2F6FD0))
                }
                Button(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (busy) "刷入中…" else "选择镜像并刷入", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (log.isNotBlank()) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("日志", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B2B44))
                    Spacer(Modifier.height(6.dp))
                    Text(log, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF33414F))
                }
            }
        }

        Text(
            "提示：刷错分区会导致无法开机，刷之前请备份原厂镜像。",
            fontSize = 12.sp,
            color = Color(0xFF8A97A8),
        )
    }
}
