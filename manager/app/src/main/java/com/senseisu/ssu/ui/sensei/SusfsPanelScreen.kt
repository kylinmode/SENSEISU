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
import com.senseisu.ssu.ui.util.execKsud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.superuser.ShellUtils

const val SUSFS_CONF = "/data/adb/ssu/susfs.conf"

data class SusfsConf(
    val paths: List<String> = emptyList(),
    val unameRelease: String = "",
    val unameVersion: String = "",
    val cmdline: String = "",
    val avcLogSpoof: Boolean = false,
    val logEnabled: Boolean = false,
    val hideSusMnts: Boolean = false,
)

private fun readConf(): SusfsConf {
    val text = runCatching { ShellUtils.fastCmd("cat $SUSFS_CONF 2>/dev/null") }.getOrDefault("")
    val map = text.lines()
        .mapNotNull { line ->
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#") || !t.contains("=")) null
            else t.substringBefore("=").trim() to t.substringAfter("=").trim()
        }
        .toMap()
    return SusfsConf(
        paths = map["paths"].orEmpty().split("|").filter { it.isNotBlank() },
        unameRelease = map["uname_release"].orEmpty(),
        unameVersion = map["uname_version"].orEmpty(),
        cmdline = map["cmdline"].orEmpty(),
        avcLogSpoof = map["avc_log_spoof"] == "true",
        logEnabled = map["log_enabled"] == "true",
        hideSusMnts = map["hide_sus_mnts"] == "true",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SusfsPanelScreen(navigator: DestinationsNavigator) {
    val scope = rememberCoroutineScope()
    var conf by remember { mutableStateOf(SusfsConf()) }
    var version by remember { mutableStateOf("") }
    var variant by remember { mutableStateOf("") }
    var addPath by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            val (c, v, va) = withContext(Dispatchers.IO) {
                Triple(
                    readConf(),
                    runCatching { ShellUtils.fastCmd("ksud susfs version") }.getOrDefault(""),
                    runCatching { ShellUtils.fastCmd("ksud susfs variant") }.getOrDefault(""),
                )
            }
            conf = c
            version = v.trim()
            variant = va.trim()
        }
    }

    fun run(label: String, args: String) {
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { execKsud(args, newShell = true) }.getOrDefault(false) }
            busy = false
            if (ok) reload()
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SUSFS 面板", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 状态
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("运行状态", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "版本 ${version.ifBlank { "—" }}　变体 ${variant.ifBlank { "—" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                        Text("隐藏路径", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        IconButton(onClick = { showAdd = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "添加")
                        }
                    }
                    if (conf.paths.isEmpty()) {
                        Text("还没有隐藏路径", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        conf.paths.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(p, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { run("del", "susfs remove-path '$p'") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Text(
                        "改动立即写入内核；删除需重启后彻底消失。",
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
                        "当前：${conf.unameRelease.ifBlank { "未设置" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            val r = conf.unameRelease.ifBlank { "6.6.89-android15-8-g0000000000-abogki000000000-4k" }
                            run("uname", "susfs set-uname --release '$r'")
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("伪装为常规版本") }
                }
            }

            // 开关
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("开关", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    ToggleRow("AVC 日志伪装", conf.avcLogSpoof) { on ->
                        run("avc", "susfs set-avc-log-spoof $on")
                    }
                    ToggleRow("SUSFS 内核日志", conf.logEnabled) { on ->
                        run("log", "susfs set-log $on")
                    }
                    ToggleRow("对非 root 进程隐藏挂载", conf.hideSusMnts) { on ->
                        run("mnt", "susfs set-hide-sus-mnts $on")
                    }
                }
            }

            // 应用
            Button(
                onClick = { run("apply", "susfs apply") },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "执行中…" else "重新应用全部配置")
            }

            Text(
                "配置文件：$SUSFS_CONF\n内容以 key=value 保存，重启后由 ksud 自动下发。",
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
                        run("add", "susfs add-path '$p'")
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
