package com.senseisu.ssu.ui.sensei

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 固定挂在各页面角落的角色（不做轮播）。 */
private fun museForPage(page: Int): String? = when (page) {
    1 -> "aru"      // 超级用户
    2 -> "shun"     // 模块
    3 -> "asuna"    // 设置
    else -> null    // 首页 hero 自带 白子/星野/优香
}

private fun museLabel(name: String): Pair<String, String> = when (name) {
    "aru" -> "阿露" to "ARU"
    "shun" -> "瞬" to "SHUN"
    "asuna" -> "亚丝娜" to "ASUNA"
    "hina" -> "日奈" to "HINA"
    "shiroko" -> "白子" to "SHIROKO"
    "hoshino" -> "星野" to "HOSHINO"
    "yuuka" -> "优香" to "YUUKA"
    else -> name to name.uppercase()
}

@Composable
private fun loadMuse(name: String): android.graphics.Bitmap? {
    val context = LocalContext.current
    return remember(name) {
        runCatching {
            context.assets.open("sensei/muse/$name.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
}

/**
 * 页面角落的固定角色立绘：右下角、半透明、不吃触摸事件。
 * 只给超级用户/模块/设置页用（首页由 hero 卡片负责）。
 */
@Composable
fun SenseiMuse(page: Int, modifier: Modifier = Modifier) {
    val name = museForPage(page) ?: return
    val bmp = loadMuse(name) ?: return
    val (cn, en) = museLabel(name)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(end = 6.dp, bottom = 84.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(300.dp)
                    .width(200.dp),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Sensei.CardSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(Sensei.Pink, size = 5.dp)
                Spacer(Modifier.width(6.dp))
                Text(cn, color = Sensei.Ink, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Text(en, color = Sensei.InkFaint, fontSize = 7.sp, letterSpacing = 0.6.sp)
            }
        }
    }
}

/** 点学生头像弹出的全屏立绘。 */
@Composable
fun SenseiMuseFullscreen(name: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = name != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val bmp = name?.let { loadMuse(it) }
        val (cn, en) = museLabel(name ?: "")
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xF2EAF5FF), Color(0xFFCFE6FB))))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(520.dp).fillMaxWidth(0.92f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Sensei.Card)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(cn, color = Sensei.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(en, color = Sensei.InkFaint, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("轻触任意处返回", color = Sensei.InkFaint, fontSize = 10.sp)
            }
        }
    }
}
