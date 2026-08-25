package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.SessionMode
import com.example.engine.TimerStatus
import com.example.ui.components.*
import com.example.viewmodel.MainViewModel

@Composable
fun TimerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val timerState by viewModel.timerEngine.uiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableStateOf(SessionMode.COUNTDOWN) }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var topicText by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("") }
    var selectedDurationMinutes by remember { mutableIntStateOf(25) }

    // Pomodoro specific setup
    var pomodoroFocusMin by remember { mutableIntStateOf(25) }
    var pomodoroShortBreakMin by remember { mutableIntStateOf(5) }
    var pomodoroLongBreakMin by remember { mutableIntStateOf(15) }
    var pomodoroRounds by remember { mutableIntStateOf(4) }
    var autoStartBreaks by remember { mutableStateOf(false) }

    var showDistractionDialog by remember { mutableStateOf(false) }

    // Sync subject default if subjects loaded
    LaunchedEffect(subjects) {
        if (subjects.isNotEmpty() && !subjects.any { it.name == selectedSubject }) {
            selectedSubject = subjects.first().name
        }
    }

    if (timerState.showCompletionSheet && timerState.completedSessionData != null) {
        SessionCompletionDialog(
            session = timerState.completedSessionData!!,
            onDismiss = { viewModel.timerEngine.dismissCompletionSheet() },
            onSave = { rating, mood, energy, notes, loc ->
                viewModel.timerEngine.saveCompletedSessionWithFeedback(rating, mood, energy, notes, loc)
            }
        )
    }

    if (showDistractionDialog) {
        DistractionLoggerDialog(
            onDismiss = { showDistractionDialog = false },
            onLogCategory = { cat ->
                viewModel.timerEngine.addDistraction(cat)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (timerState.status == TimerStatus.RUNNING || timerState.status == TimerStatus.PAUSED) {
            // Active Timer View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TimerProgressRing(
                    state = timerState,
                    onPauseClick = { viewModel.timerEngine.pauseSession() },
                    onResumeClick = { viewModel.timerEngine.resumeSession() },
                    onFinishClick = { viewModel.timerEngine.finishSession(isEarly = true) },
                    onCancelClick = { viewModel.timerEngine.cancelSession() },
                    onLogDistractionClick = { showDistractionDialog = true },
                    onToggleFullScreen = { viewModel.setFullScreenFocus(true) }
                )
            }
        } else {
            // Setup & Configuration View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Mode Selector Tabs
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMode == SessionMode.COUNTDOWN,
                        onClick = { selectedMode = SessionMode.COUNTDOWN },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Timer")
                    }

                    SegmentedButton(
                        selected = selectedMode == SessionMode.POMODORO,
                        onClick = { selectedMode = SessionMode.POMODORO },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AvTimer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Pomodoro")
                    }

                    SegmentedButton(
                        selected = selectedMode == SessionMode.STOPWATCH,
                        onClick = { selectedMode = SessionMode.STOPWATCH },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Stopwatch")
                    }
                }

                // Subject Picker
                Column {
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { sub ->
                            val isSelected = sub.name == selectedSubject
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubject = sub.name },
                                label = { Text(sub.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(parseColorHex(sub.colorHex))
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Topic & Goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = topicText,
                        onValueChange = { topicText = it },
                        label = { Text("Topic (Optional)") },
                        placeholder = { Text("e.g. Calculus") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it },
                        label = { Text("Session Goal") },
                        placeholder = { Text("e.g. 10 problems") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                // Mode-specific configuration
                when (selectedMode) {
                    SessionMode.COUNTDOWN -> {
                        Column {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val durationOptions = listOf(5, 10, 15, 25, 30, 45, 60, 90, 120)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(durationOptions) { mins ->
                                    val isSelected = selectedDurationMinutes == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDurationMinutes = mins },
                                        label = { Text("${mins}m") },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    SessionMode.POMODORO -> {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Pomodoro Intervals",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Focus Duration", style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (pomodoroFocusMin > 5) pomodoroFocusMin -= 5 }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                                        }
                                        Text("${pomodoroFocusMin}m", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        IconButton(onClick = { if (pomodoroFocusMin < 120) pomodoroFocusMin += 5 }) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Short Break", style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (pomodoroShortBreakMin > 1) pomodoroShortBreakMin -= 1 }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                                        }
                                        Text("${pomodoroShortBreakMin}m", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        IconButton(onClick = { if (pomodoroShortBreakMin < 30) pomodoroShortBreakMin += 1 }) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Rounds", style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (pomodoroRounds > 1) pomodoroRounds -= 1 }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                                        }
                                        Text("$pomodoroRounds", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        IconButton(onClick = { if (pomodoroRounds < 12) pomodoroRounds += 1 }) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SessionMode.STOPWATCH -> {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Stopwatch counts open-ended study time. You can pause or finish anytime.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Presets quick picks
                if (presets.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Preset Templates",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(presets) { preset ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        viewModel.startTimerWithPreset(preset)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Big Start Button
                Button(
                    onClick = {
                        viewModel.timerEngine.configureSession(
                            mode = selectedMode,
                            subject = selectedSubject,
                            topic = topicText,
                            goal = goalText,
                            durationMinutes = selectedDurationMinutes,
                            pomodoroFocusMin = pomodoroFocusMin,
                            pomodoroShortBreakMin = pomodoroShortBreakMin,
                            pomodoroLongBreakMin = pomodoroLongBreakMin,
                            pomodoroRounds = pomodoroRounds,
                            autoStartBreaks = autoStartBreaks
                        )
                        viewModel.timerEngine.startSession()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val startLabel = when (selectedMode) {
                        SessionMode.COUNTDOWN -> "Start ${selectedDurationMinutes}m Focus Timer"
                        SessionMode.POMODORO -> "Start Pomodoro ($pomodoroFocusMin/$pomodoroShortBreakMin)"
                        SessionMode.STOPWATCH -> "Start Stopwatch"
                    }
                    Text(
                        text = startLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
