package com.senseisu.ssu.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.senseisu.ssu.BuildConfig
import com.senseisu.ssu.R

private data class Credit(val name: String, val url: String)

private val credits = listOf(
    Credit("KernelSU", "https://github.com/tiann/KernelSU"),
    Credit("KernelSU-Next", "https://github.com/KernelSU-Next/KernelSU-Next"),
    Credit("SukiSU", "https://github.com/SukiSU-Ultra/SukiSU-Ultra"),
)

private const val PROJECT_URL = "https://github.com/kylinmode/SENSEISU"

@Preview
@Composable
fun AboutCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            AboutCardContent()
        }
    }
}

@Composable
fun AboutDialog(dismiss: () -> Unit) {
    Dialog(onDismissRequest = { dismiss() }) {
        AboutCard()
    }
}

@Composable
private fun AboutCardContent() {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            // 项目自己的 logo（不再是上游图标）
            Surface(
                modifier = Modifier.size(40.dp),
                color = colorResource(id = R.color.ssu_background),
                shape = CircleShape
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ssu_foreground),
                    contentDescription = null,
                    modifier = Modifier.scale(1.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 致谢：三个上游项目
        Text(
            text = "致谢 / Credits",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        credits.forEach { credit ->
            Text(
                text = credit.name,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { runCatching { uriHandler.openUri(credit.url) } }
                    .padding(vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 末尾：本项目在 GitHub 上的地址
        Text(
            text = stringResource(id = R.string.about_source_code, "GitHub"),
            style = TextStyle(fontSize = 14.sp)
        )
        Text(
            text = PROJECT_URL,
            style = TextStyle(
                fontSize = 14.sp,
                color = linkColor,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { runCatching { uriHandler.openUri(PROJECT_URL) } }
                .padding(top = 3.dp)
        )
    }
}
