package com.senseisu.ssu.ui.sensei

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 安装页顶部：返回 + 标题 + 两个标签（修补文件 / 本机刷入） */
@Composable
fun SenseiInstallHeader(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF1B2B44))
            }
            Text("安装", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B2B44))
        }

        Row(
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE3EEFB))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("修补文件", "本机刷入").forEachIndexed { index, label ->
                val selected = selectedTab == index
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) Color.White else Color.Transparent,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier.clickable { onSelectTab(index) },
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color(0xFF1B2B44) else Color(0xFF5B6B80),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}
