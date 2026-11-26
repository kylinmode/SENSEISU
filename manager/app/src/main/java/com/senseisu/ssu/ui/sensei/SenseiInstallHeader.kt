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

/** 仿底部栏的触摸效果：纯色填充变化，无涟漪、无阴影 */
@Composable
private fun PillTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE3EEFB))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("修补文件", "本机刷入").forEachIndexed { index, label ->
            val active = selected == index
            val bg by animateColorAsState(
                targetValue = if (active) Color.White else Color.Transparent,
                label = "pillBg",
            )
            val fg by animateColorAsState(
                targetValue = if (active) Color(0xFF1B2B44) else Color(0xFF5B6B80),
                label = "pillFg",
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    // indication = null → 点按没有灰色方块/涟漪
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = fg,
                )
            }
        }
    }
}
