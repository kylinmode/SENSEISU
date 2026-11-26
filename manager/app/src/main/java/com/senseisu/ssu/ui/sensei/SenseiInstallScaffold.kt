package com.senseisu.ssu.ui.sensei

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 安装页的 Sensei 外观外壳。
 *
 * 只负责：背景、标题、返回按钮、内容容器。
 * 刷入逻辑由上游 InstallContent 提供（ksud 调用、A/B 槽、备份等都不改）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenseiInstallScaffold(
    navigator: DestinationsNavigator,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEAF4FF), Color(0xFFF7FBFF), Color.White)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = { runCatching { navigator.navigateUp() } }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF1B2B44),
                )
            }
            Text(
                "安装",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B2B44),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
        ) {
            content()
        }

        // 本机刷入：选一个已修补的镜像，直接写当前设备分区（需要 root）
        SenseiLocalFlashBar()
    }
}
