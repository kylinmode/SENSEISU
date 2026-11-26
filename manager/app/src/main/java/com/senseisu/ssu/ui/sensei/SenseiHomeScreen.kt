package com.senseisu.ssu.ui.sensei

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.senseisu.ssu.BuildConfig
import com.senseisu.ssu.ui.util.getModuleCount
import com.senseisu.ssu.ui.util.getSuSFSVersion
import com.senseisu.ssu.ui.util.getSuperuserCount
import com.senseisu.ssu.ui.util.rootAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 一键把首页换成 SSU 版式。想回退改成 false 即可。 */
const val SSU_SENSEI_HOME: Boolean = true

private data class Student(val key: String, val cn: String, val en: String)

private val roster = listOf(
    Student("shiroko", "白子", "SHIROKO"),
    Student("hoshino", "星野", "HOSHINO"),
    Student("yuuka", "优香", "YUUKA"),
    Student("aru", "阿露", "ARU"),
    Student("shun", "瞬", "SHUN"),
    Student("asuna", "亚丝娜", "ASUNA"),
    Student("hina", "日奈", "HINA"),
)

private fun readSeccomp(): String = runCatching {
    java.io.File("/proc/self/status").useLines { lines ->
        val line = lines.firstOrNull { it.startsWith("Seccomp:") } ?: return@useLines "未知"
        when (line.substringAfter(":").trim()) {
            "0" -> "未启用"
            "1" -> "过滤模式"
            "2" -> "严格模式"
            else -> "未知"
        }
    }
}.getOrDefault("未知")


/** 读 AK3 刷入时写进内核 cmdline 的构建标记（ssu_build=2026.11.25-android15-6.6） */
private fun readKernelBuildStamp(): String {
    val cmdline = runCatching { java.io.File("/proc/cmdline").readText() }.getOrDefault("")
    return Regex("(?:^|\\s)ssu_build=(\\S+)").find(cmdline)?.groupValues?.get(1)
        ?.replace("%20", " ")
        .orEmpty()
}


/** 内核内置构建信息：高 32 位是日期 YYYYMMDD，低 8 位是构建人编号 */
private fun formatKernelBuild(raw: Long): String {
    val date = (raw ushr 32).toInt()
    val builder = (raw and 0xFF).toInt()
    if (date <= 0) return ""
    val y = date / 10000
    val m = (date / 100) % 100
    val day = date % 100
    val name = when (builder) {
        1 -> "爱意见汐"
        else -> ""
    }
    return "%04d.%02d.%02d %s".format(y, m, day, name).trim()
}

private data class KernelState(
    val root: Boolean? = null,
    val superusers: Int = 0,
    val modules: Int = 0,
    val susfs: String = "",
    val loading: Boolean = true,
    /** "LKM" 或 "GKI"，用于状态块上的模式徽标 */
    val mode: String = "LKM",
    /** AK3 刷入时写进 cmdline 的构建标记 */
    val buildStamp: String = "",
)

/**
 * SSU 首页：版式参考主流 root 管理器（大标题 + 工作状态卡 + 版本信息 + 状态信息），
 * 视觉沿用蔚蓝档案的浅蓝描图纸与学生立绘。
 */
@Composable
fun SenseiHomeScreen(navigator: DestinationsNavigator, bottomInnerPadding: Dp = 0.dp) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(KernelState()) }
    var seccomp by remember { mutableStateOf("—") }
    var muse by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        seccomp = withContext(Dispatchers.IO) { readSeccomp() }
        state = withContext(Dispatchers.IO) {
            val root = runCatching { rootAvailable() }.getOrDefault(false)
            val su = runCatching { getSuperuserCount() }.getOrDefault(0)
            val mods = runCatching { getModuleCount() }.getOrDefault(0)
            // SUSFS 状态：区分「LKM 模块」与「内核内置」
            val susfs = runCatching { com.senseisu.ssu.ui.util.susfsStatusText() }
                .getOrDefault("未检测")
                .takeIf { it != "未检测" } ?: ""
            // 内核内置构建信息（改刷入脚本伪造不了），取不到再退回 cmdline
            val kernelBuild = runCatching {
                com.senseisu.ssu.ui.util.getExtensionValue("build")
                    ?.let { formatKernelBuild(it) }
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull() ?: readKernelBuildStamp()
            val driveMode = runCatching {
                val v = com.senseisu.ssu.Natives.version
                when {
                    // 读不到 SSU 驱动版本，说明当前内核不是我们编的（例如还在用别的 root 方案）
                    v <= 0 -> "OTHER"
                    com.senseisu.ssu.Natives.isLkmMode -> "LKM"
                    else -> "GKI"
                }
            }.getOrDefault("OTHER")
            KernelState(root, su, mods, susfs, loading = false, mode = driveMode, buildStamp = kernelBuild)
        }
    }

    val heroBitmap = remember {
        runCatching {
            context.assets.open("sensei/hero.png").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    val studentBitmaps = remember {
        List(roster.size) { index ->
            runCatching {
                context.assets.open("sensei/students/%02d.png".format(index))
                    .use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }

    SenseiBackground {
        if (heroBitmap != null) {
            Image(
                bitmap = heroBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.28f),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xCCF2FAFF),
                            0.45f to Color(0xD9EAF5FF),
                            1f to Color(0xF2E2F1FE),
                        )
                    )
            )
        }

        // 顶部要让开系统状态栏，否则「Sensei SU」标题会和状态栏图标重叠
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding)
                .padding(top = statusBarTop + 14.dp, bottom = bottomInnerPadding + 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TitleBar(
                onLock = { runCatching { navigator.navigate(InstallScreenDestination) } },
                susfsSupported = state.susfs.isNotBlank(),
            )
            HeroArt(heroBitmap)
            WorkingStatusBar(state, onClick = { runCatching { navigator.navigate(InstallScreenDestination) } })
            UpdateBanner(state)
            SectionTitle("版本信息 / VERSION")
            InfoCard(
                listOf(
                    "设备" to com.senseisu.ssu.ui.util.resolveDeviceName(context),
                    "内核版本" to (System.getProperty("os.version") ?: "—"),
                    "Android 版本" to "${Build.VERSION.RELEASE} · SDK ${Build.VERSION.SDK_INT}",
                    "内核驱动版本" to when {
                        state.root != true -> "未检测到"
                        state.mode == "OTHER" -> "未检测到 SSU 驱动（当前内核可能来自其它 root 方案）"
                        state.mode == "GKI" -> {
                            // 内置模式：AK3 是编译进内核的，显示构建日期 + 构建人
                            val build = state.buildStamp.takeIf { it.isNotBlank() }
                            val suffix = build?.let { " $it" }.orEmpty()
                            "SSU$suffix · Built-in（${com.senseisu.ssu.Natives.version}）"
                        }
                        // LKM 模式：没有 AK3 构建信息，只显示工作模式
                        else -> "LKM（${com.senseisu.ssu.Natives.version}）"
                    },
                    "管理器版本" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    "SuSFS 版本" to state.susfs.ifBlank {
                        if (state.mode == "GKI") "未启用（需内核开启 CONFIG_KSU_SUSFS）"
                        else "未安装（LKM 模式需装 SUSFS 模块）"
                    },
                ),
            )
            SectionTitle("状态信息 / STATUS")
            InfoCard(
                listOf(
                    "SELinux 状态" to if (state.root == true) "已隐藏相关规则" else "—",
                    "Seccomp" to seccomp,
                    "模块挂载" to if (state.root == true) "对普通应用已卸载" else "—",
                ),
            )
            RosterCard(studentBitmaps) { muse = it }
            Footer()
        }
        SenseiMuseFullscreen(name = muse) { muse = null }
    }
}

@Composable
private fun TitleBar(onLock: () -> Unit, susfsSupported: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Sensei SU",
            color = Sensei.Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.width(10.dp))
        SenseiOnlyBadge()
        Spacer(Modifier.weight(1f))
        if (susfsSupported) {
            IconAction(Icons.Filled.Lock, onLock)
            Spacer(Modifier.width(6.dp))
        }
        PowerMenu()
    }
}


/** 右上角电源菜单：重启 / 软重启 / Recovery / Bootloader / Download / EDL */
@Composable
private fun PowerMenu() {
    var expanded by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun run(cmd: String) {
        runCatching { com.topjohnwu.superuser.Shell.cmd(cmd).exec() }
            .onFailure { android.widget.Toast.makeText(ctx, "执行失败", android.widget.Toast.LENGTH_SHORT).show() }
    }
    Box {
        IconAction(Icons.Filled.PowerSettingsNew) { expanded = true }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White),
        ) {
            listOf(
                "重启系统" to "svc power reboot || reboot",
                "重启到 Recovery" to "reboot recovery",
                "重启到 Bootloader" to "reboot bootloader",
            ).forEach { (label, cmd) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(label, color = Sensei.Ink, fontSize = 14.sp) },
                    onClick = { expanded = false; run(cmd) },
                )
            }
        }
    }
}

@Composable
private fun IconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Sensei.InkSoft, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun HeroArt(hero: android.graphics.Bitmap?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Sensei.PaperTop),
    ) {
        if (hero != null) {
            Image(
                bitmap = hero.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Sensei.PaperTop, Sensei.PaperBottom))))
        }
        // 底部渐白，和下一张卡片自然衔接
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.62f to Color.Transparent,
                        0.88f to Color(0x66FFFFFF),
                        1f to Color.White.copy(alpha = 0.92f),
                    )
                )
        )
    }
}

@Composable
private fun WorkingStatusBar(state: KernelState, onClick: () -> Unit = {}) {
    SenseiCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when (state.root) {
                            true -> Sensei.Mint.copy(alpha = 0.18f)
                            false -> Sensei.Amber.copy(alpha = 0.18f)
                            null -> Sensei.InkFaint.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (state.root == true) Icons.Filled.Check else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = when (state.root) {
                        true -> Sensei.Mint
                        false -> Sensei.Amber
                        null -> Sensei.InkFaint
                    },
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (state.root) {
                            true -> "工作中"
                            false -> "未就绪"
                            null -> "检测中"
                        },
                        color = Sensei.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Sensei.SkyDeep)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            if (state.mode == "GKI") "Built-in" else "LKM",
                            color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    when (state.root) {
                        true -> "${state.superusers} 个超级用户，${state.modules} 个模块"
                        false -> "内核驱动未加载，先修补 boot"
                        null -> "正在读取内核驱动状态…"
                    },
                    color = Sensei.InkSoft, fontSize = 11.sp,
                )
            }
        }
    }
}





@Composable
private fun UpdateBanner(state: KernelState) {
    val text = if (state.loading) "正在检查更新…" else "SSU ${BuildConfig.VERSION_NAME} · 当前已是最新构建"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = Sensei.InkSoft, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Sensei.InkSoft, fontSize = 12.sp)
    }
}

@Composable
private fun SectionTitle(text: String) {
    SenseiSectionLabel(text, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    SenseiCard(padding = 4.dp) {
        rows.forEachIndexed { index, pair ->
            val label = pair.first
            val value = pair.second
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(Sensei.Stroke.copy(alpha = 0.45f))
                )
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                Text(label, color = Sensei.InkFaint, fontSize = 11.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    value,
                    color = Sensei.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}

@Composable
private fun RosterCard(avatars: List<android.graphics.Bitmap?>, onPick: (String) -> Unit) {
    SenseiCard {
        SenseiHeader("SCHALE ROSTER / 学生名册", trailing = "07 MEMBERS")
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            roster.forEachIndexed { index, student ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPick(student.key) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                ) {
                    StudentAvatar(
                        index = index,
                        label = student.cn.take(1),
                        bitmap = avatars.getOrNull(index),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(student.cn, color = Sensei.Ink, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Text(student.en, color = Sensei.InkFaint, fontSize = 7.sp, letterSpacing = 0.4.sp)
                }
            }
        }
    }
}

@Composable
private fun Footer() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(Sensei.Pink, size = 5.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            "SSU · Sensei SU  内核级 root 管理器",
            color = Sensei.InkFaint,
            fontSize = 9.sp,
            letterSpacing = 0.4.sp,
        )
    }
}
