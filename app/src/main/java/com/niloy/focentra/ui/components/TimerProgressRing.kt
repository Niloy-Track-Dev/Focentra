package com.niloy.focentra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.focentra.engine.PomodoroPhase
import com.niloy.focentra.engine.SessionMode
import com.niloy.focentra.engine.TimerStatus
import com.niloy.focentra.engine.TimerUiState

@Composable
fun TimerProgressRing(
    state: TimerUiState,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    onCancelClick: () -> Unit,
    onLogDistractionClick: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    ringSize: Dp = 290.dp
) {
    val isRunning = state.status == TimerStatus.RUNNING
    val isPaused = state.status == TimerStatus.PAUSED

    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 600),
        label = "timer_progress"
    )

    val primaryGradient = Brush.sweepGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary
        )
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status & Phase Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = when {
                isPaused -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                state.mode == SessionMode.POMODORO && state.pomodoroPhase != PomodoroPhase.FOCUS ->
                    MaterialTheme.colorScheme.secondaryContainer
                isRunning -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusDotColor = when {
                    isPaused -> MaterialTheme.colorScheme.error
                    isRunning -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val badgeText = when {
                    isPaused -> "PAUSED"
                    state.mode == SessionMode.POMODORO -> {
                        when (state.pomodoroPhase) {
                            PomodoroPhase.FOCUS -> "FOCUSING • ROUND ${state.currentRound}/${state.totalRounds}"
                            PomodoroPhase.SHORT_BREAK -> "SHORT BREAK (${state.pomodoroShortBreakMinutes}m)"
                            PomodoroPhase.LONG_BREAK -> "LONG BREAK (${state.pomodoroLongBreakMinutes}m)"
                        }
                    }
                    state.mode == SessionMode.STOPWATCH -> "STOPWATCH ACTIVE"
                    isRunning -> "FOCUSING"
                    else -> "READY"
                }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Circular Timer Canvas
        Box(
            modifier = Modifier
                .size(ringSize)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Track Background
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )

                // Progress Arc
                val sweepAngle = if (state.mode == SessionMode.STOPWATCH) {
                    360f // Full active ring for stopwatch
                } else {
                    (1.0f - animatedProgress) * 360f
                }

                drawArc(
                    brush = primaryGradient,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Time readout
                val timeString = when (state.mode) {
                    SessionMode.COUNTDOWN, SessionMode.POMODORO -> {
                        val min = state.remainingSeconds / 60
                        val sec = state.remainingSeconds % 60
                        String.format("%02d:%02d", min, sec)
                    }
                    SessionMode.STOPWATCH -> {
                        val hrs = state.focusedSeconds / 3600
                        val min = (state.focusedSeconds % 3600) / 60
                        val sec = state.focusedSeconds % 60
                        if (hrs > 0) String.format("%02d:%02d:%02d", hrs, min, sec)
                        else String.format("%02d:%02d", min, sec)
                    }
                }

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = state.subject,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (state.topic.isNotBlank()) {
                    Text(
                        text = state.topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Distraction counter pill if any logged
                if (state.distractionCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.distractionCount} distractions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Pomodoro Round Indicators
        if (state.mode == SessionMode.POMODORO) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (r in 1..state.totalRounds) {
                    val isDone = r < state.currentRound
                    val isCurrent = r == state.currentRound
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone -> MaterialTheme.colorScheme.primary
                                    isCurrent -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel / Reset Button
            IconButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel session",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Distraction Logger
            IconButton(
                onClick = onLogDistractionClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Log Distraction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Primary Play / Pause Button (Hero)
            Button(
                onClick = {
                    if (isRunning) onPauseClick() else onResumeClick()
                },
                modifier = Modifier
                    .size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start/Resume",
                    modifier = Modifier.size(34.dp)
                )
            }

            // Finish Button
            IconButton(
                onClick = onFinishClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Finish session",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Full-screen Focus Mode Toggle
            IconButton(
                onClick = onToggleFullScreen,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Full Screen Focus",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
