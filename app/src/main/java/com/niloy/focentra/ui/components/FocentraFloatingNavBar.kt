package com.niloy.focentra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.focentra.viewmodel.NavigationTab

data class NavTabItem(
    val tab: NavigationTab,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

@Composable
fun FocentraFloatingNavBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            NavTabItem(
                tab = NavigationTab.DASHBOARD,
                label = "Home",
                activeIcon = Icons.Filled.Dashboard,
                inactiveIcon = Icons.Outlined.Dashboard
            ),
            NavTabItem(
                tab = NavigationTab.TIMER,
                label = "Timer",
                activeIcon = Icons.Filled.HourglassBottom,
                inactiveIcon = Icons.Outlined.Timer
            ),
            NavTabItem(
                tab = NavigationTab.ANALYTICS,
                label = "Stats",
                activeIcon = Icons.Filled.Leaderboard,
                inactiveIcon = Icons.Outlined.Leaderboard
            ),
            NavTabItem(
                tab = NavigationTab.CALENDAR,
                label = "Calendar",
                activeIcon = Icons.Filled.CalendarMonth,
                inactiveIcon = Icons.Outlined.CalendarToday
            ),
            NavTabItem(
                tab = NavigationTab.SETTINGS,
                label = "Settings",
                activeIcon = Icons.Filled.Tune,
                inactiveIcon = Icons.Outlined.Tune
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { item ->
                    val isSelected = currentTab == item.tab
                    FloatingNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onTabSelected(item.tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: NavTabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        label = "icon_color"
    )

    val pillBackground by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "pill_bg"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(pillBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.1.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1
            )
        }
    }
}
