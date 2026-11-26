package com.senseisu.ssu.ui.sensei

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 独立背景层：只负责画背景，绝不改变内容层结构。
 *
 * 与 BakaSU 相同的容错策略：
 *  - URI 失效 / 图片损坏 → 静默跳过，只留纯色底
 *  - 大图降采样，避免 OOM
 *  - 任何异常都不向上抛
 */
@Composable
fun SenseiBackgroundLayer(uriString: String?, blur: Boolean) {
    if (uriString.isNullOrBlank()) return

    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, uriString) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                // 先读尺寸，算降采样比例
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                val sample = run {
                    val longest = maxOf(opts.outWidth, opts.outHeight)
                    var s = 1
                    while (longest / s > 2048) s *= 2
                    s
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }.getOrNull()
        }
    }

    val bmp by bitmapState

    Box(modifier = Modifier.fillMaxSize()) {
        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blur) Modifier.blur(24.dp) else Modifier),
            )
            // 压暗一层保证文字可读
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.14f)))
        }
    }
}
