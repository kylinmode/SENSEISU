package com.senseisu.ssu.ui.sensei

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val active = index == selected
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { go(index) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (active) item.iconSelected else item.iconNotSelected,
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            text = stringResource(item.label),
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
