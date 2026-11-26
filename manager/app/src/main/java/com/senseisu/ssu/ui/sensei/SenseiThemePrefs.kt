package com.senseisu.ssu.ui.sensei

import android.content.Context

/** Sensei 主题偏好（第 1 轮：模式 / 动态色 / 背景 / 模糊） */
const val THEME_PREFS = "sensei_theme"
const val KEY_MODE = "theme_mode"        // 0 跟随系统 1 浅色 2 深色
const val KEY_DYNAMIC = "theme_dynamic"  // 动态取色
const val KEY_BG = "theme_bg_uri"        // 背景图 uri
const val KEY_BLUR = "theme_bg_blur"     // 背景模糊

fun themePrefs(context: Context) = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)

data class SenseiThemeState(
    val mode: Int = 0,
    val dynamic: Boolean = true,
    val bgUri: String? = null,
    val blur: Boolean = false,
)

fun loadSenseiTheme(context: Context): SenseiThemeState {
    val p = themePrefs(context)
    return SenseiThemeState(
        mode = p.getInt(KEY_MODE, 0),
        dynamic = p.getBoolean(KEY_DYNAMIC, true),
        bgUri = p.getString(KEY_BG, null),
        blur = p.getBoolean(KEY_BLUR, false),
    )
}
