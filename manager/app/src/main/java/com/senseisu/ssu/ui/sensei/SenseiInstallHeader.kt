package com.senseisu.ssu.ui.sensei

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding

/**
 * 安装页顶部：
 *   ← 安装
 *      ( 修补文件 | 本机刷入 )   ← 水平居中，胶囊样式，无方形阴影
 */
@Composable
fun SenseiInstallHeader(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()          // 避开状态栏，不再重叠
            .padding(bottom = 6.dp)
    ) {
        // 标题行
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF1B2B44),
                )
            }
            Text("安装", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B2B44))
        }

        // 胶囊：水平居中
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            PillTabs(selected = selectedTab, onSelect = onSelectTab)
        }
    }
}

/** 滑动指示器式胶囊：选中块平滑滑动，点按无涟漪 */
@Composable
private fun PillTabs(selected: Int, onSelect: (Int) -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labels = listOf("修补文件", "本机刷入")
    // 每个 tab 的固定宽度（按最长文本估算，保证指示器可滑动且不抖动）
    val tabWidth = 84.dp
    val pad = 3.dp
    val offset by androidx.compose.animation.core.animateDpAsState(
        targetValue = pad + tabWidth * selected,
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "pillOffset",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE3EEFB))
            .padding(pad)
    ) {
        // 滑动底块
        Box(
            modifier = Modifier
                .offset(x = offset - pad)
                .width(tabWidth)
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            labels.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected == index) Color(0xFF1B2B44) else Color(0xFF5B6B80),
                    )
                }
            }
        }
    }
}
