package com.senseisu.ssu.ui.sensei

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Sensei 主题设置（仿 BakaSU 的个性化项） */
const val THEME_PREFS = "sensei_theme"
const val KEY_MODE = "theme_mode"       // 0 跟随系统 1 浅色 2 深色
const val KEY_SEED = "theme_seed"       // ARGB 或 -1

val THEME_PRESETS = listOf(
    "蔚蓝" to 0xFF4A6FA5,
    "薄荷" to 0xFF3FA97A,
    "樱花" to 0xFFD2477E,
    "暖橙" to 0xFFF3B45A,
    "紫罗兰" to 0xFF7A5AF8,
    "石墨" to 0xFF4A4A4A,
)

fun themePrefs(context: Context) = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenseiThemeSettingsContent() {
    val context = LocalContext.current
    val prefs = remember { themePrefs(context) }

    var mode by remember { mutableStateOf(prefs.getInt(KEY_MODE, 0)) }
    var seed by remember { mutableStateOf(prefs.getInt(KEY_SEED, -1)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 主题模式
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("主题模式", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("跟随系统", "浅色", "深色").forEachIndexed { index, label ->
                        FilterChip(
                            selected = mode == index,
                            onClick = {
                                mode = index
                                prefs.edit().putInt(KEY_MODE, index).apply()
                            },
                            label = { Text(label, fontSize = 13.sp) },
                        )
                    }
                }
                Text("切换后返回首页生效", fontSize = 12.sp, color = Color(0xFF8A97A8))
            }
        }

        // 主题色
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("主题色", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    THEME_PRESETS.forEach { (name, argb) ->
                        val c = Color(argb.toInt())
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (seed == argb.toInt()) 3.dp else 0.dp,
                                    color = Color(0xFF1B2B44),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    seed = argb.toInt()
                                    prefs.edit().putInt(KEY_SEED, argb.toInt()).apply()
                                },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        seed = -1
                        prefs.edit().putInt(KEY_SEED, -1).apply()
                    }) { Text("跟随系统取色") }
                }
            }
        }

    }
}
