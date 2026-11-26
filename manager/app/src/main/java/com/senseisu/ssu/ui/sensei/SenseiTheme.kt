package com.senseisu.ssu.ui.sensei

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SSU (Sensei SU) 视觉规范。
 *
 * 关键词：浅蓝描图纸底、白色半透明卡片、24dp 圆角、柔光阴影、
 * 樱花粉点缀、英文小标 + 中文大标题。
 */

/** 白色卡片背景不透明度（0.35~1.0），由主题设置里的滑块控制，只影响底板，不影响文字。 */
val LocalSenseiCardAlpha = androidx.compose.runtime.staticCompositionLocalOf { 0.94f }

object Sensei {
    val PaperTop = Color(0xFFF2FAFF)
    val PaperBottom = Color(0xFFE4F1FE)
    val Grid = Color(0xFFBBD8F0)

    val Card = Color(0xF2FFFFFF)
    val CardSoft = Color(0xE6FFFFFF)
    val Stroke = Color(0xFFCFE5F7)

    val Ink = Color(0xFF1B2A38)
    val InkSoft = Color(0xFF6B8199)
    val InkFaint = Color(0xFF9DB2C6)

    val Sky = Color(0xFF3E9BE0)
    val SkyDeep = Color(0xFF1F6FB2)
    val SkySoft = Color(0xFFBFE2FB)

    val Pink = Color(0xFFF58AB8)
    val PinkDeep = Color(0xFFE4679C)
    val PinkWash = Color(0xFFFDE7F0)

    val Mint = Color(0xFF57C9A4)
    val Amber = Color(0xFFF0B24B)

    /** 学生立绘配色（占位用，真立绘放 assets/sensei/ 即可替换） */
    val StudentPalettes = listOf(
        Color(0xFF8E7CF0) to Color(0xFFD9D2FF), // 紫
        Color(0xFFF58AB8) to Color(0xFFFDE0EC), // 粉
        Color(0xFF4EA8E8) to Color(0xFFD6ECFC), // 蓝
        Color(0xFFF08A8A) to Color(0xFFFFE0E0), // 珊瑚
        Color(0xFF6C7BA8) to Color(0xFFDCE3F5), // 靛
        Color(0xFFE0B25C) to Color(0xFFFBEDD0), // 金
        Color(0xFFB79BE0) to Color(0xFFEDE3FA), // 薰衣草
    )

    val gridBrush: Brush = Brush.verticalGradient(listOf(PaperTop, PaperBottom))
}

/** 白色卡片：圆角 24dp + 柔光阴影 + 细描边。 */
@Composable
fun SenseiCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    padding: Dp = 18.dp,
    color: Color = Sensei.Card,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color(0x2A5C93C4),
                spotColor = Color(0x334B84B8),
                clip = false,
            )
            .clip(shape)
            .background(color)
            .padding(padding),
        content = content,
    )
}

/** 英文小标，比如 MISSION 01 / SETUP。 */
@Composable
fun SenseiSectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Sensei.InkFaint) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
    )
}

/** 状态点。 */
@Composable
fun StatusDot(color: Color, size: Dp = 6.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

/** 粉色 SENSEI ONLY 徽章。 */
@Composable
fun SenseiOnlyBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Sensei.PinkWash)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("✦", color = Sensei.PinkDeep, fontSize = 11.sp)
        Text(
            "SENSEI ONLY",
            color = Sensei.PinkDeep,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
    }
}

/** 顶部分区标题：左侧英文小标 + 右侧状态。 */
@Composable
fun SenseiHeader(title: String, trailing: String? = null, trailingColor: Color = Sensei.Mint) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SenseiSectionLabel(title)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(trailingColor)
                Text(trailing, color = Sensei.InkFaint, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
        }
    }
}

/** 圆形学生头像占位：渐变底 + 首字。 */
@Composable
fun StudentAvatar(
    index: Int,
    label: String,
    size: Dp = 46.dp,
    ring: Color = Color.White,
    bitmap: android.graphics.Bitmap? = null,
) {
    val (main, soft) = Sensei.StudentPalettes[index % Sensei.StudentPalettes.size]
    Box(
        modifier = Modifier
            .size(size + 4.dp)
            .clip(CircleShape)
            .background(ring)
            .padding(2.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(soft, main))),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = label,
                color = Color.White.copy(alpha = LocalSenseiCardAlpha.current),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.34f).sp,
            )
        }
    }
}

/** 页面底纹：浅蓝渐变 + 网格线。 */
@Composable
fun SenseiBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Sensei.gridBrush)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val step = 44.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(Sensei.Grid, start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f, alpha = 0.35f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(Sensei.Grid, start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f, alpha = 0.35f)
                y += step
            }
        }
        content()
    }
}

@Composable
fun SenseiScreenTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = Sensei.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Sensei.InkSoft, fontSize = 12.sp)
    }
}

/** 细分割线。 */
@Composable
fun SenseiDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Sensei.Stroke.copy(alpha = 0.6f)),
    )
}

internal val cardGap = 12.dp
internal val pagePadding = 16.dp

/** 供页面引用，避免各处硬编码字重。 */
internal val titleStyle = androidx.compose.ui.text.TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    color = Sensei.Ink,
)

internal val bodyStyle = androidx.compose.ui.text.TextStyle(
    fontSize = 13.sp,
    color = Sensei.InkSoft,
)
