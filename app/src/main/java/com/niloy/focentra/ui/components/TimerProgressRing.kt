package com.niloy.focentra.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
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
    ringSize: Dp = 295.dp
) {
    val isRunning = state.status == TimerStatus.RUNNING
    val isPaused = state.status == TimerStatus.PAUSED

    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 600),
        label = "timer_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isRunning) 0.15f else 0.05f,
        targetValue = if (isRunning) 0.35f else 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val primaryGradient = Brush.sweepGradient(
        listOf(
            primaryColor,
            secondaryColor,
            primaryColor
        )
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status & Phase Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = when {
                isPaused -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                state.mode == SessionMode.POMODORO && state.pomodoroPhase != PomodoroPhase.FOCUS ->
                    MaterialTheme.colorScheme.secondaryContainer
                isRunning -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .padding(bottom = 16.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
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
                    isRunning -> "FOCUSING IN THE ZONE"
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

        // Circular Timer Canvas with Ambient Glow Aura
        Box(
            modifier = Modifier
                .size(ringSize)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Aura
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isRunning) {
                    drawCircle(
                        color = primaryColor.copy(alpha = pulseGlowAlpha),
                        radius = size.minDimension / 2f
                    )
                }

                val strokeWidth = 15.dp.toPx()
                // Track Background
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )

                // Progress Arc
                val sweepAngle = if (state.mode == SessionMode.STOPWATCH) {
                    360f
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
                        letterSpacing = (-1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = state.subject,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (state.topic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
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
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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
                    .size(50.dp)
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
                    .size(50.dp)
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
                    .size(76.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary),
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
                    modifier = Modifier.size(36.dp)
                )
            }

            // Finish Button
            IconButton(
                onClick = onFinishClick,
                modifier = Modifier
                    .size(50.dp)
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
                    .size(50.dp)
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
