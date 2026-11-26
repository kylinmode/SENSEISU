package com.senseisu.ssu.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_DARK,
    tertiary = SECONDARY_DARK
)

private val LightColorScheme = lightColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_LIGHT,
    tertiary = SECONDARY_LIGHT
)

fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
}

@Composable
fun KernelSUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        amoledMode && darkTheme && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = dynamicDarkColorScheme(context)
            dynamicScheme.copy(
                background = AMOLED_BLACK,
                surface = AMOLED_BLACK,
                surfaceVariant = dynamicScheme.surfaceVariant.blend(AMOLED_BLACK, 0.6f),
                surfaceContainer = dynamicScheme.surfaceContainer.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerLow = dynamicScheme.surfaceContainerLow.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerLowest = dynamicScheme.surfaceContainerLowest.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerHigh = dynamicScheme.surfaceContainerHigh.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerHighest = dynamicScheme.surfaceContainerHighest.blend(AMOLED_BLACK, 0.6f)
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        amoledMode && darkTheme -> {
            DarkColorScheme.copy(
                background = AMOLED_BLACK,
                surface = AMOLED_BLACK,
                surfaceVariant = DARK_GREY.blend(AMOLED_BLACK, 0.8f),
                surfaceContainer = DARK_GREY.blend(AMOLED_BLACK, 0.8f),
                surfaceContainerLow = DARK_GREY.blend(AMOLED_BLACK, 0.8f),
                surfaceContainerLowest = DARK_GREY.blend(AMOLED_BLACK, 0.8f),
                surfaceContainerHigh = DARK_GREY.blend(AMOLED_BLACK, 0.8f),
                surfaceContainerHighest = DARK_GREY.blend(AMOLED_BLACK, 0.8f),
            )
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    SystemBarStyle(
        darkMode = darkTheme
    )

    MaterialTheme(
        colorScheme = colorScheme.sensei(darkTheme),
        shapes = SenseiShapes,
        typography = Typography,
        content = content
    )
}

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode },
            navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}

/** 蔚蓝档案配色（全局覆盖，所有页面生效） */
private object SenseiScheme {
    val Sky = androidx.compose.ui.graphics.Color(0xFF3E9BE0)
    val SkyDeep = androidx.compose.ui.graphics.Color(0xFF1F6FB2)
    val SkySoft = androidx.compose.ui.graphics.Color(0xFFBFE2FB)
    val Pink = androidx.compose.ui.graphics.Color(0xFFF58AB8)
    val PinkWash = androidx.compose.ui.graphics.Color(0xFFFDE7F0)
    val Paper = androidx.compose.ui.graphics.Color(0xFFF2FAFF)
    val PaperDeep = androidx.compose.ui.graphics.Color(0xFFE4F1FE)
    val Ink = androidx.compose.ui.graphics.Color(0xFF1B2A38)
    val InkSoft = androidx.compose.ui.graphics.Color(0xFF6B8199)
    val NightBg = androidx.compose.ui.graphics.Color(0xFF101A24)
    val NightSurface = androidx.compose.ui.graphics.Color(0xFF17242F)
}

private val SenseiShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

private fun androidx.compose.material3.ColorScheme.sensei(dark: Boolean): androidx.compose.material3.ColorScheme =
    if (!dark) copy(
        primary = SenseiScheme.Sky,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = SenseiScheme.SkySoft,
        onPrimaryContainer = SenseiScheme.SkyDeep,
        secondary = SenseiScheme.Pink,
        secondaryContainer = SenseiScheme.PinkWash,
        onSecondaryContainer = SenseiScheme.Ink,
        background = SenseiScheme.Paper,
        onBackground = SenseiScheme.Ink,
        surface = androidx.compose.ui.graphics.Color.White,
        onSurface = SenseiScheme.Ink,
        surfaceVariant = SenseiScheme.PaperDeep,
        onSurfaceVariant = SenseiScheme.InkSoft,
        surfaceContainer = androidx.compose.ui.graphics.Color.White,
        surfaceContainerHigh = SenseiScheme.Paper,
        surfaceContainerLow = SenseiScheme.Paper,
        surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
        surfaceContainerHighest = SenseiScheme.PaperDeep,
        outline = SenseiScheme.SkySoft,
    ) else copy(
        primary = SenseiScheme.Sky,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = SenseiScheme.SkyDeep,
        onPrimaryContainer = SenseiScheme.SkySoft,
        secondary = SenseiScheme.Pink,
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3A2430),
        background = SenseiScheme.NightBg,
        onBackground = androidx.compose.ui.graphics.Color(0xFFE6F1F8),
        surface = SenseiScheme.NightSurface,
        onSurface = androidx.compose.ui.graphics.Color(0xFFE6F1F8),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1F2E3B),
        onSurfaceVariant = SenseiScheme.SkySoft,
        outline = androidx.compose.ui.graphics.Color(0xFF2C3E4E),
    )
