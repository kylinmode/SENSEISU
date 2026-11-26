package com.senseisu.ssu.ui.sensei

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.senseisu.ssu.Natives
import com.senseisu.ssu.ui.screen.BottomBarDestination

/**
 * BakaSU 风格底栏：悬浮胶囊 + 图标 + 小字标签。
 * 选中项用一个浅色圆底高亮。
 */
@Composable
fun SenseiBottomBar(
    navController: NavHostController,
    lastValidSelection: MutableState<Int>,
) {
    val navigator = navController.rememberDestinationsNavigator()
    val fullFeatured = Natives.isFullFeatured()
    val items = remember(fullFeatured) {
        BottomBarDestination.entries.filter { fullFeatured || !it.rootRequired }
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val selected = items.indexOfFirst { it.direction.route == currentRoute }

    if (selected != -1) lastValidSelection.value = selected

    fun go(index: Int) {
        val dest: DirectionDestinationSpec = items.getOrNull(index)?.direction ?: return
        if (dest.route == currentRoute) return
        navigator.navigate(dest) {
            popUpTo(NavGraphs.root.startRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .clipToBounds()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                items.forEachIndexed { index, item ->
                    val active = index == selected
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(70.dp)
                            .height(47.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .drawBehind {
                                if (active) {
                                    drawRoundRect(
                                        color = highlightColor,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                                    )
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { go(index) },
                    ) {
                        Icon(
                            imageVector = item.iconNotSelected,
                            contentDescription = null,
                            tint = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = stringResource(item.label),
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
