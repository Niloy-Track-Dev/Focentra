package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.PomodoroPhase
import com.example.engine.SessionMode
import com.example.engine.TimerStatus
import com.example.ui.components.DistractionLoggerDialog
import com.example.ui.theme.FullScreenThemePresets
import com.example.viewmodel.MainViewModel

@Composable
fun FullScreenFocusScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val timerState by viewModel.timerEngine.uiState.collectAsStateWithLifecycle()
    val fullScreenThemeId by viewModel.fullScreenTheme.collectAsStateWithLifecycle()
    val activeTheme = remember(fullScreenThemeId) { FullScreenThemePresets.getPresetById(fullScreenThemeId) }

    var showDistractionDialog by remember { mutableStateOf(false) }
    var showBrainDumpDialog by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    val brainDumpNotes by viewModel.brainDumpNotes.collectAsStateWithLifecycle()

    val quotes = remember {
        listOf(
            "Deep work is the ability to focus without distraction on a cognitively demanding task.",
            "Discipline is choosing between what you want now and what you want most.",
            "Energy flows where attention goes.",
            "Small daily improvements over time lead to stunning results.",
            "Focus is a muscle. The more you practice, the stronger it becomes.",
            "Don't count the days, make the days count."
        )
    }

    val currentQuote = remember(timerState.sessionStartTime) {
        quotes[(timerState.sessionStartTime % quotes.size).toInt().coerceIn(0, quotes.size - 1)]
    }

    if (showDistractionDialog) {
        DistractionLoggerDialog(
            onDismiss = { showDistractionDialog = false },
            onLogCategory = { cat ->
                viewModel.timerEngine.addDistraction(cat)
            }
        )
    }

    if (showBrainDumpDialog) {
        com.example.ui.components.BrainDumpDialog(
            notes = brainDumpNotes,
            onDismiss = { showBrainDumpDialog = false },
            onAddNote = { text -> viewModel.addBrainDumpNote(text, timerState.subject) },
            onToggleDone = { id -> viewModel.toggleBrainDumpDone(id) },
            onDeleteNote = { id -> viewModel.deleteBrainDumpNote(id) },
            onClearCompleted = { viewModel.clearCompletedBrainDumps() }
        )
    }

    val backgroundModifier = if (activeTheme.backgroundGradient != null) {
        Modifier.background(Brush.verticalGradient(activeTheme.backgroundGradient))
    } else {
        Modifier.background(activeTheme.backgroundColor)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(backgroundModifier)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Bar: Exit Fullscreen button & Subject
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setFullScreenFocus(false) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(activeTheme.cardBackgroundColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = activeTheme.cardTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = activeTheme.cardBackgroundColor,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(activeTheme.accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timerState.subject + if (timerState.topic.isNotBlank()) " • ${timerState.topic}" else "",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = activeTheme.cardTextColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Center Clock & Motivation
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isRunning = timerState.status == TimerStatus.RUNNING
            val isPaused = timerState.status == TimerStatus.PAUSED

            val timeString = when (timerState.mode) {
                SessionMode.COUNTDOWN, SessionMode.POMODORO -> {
                    val min = timerState.remainingSeconds / 60
                    val sec = timerState.remainingSeconds % 60
                    String.format("%02d:%02d", min, sec)
                }
                SessionMode.STOPWATCH -> {
                    val hrs = timerState.focusedSeconds / 3600
                    val min = (timerState.focusedSeconds % 3600) / 60
                    val sec = timerState.focusedSeconds % 60
                    if (hrs > 0) String.format("%02d:%02d:%02d", hrs, min, sec)
                    else String.format("%02d:%02d", min, sec)
                }
            }

            Text(
                text = timeString,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-2).sp
                ),
                color = if (isPaused) Color(0xFFEF4444) else activeTheme.clockColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pomodoro phase or status
            val statusLabel = when {
                isPaused -> "PAUSED"
                timerState.mode == SessionMode.POMODORO -> {
                    when (timerState.pomodoroPhase) {
                        PomodoroPhase.FOCUS -> "FOCUS ROUND ${timerState.currentRound}/${timerState.totalRounds}"
                        PomodoroPhase.SHORT_BREAK -> "SHORT BREAK"
                        PomodoroPhase.LONG_BREAK -> "LONG BREAK"
                    }
                }
                isRunning -> "DEEP FOCUS MODE"
                else -> "IDLE"
            }

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = activeTheme.accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "“$currentQuote”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = activeTheme.quoteColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Bottom Controls
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distraction Button
                IconButton(
                    onClick = { showDistractionDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(activeTheme.buttonBackgroundColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Log Distraction",
                        tint = activeTheme.buttonIconColor
                    )
                }

                // Brain Dump / Focus Notes Button
                IconButton(
                    onClick = { showBrainDumpDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(activeTheme.buttonBackgroundColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Focus Brain Dump",
                        tint = activeTheme.buttonIconColor
                    )
                }

                // Play / Pause Button
                Button(
                    onClick = {
                        if (timerState.status == TimerStatus.RUNNING) {
                            viewModel.timerEngine.pauseSession()
                        } else {
                            viewModel.timerEngine.resumeSession()
                        }
                    },
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.accentColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (timerState.status == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pause / Resume",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Finish Button
                IconButton(
                    onClick = {
                        viewModel.timerEngine.finishSession(isEarly = true)
                        viewModel.setFullScreenFocus(false)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(activeTheme.accentColor.copy(alpha = 0.25f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Finish Session",
                        tint = activeTheme.accentColor
                    )
                }
            }
        }
    }
}
