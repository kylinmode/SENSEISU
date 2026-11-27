package com.senseisu.ssu.ui.sensei

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.senseisu.ssu.ui.util.execKsudOutputRaw
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SUSFS_CONF = "/data/adb/ssu/susfs.conf"

/** 内核里真正启用的能力（来自 ksud susfs features） */
private fun readFeatures(): Set<String> {
    val out = execKsudOutputRaw("${ksudPath()} susfs features")
    return out.lines().map { it.trim() }.filter { it.startsWith("CONFIG_KSU_SUSFS") }.toSet()
}

private fun ksudPath(): String = com.senseisu.ssu.ui.util.ksudPathForDiagnostics()

private fun readConfText(): String =
    runCatching { ShellUtils.fastCmd("cat $SUSFS_CONF 2>/dev/null") }.getOrDefault("")

private fun getKey(text: String, key: String): String =
    text.lines().firstOrNull { it.startsWith("$key=") }?.substringAfter("=")?.trim().orEmpty()

/** 按界面状态整份写入（不依赖读文件，避免读失败导致清空） */
private fun writeModel(
    paths: List<String>,
    unameRelease: String,
    unameVersion: String,
    cmdline: String,
    avc: Boolean,
    log: Boolean,
    hideMnts: Boolean,
): Boolean {
    val body = buildString {
        appendLine("# SenseiSU SUSFS 配置（管理器编辑，ksud susfs apply 生效）")
        appendLine("paths=" + paths.joinToString("|"))
        appendLine("uname_release=" + unameRelease)
        appendLine("uname_version=" + unameVersion)
        appendLine("cmdline=" + cmdline)
        appendLine("avc_log_spoof=" + avc)
        appendLine("log_enabled=" + log)
        appendLine("hide_sus_mnts=" + hideMnts)
    }
    val b64 = android.util.Base64.encodeToString(body.toByteArray(), android.util.Base64.NO_WRAP)
    return runCatching {
        ShellUtils.fastCmd("mkdir -p /data/adb/ssu && echo $b64 | base64 -d > $SUSFS_CONF && chmod 600 $SUSFS_CONF")
        true
    }.getOrDefault(false)
}

private fun log(msg: String) {
    val b64 = android.util.Base64.encodeToString(
        ("[${System.currentTimeMillis()}] $msg\n").toByteArray(), android.util.Base64.NO_WRAP
    )
    runCatching {
        ShellUtils.fastCmd("mkdir -p /data/adb/ssu && echo $b64 | base64 -d >> /data/adb/ssu/susfs.log")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SusfsPanelScreen(navigator: DestinationsNavigator) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var version by remember { mutableStateOf("") }
    var variant by remember { mutableStateOf("") }
    var features by remember { mutableStateOf<Set<String>>(emptySet()) }
    var paths by remember { mutableStateOf<List<String>>(emptyList()) }
    var unameRelease by remember { mutableStateOf("") }
    var unameVersion by remember { mutableStateOf("") }
    var cmdline by remember { mutableStateOf("") }
    var avcSpoof by remember { mutableStateOf(false) }
    var logEnabled by remember { mutableStateOf(false) }
    var hideMnts by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confText by remember { mutableStateOf("") }
    var confExists by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            val data = withContext(Dispatchers.IO) {
                val text = readConfText()
                withContext(Dispatchers.Main) {
                    confText = text
                    confExists = text.isNotBlank()
                }
                listOf(
                    execKsudOutputRaw("${ksudPath()} susfs version"),
                    execKsudOutputRaw("${ksudPath()} susfs variant"),
                    execKsudOutputRaw("${ksudPath()} susfs features"),
                    getKey(text, "paths"),
                    getKey(text, "uname_release"),
                    getKey(text, "uname_version"),
                    getKey(text, "cmdline"),
                    getKey(text, "avc_log_spoof"),
                    getKey(text, "log_enabled"),
                    getKey(text, "hide_sus_mnts"),
                )
            }
            version = data[0].trim()
            variant = data[1].trim()
            features = data[2].lines().map { it.trim() }.filter { it.startsWith("CONFIG_KSU_SUSFS") }.toSet()
            val fileHasPaths = data[3].isNotBlank()
            if (fileHasPaths || paths.isEmpty()) {
                paths = data[3].split("|").filter { it.isNotBlank() }
            }
            unameRelease = data[4].ifBlank { unameRelease }
            unameVersion = data[5].ifBlank { unameVersion }
            cmdline = data[6]
            if (fileHasPaths) {
                avcSpoof = data[7] == "true"
                logEnabled = data[8] == "true"
                hideMnts = data[9] == "true"
            }
        }
    }

    fun exec(label: String, cmd: String, optimistic: (() -> Unit)? = null) {
        busy = true
        optimistic?.invoke()
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                val r = execKsudOutputRaw("${ksudPath()} $cmd")
                log("$label → $cmd\n$r")
                r
            }
            busy = false
            reload()
            val msg = out.ifBlank { if (label.isNotEmpty()) "$label 完成" else "完成" }
            snackbar.showSnackbar(if (msg.length > 180) msg.take(180) + "…" else msg)
        }
    }

    val hasAvcFeature = features.contains("CONFIG_KSU_SUSFS_AVC_LOG_SPOOFING")

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it, shape = RoundedCornerShape(14.dp)) } },
        topBar = {
            TopAppBar(
                title = { Text("SUSFS 面板", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { reload() }) { Text("刷新") }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("运行状态", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "版本 ${version.ifBlank { "—" }}　变体 ${variant.ifBlank { "—" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "内核能力：${features.size} 项",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 本地配置
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("本地配置", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        if (confExists) SUSFS_CONF else "未找到 $SUSFS_CONF",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (confExists) {
                        Text(
                            confText.take(600),
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                busy = true
                                scope.launch {
                                    val r = withContext(Dispatchers.IO) {
                                        ShellUtils.fastCmd(
                                            "mkdir -p /data/adb/ssu && " +
                                                "if [ -f /sdcard/senseisu/ssu3/susfs.conf ]; then " +
                                                "cp /sdcard/senseisu/ssu3/susfs.conf $SUSFS_CONF && echo imported; " +
                                                "else echo missing; fi"
                                        )
                                    }
                                    busy = false
                                    reload()
                                    snackbar.showSnackbar(if (r.contains("imported")) "已导入本地配置" else "没找到 /sdcard/senseisu/ssu3/susfs.conf")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("从 /sdcard 导入") }

                        OutlinedButton(
                            onClick = {
                                busy = true
                                scope.launch {
                                    val out = withContext(Dispatchers.IO) {
                                        execKsudOutputRaw("${ksudPath()} susfs apply")
                                    }
                                    busy = false
                                    reload()
                                    snackbar.showSnackbar(out.ifBlank { "已按本地配置下发" })
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("按本地配置下发") }
                    }
                }
            }

            // 隐藏路径
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("隐藏路径（sus_path）", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        IconButton(onClick = { showAdd = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "添加")
                        }
                    }
                    if (paths.isEmpty()) {
                        Text("还没有隐藏路径", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        paths.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(p, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    paths = paths - p
                                    writeModel(paths, unameRelease, unameVersion, cmdline, avcSpoof, logEnabled, hideMnts)
                                    exec("已从配置移除 $p", "susfs apply")
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Text(
                        "路径要真实存在；添加立即生效，移除需重启。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // uname
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("内核版本伪装（uname）", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "当前：${unameRelease.ifBlank { "未设置" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    var editing by remember { mutableStateOf(false) }
                    if (editing) {
                        OutlinedTextField(
                            value = unameRelease,
                            onValueChange = { unameRelease = it },
                            label = { Text("release（≤64 字符）") },
                            singleLine = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                editing = false
                                unameVersion = unameVersion.ifBlank { "#1 SMP PREEMPT" }
                                writeModel(paths, unameRelease, unameVersion, cmdline, avcSpoof, logEnabled, hideMnts)
                                exec("uname 已更新", "susfs apply")
                            }, shape = RoundedCornerShape(12.dp)) { Text("保存并应用") }
                            OutlinedButton(onClick = { editing = false }, shape = RoundedCornerShape(12.dp)) { Text("取消") }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { editing = true }, shape = RoundedCornerShape(12.dp)) { Text("编辑") }
                            OutlinedButton(onClick = {
                                val real = runCatching { ShellUtils.fastCmd("uname -r") }.getOrDefault("").trim()
                                unameRelease = real.ifBlank { "6.6.89-android15" }
                                unameVersion = "#1 SMP PREEMPT"
                                writeModel(paths, unameRelease, unameVersion, cmdline, avcSpoof, logEnabled, hideMnts)
                                exec("uname 已更新", "susfs apply")
                            }, shape = RoundedCornerShape(12.dp)) { Text("用当前版本号") }
                        }
                    }
                }
            }

            // 开关
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("开关", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

                    ToggleRow(
                        label = "AVC 日志伪装",
                        desc = if (hasAvcFeature) null else "当前内核未启用该功能",
                        checked = avcSpoof && hasAvcFeature,
                        enabled = hasAvcFeature && !busy,
                    ) { on ->
                        avcSpoof = on
                        writeModel(paths, unameRelease, unameVersion, cmdline, avcSpoof, logEnabled, hideMnts)
                        exec("已保存（内核不支持该项）", "susfs apply")
                    }

                    ToggleRow(
                        label = "SUSFS 内核日志",
                        desc = null,
                        checked = logEnabled,
                        enabled = features.contains("CONFIG_KSU_SUSFS_ENABLE_LOG") && !busy,
                    ) { on ->
                        logEnabled = on
                        writeModel(paths, unameRelease, unameVersion, cmdline, avcSpoof, logEnabled, hideMnts)
                        exec("内核日志已${if (on) "开启" else "关闭"}", "susfs apply")
                    }

                    ToggleRow(
                        label = "对非 root 进程隐藏挂载",
                        desc = null,
                        checked = hideMnts,
                        enabled = features.contains("CONFIG_KSU_SUSFS_SUS_MOUNT") && !busy,
                    ) { on ->
                        hideMnts = on
                        writeModel(paths, unameRelease, unameVersion, cmdline, avcSpoof, logEnabled, hideMnts)
                        exec("挂载隐藏已${if (on) "开启" else "关闭"}", "susfs apply")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    busy = true
                    scope.launch {
                        val out = withContext(Dispatchers.IO) {
                            val r = execKsudOutputRaw("${ksudPath()} susfs apply")
                            log("apply\n$r")
                            r
                        }
                        busy = false
                        snackbar.showSnackbar(out.ifBlank { "已重新下发配置" })
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "执行中…" else "重新应用全部配置") }

            OutlinedButton(
                onClick = {
                    busy = true
                    scope.launch {
                        val path = withContext(Dispatchers.IO) {
                            val k = ksudPath()
                            val cmds = listOf(
                                "id", "uname -r",
                                "$k susfs version", "$k susfs variant", "$k susfs features",
                                "$k susfs config", "cat $SUSFS_CONF",
                                "cat /data/adb/ssu/susfs.log 2>/dev/null | tail -30",
                                "lsmod | grep -i susfs", "ls -l /data/adb/modules/ | head -20",
                            )
                            val body = buildString {
                                appendLine("SenseiSU SUSFS 诊断  ${java.util.Date()}")
                                cmds.forEach { c ->
                                    appendLine("\n$ $c")
                                    appendLine(execKsudOutputRaw(c))
                                }
                            }
                            val b64 = android.util.Base64.encodeToString(body.toByteArray(), android.util.Base64.NO_WRAP)
                            val out = "/storage/emulated/0/Download/ssu-susfs-diag.txt"
                            ShellUtils.fastCmd("echo $b64 | base64 -d > $out && chmod 644 $out")
                            out
                        }
                        busy = false
                        snackbar.showSnackbar("诊断已保存：$path")
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出诊断文件") }

            Text(
                "配置文件：$SUSFS_CONF\n开机时由 ksud 自动下发。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showAdd) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加隐藏路径") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    placeholder = { Text("/data/adb/modules") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = input.trim()
                    if (p.startsWith("/")) {
                        showAdd = false
                        paths = (paths + p).distinct()
                        writeModel(paths, unameRelease, unameVersion, cmdline, avcSpoof, logEnabled, hideMnts)
                        exec("已隐藏 $p", "susfs apply")
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    desc: String?,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp)
            desc?.let {
                Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
