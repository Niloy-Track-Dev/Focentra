package com.niloy.focentra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.focentra.viewmodel.AppNotification
import com.niloy.focentra.viewmodel.MainViewModel
import com.niloy.focentra.viewmodel.NotificationType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val seenNotificationIds by viewModel.seenNotificationIds.collectAsStateWithLifecycle()

    val initialSeenIds = remember { seenNotificationIds }

    LaunchedEffect(Unit) {
        viewModel.markNotificationsAsSeen()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        if (activeNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Notifications",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your revision alerts, goal trackers, and smart reminders will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(activeNotifications, key = { it.id }) { notif ->
                    val isUnseen = notif.id !in initialSeenIds
                    NotificationCardItem(
                        notification = notif,
                        isUnseen = isUnseen,
                        onDismiss = {
                            viewModel.dismissNotification(notif.id)
                        },
                        onStartRevision = { subjectName, _ ->
                            viewModel.dismissNotification(notif.id)
                            viewModel.startQuickTimer(30, subjectName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    notification: AppNotification,
    isUnseen: Boolean,
    onDismiss: () -> Unit,
    onStartRevision: (String, String) -> Unit
) {
    val cardBgColor = if (isUnseen) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val cardBorderColor = if (isUnseen) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val iconVector = when (notification.type) {
        NotificationType.REVISION_DUE -> Icons.Default.Autorenew
        NotificationType.INACTIVITY_WARNING -> Icons.Default.Warning
        NotificationType.GOAL_WARNING -> Icons.Default.AdsClick
        NotificationType.PEAK_FOCUS_REMINDER -> Icons.Default.Bolt
        NotificationType.STREAK_SAVER -> Icons.Default.LocalFireDepartment
        NotificationType.EXAM_COUNTDOWN -> Icons.Default.Timer
        NotificationType.BRAIN_DUMP_PROMPT -> Icons.Default.Lightbulb
        NotificationType.WEEKLY_DIGEST -> Icons.Default.EmojiEvents
        NotificationType.BREAK_OVERSTAY -> Icons.Default.Alarm
        NotificationType.AMBIENT_VIBE -> Icons.Default.VolumeUp
        NotificationType.MOTIVATION -> Icons.Default.Lightbulb
    }

    val iconTint = when (notification.type) {
        NotificationType.REVISION_DUE -> MaterialTheme.colorScheme.primary
        NotificationType.INACTIVITY_WARNING -> Color(0xFFF59E0B)
        NotificationType.GOAL_WARNING -> Color(0xFF10B981)
        NotificationType.PEAK_FOCUS_REMINDER -> Color(0xFF8B5CF6)
        NotificationType.STREAK_SAVER -> Color(0xFFEF4444)
        NotificationType.EXAM_COUNTDOWN -> Color(0xFFEC4899)
        NotificationType.BRAIN_DUMP_PROMPT -> Color(0xFF06B6D4)
        NotificationType.WEEKLY_DIGEST -> Color(0xFFF59E0B)
        NotificationType.BREAK_OVERSTAY -> Color(0xFFEF4444)
        NotificationType.AMBIENT_VIBE -> MaterialTheme.colorScheme.primary
        NotificationType.MOTIVATION -> MaterialTheme.colorScheme.primary
    }

    val timeFormatted = remember(notification.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(notification.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    if (isUnseen) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (notification.type == NotificationType.REVISION_DUE) "Bypass / Dismiss" else "Dismiss",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (notification.type == NotificationType.REVISION_DUE && notification.subjectName.isNotBlank()) {
                    Button(
                        onClick = { onStartRevision(notification.subjectName, notification.chapterName) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start 30m Revision",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
