package com.senseisu.ssu.ui.sensei

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 圆角图片横幅：底部渐隐，避免压到文字 */
@Composable
fun BaBanner(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    corner: Dp = 20.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x14101820), Color(0x4D101820))
                    )
                )
        )
    }
}

/** 空态：插画 + 一句 BA 风格提示 */
@Composable
fun BaEmptyState(
    @DrawableRes res: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(190.dp)
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x0D101820), Color(0x66101820))
                        )
                    )
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Sensei.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            fontSize = 12.sp,
            color = Sensei.InkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}
