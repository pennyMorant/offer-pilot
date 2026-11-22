@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val DockHeight = 64.dp

@Composable
fun BottomNavigationDock(
    selected: Destination,
    working: Boolean,
    authorized: Boolean,
    onSelect: (Destination) -> Unit,
    onWork: () -> Unit,
    onAuthorize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fabCorner by animateDpAsState(
        targetValue = if (working) 20.dp else DockHeight / 2,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "fabCorner",
    )
    val fabBg by animateColorAsState(
        targetValue = if (working) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "fabBg",
    )
    val fabFg = if (working) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onPrimary
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Row(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            shadowElevation = 4.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.height(DockHeight),
        ) {
            Row(
                Modifier.fillMaxHeight().padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Destination.entries.forEach { destination ->
                    val isSelected = selected == destination
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxHeight().clip(CircleShape)
                            .clickable { onSelect(destination) },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(destination.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
        Surface(
            onClick = { if (working || authorized) onWork() else onAuthorize() },
            color = fabBg,
            contentColor = fabFg,
            shape = RoundedCornerShape(fabCorner),
            shadowElevation = 6.dp,
            modifier = Modifier.size(DockHeight),
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = working,
                    transitionSpec = { fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec) },
                    label = "workFabIcon",
                ) { isWorking ->
                    Icon(
                        if (isWorking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (isWorking) "结束工作" else "开始工作",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
