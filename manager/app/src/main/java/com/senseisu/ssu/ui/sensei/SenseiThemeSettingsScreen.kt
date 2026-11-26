package com.senseisu.ssu.ui.sensei

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 第 1 轮：外观底盘（主题模式 / 动态颜色 / 自定义背景 / 模糊） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenseiThemeSettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { themePrefs(context) }
    var state by remember { mutableStateOf(loadSenseiTheme(context)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            prefs.edit().putString(KEY_BG, uri.toString()).apply()
            state = state.copy(bgUri = uri.toString())
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("返回") }
                Spacer(Modifier.width(8.dp))
                Text("主题设置", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }

            // 主题模式
            SettingsCard("主题模式") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("跟随系统", "浅色", "深色").forEachIndexed { i, label ->
                        FilterChip(
                            selected = state.mode == i,
                            onClick = {
                                prefs.edit().putInt(KEY_MODE, i).apply()
                                state = state.copy(mode = i)
                            },
                            label = { Text(label, fontSize = 13.sp) },
                        )
                    }
                }
            }

            // 动态颜色
            SettingsCard("动态颜色") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("跟随系统壁纸取色", fontSize = 13.sp)
                    Switch(
                        checked = state.dynamic,
                        onCheckedChange = {
                            prefs.edit().putBoolean(KEY_DYNAMIC, it).apply()
                            state = state.copy(dynamic = it)
                        },
                    )
                }
            }

            // 自定义背景
            SettingsCard("自定义背景") {
                Text(if (state.bgUri.isNullOrBlank()) "未设置" else "已设置", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { picker.launch(arrayOf("image/*")) }, shape = RoundedCornerShape(12.dp)) {
                        Text("选择图片")
                    }
                    OutlinedButton(
                        onClick = {
                            prefs.edit().remove(KEY_BG).apply()
                            state = state.copy(bgUri = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("清除") }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("背景模糊", fontSize = 13.sp)
                    Switch(
                        checked = state.blur,
                        onCheckedChange = {
                            prefs.edit().putBoolean(KEY_BLUR, it).apply()
                            state = state.copy(blur = it)
                        },
                    )
                }
            }

            Text(
                "改完返回首页即生效",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            content()
        }
    }
}
