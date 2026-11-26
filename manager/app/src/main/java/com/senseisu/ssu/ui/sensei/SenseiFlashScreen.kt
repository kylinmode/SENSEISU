package com.senseisu.ssu.ui.sensei

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senseisu.ssu.ui.util.getRootShell
import com.senseisu.ssu.ui.util.rootAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本地刷入：手机内直接 dd 写 boot / init_boot，不需要 PC。
 *
 * 前提是设备已经有任意形式的 root（否则拿不到块设备写权限）。
 * 流程：选镜像 → 备份原分区 → 写入 → 读回校验 → 失败回滚 → 重启。
 */

/** 把 cache 里的镜像发布到公共 Download（/storage/emulated/0/Download），无需存储权限。 */
private fun publishToPublicDownload(context: Context, src: File, displayName: String): String? = runCatching {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_DOWNLOADS)
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching null
        resolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
        values.clear()
        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        "/storage/emulated/0/Download/$displayName"
    } else {
        @Suppress("DEPRECATION")
        val dir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val dst = File(dir, displayName)
        src.copyTo(dst, overwrite = true)
        dst.absolutePath
    }
}.getOrNull()

@Composable
fun SenseiFlashScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var imageFile by remember { mutableStateOf<File?>(null) }
    var log by remember { mutableStateOf(listOf<String>()) }
    var running by remember { mutableStateOf(false) }
    var rootOk by remember { mutableStateOf<Boolean?>(null) }
    var done by remember { mutableStateOf(false) }
    var patchedFile by remember { mutableStateOf<File?>(null) }
    var patching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        rootOk = withContext(Dispatchers.IO) { runCatching { rootAvailable() }.getOrDefault(false) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val f = withContextSyncCopy(context, uri)
        if (f != null) {
            imageFile = f
            log = log + "已选择镜像：${f.name}（${f.length() / 1024 / 1024} MB）"
        } else {
            log = log + "读取镜像失败"
        }
    }

    // 修补模式：不需要 root。直接以应用 uid 执行内置 ksud 的 boot-patch。
    fun runPatch() {
        val img = imageFile ?: return
        patching = true
        patchedFile = null
        log = log + "开始修补（应用内 ksud，无需 root）…"
        Thread {
            val ksud = File(context.applicationInfo.nativeLibraryDir, "libssud.so")
            // 先写应用私有 cache（无需任何权限），成功后再发布到公共 Download
            val outDir = context.cacheDir
            val lines = mutableListOf<String>()
            runCatching {
                val kmi = com.senseisu.ssu.ui.util.getCurrentKmiBlocking()
                val proc = ProcessBuilder(
                    ksud.absolutePath,
                    "boot-patch",
                    "-b", img.absolutePath,
                    "--kmi", kmi,
                    "-o", outDir.absolutePath,
                    "--out-name", "ssu-patched.img",
                ).redirectErrorStream(true).start()
                proc.inputStream.bufferedReader().forEachLine { lines += it }
                proc.waitFor()
            }.onFailure { lines += "执行失败: ${it.message}" }
            val out = File(outDir, "ssu-patched.img")
            if (out.isFile) {
                val published = publishToPublicDownload(context, out, "ssu-patched.img")
                log = log + lines + if (published != null) {
                    listOf("修补完成：$published", "把这个文件拷到电脑，fastboot flash 即可")
                } else {
                    listOf("修补完成（私有目录）：${out.absolutePath}", "发布到公共 Download 失败，可用文件管理器从应用私有目录导出")
                }
                patchedFile = out.takeIf { published != null } ?: out
            } else {
                log = log + lines + "没有产出 ssu-patched.img，检查上面的日志"
                patchedFile = null
            }
            patching = false
        }.start()
    }

    fun runFlash() {
        val img = imageFile ?: return
        running = true
        done = false
        log = log + "开始刷入…"
        Thread {
            val script = File(context.cacheDir, "ssu-flash-local.sh")
            runCatching {
                context.assets.open("ssu-flash-local.sh").use { input ->
                    script.outputStream().use { input.copyTo(it) }
                }
            }
            val shell = getRootShell(true)
            val lines = mutableListOf<String>()
            shell.newJob()
                .add("sh ${script.absolutePath} '${img.absolutePath}' init_boot 2>&1")
                .to(lines, null)
                .exec()
            log = log + lines
            running = false
            done = lines.any { it.contains("写入校验通过") || it.contains("完成") }
        }.start()
    }

    SenseiBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp,
                    bottom = 40.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(cardGap),
        ) {
            // 顶部栏
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Sensei.InkSoft, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("本地刷入", color = Sensei.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SenseiOnlyBadge()
            }

            // 前置条件
            SenseiCard {
                SenseiHeader("PRECHECK / 前置条件", trailing = if (rootOk == true) "READY" else "CHECK")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (rootOk == true) Icons.Filled.Check else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (rootOk == true) Sensei.Mint else Sensei.Amber,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (rootOk) {
                            true -> "检测到 root，可以本机写分区"
                            false -> "没有 root：仍可用上方「修补镜像」，再把产物拷到电脑 fastboot 刷入"
                            null -> "正在检测 root…"
                        },
                        color = Sensei.InkSoft,
                        fontSize = 12.sp,
                    )
                }
            }

            // 选镜像
            SenseiCard {
                SenseiHeader("IMAGE / 修补后的镜像")
                Spacer(Modifier.height(12.dp))
                Text(
                    imageFile?.let { "${it.name} · ${it.length() / 1024 / 1024} MB" } ?: "还没有选择镜像",
                    color = if (imageFile == null) Sensei.InkFaint else Sensei.Ink,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("选择镜像") { picker.launch(arrayOf("*/*")) }
                    PillButton(
                        text = if (patching) "修补中…" else "修补镜像",
                        enabled = imageFile != null && !patching,
                        primary = true,
                        onClick = { runPatch() },
                    )
                    PillButton(
                        text = if (running) "刷入中…" else "本地刷入",
                        enabled = imageFile != null && rootOk == true && !running,
                        onClick = { runFlash() },
                    )
                }
            }

            // 日志
            SenseiCard {
                SenseiHeader("LOG / 输出", trailing = if (done) "DONE" else "IDLE")
                Spacer(Modifier.height(10.dp))
                if (log.isEmpty()) {
                    Text("还没有操作记录", color = Sensei.InkFaint, fontSize = 11.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        log.takeLast(60).forEach { line ->
                            Text(
                                line,
                                color = if (line.contains("!!") || line.contains("失败")) Sensei.PinkDeep else Sensei.InkSoft,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Sensei.InkFaint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "刷入前会自动备份原分区到 /sdcard/Download，校验失败会回滚",
                    color = Sensei.InkFaint, fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    enabled: Boolean = true,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (primary) {
        Brush.horizontalGradient(listOf(Sensei.Sky, Sensei.SkyDeep))
    } else {
        Brush.horizontalGradient(listOf(Color.White, Color.White))
    }
    Box(
        Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (primary) Color.White else Sensei.Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun withContextSyncCopy(context: Context, uri: Uri): File? = runCatching {
    val name = "ssu-image-" + System.currentTimeMillis() + ".img"
    val dst = File(context.cacheDir, name)
    context.contentResolver.openInputStream(uri)?.use { input ->
        dst.outputStream().use { output -> input.copyTo(output) }
    } ?: return@runCatching null
    dst
}.getOrNull()
