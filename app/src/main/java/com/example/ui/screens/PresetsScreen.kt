package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.PresetEntity
import com.example.ui.components.EmptyStateView
import com.example.viewmodel.MainViewModel

@Composable
fun PresetsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<PresetEntity?>(null) }

    if (showCreateDialog) {
        CreatePresetDialog(
            subjects = subjects.map { it.name },
            onDismiss = { showCreateDialog = false },
            onSave = { newPreset ->
                viewModel.savePreset(newPreset)
                showCreateDialog = false
            }
        )
    }

    if (presetToDelete != null) {
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text("Delete Preset?") },
            text = { Text("Are you sure you want to delete «${presetToDelete!!.title}»?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePreset(presetToDelete!!)
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Preset") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (presets.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.BookmarkBorder,
                title = "No study presets",
                description = "Save your favorite timer configurations for one-tap starting.",
                actionButtonText = "Create Preset",
                onActionClick = { showCreateDialog = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        onStartClick = { viewModel.startTimerWithPreset(preset) },
                        onDeleteClick = { presetToDelete = preset }
                    )
                }
            }
        }
    }
}

@Composable
fun PresetCard(
    preset: PresetEntity,
    onStartClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (preset.type == "POMODORO") Icons.Default.AvTimer else Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val detailText = when (preset.type) {
                        "POMODORO" -> "${preset.pomodoroFocusMin}m focus / ${preset.pomodoroShortBreakMin}m break • ${preset.pomodoroRounds} rounds"
                        "STOPWATCH" -> "Open stopwatch"
                        else -> "${preset.durationMinutes}m duration"
                    }
                    Text(
                        text = "${preset.subject} • $detailText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                FilledTonalButton(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start")
                }
            }
        }
    }
}

@Composable
fun CreatePresetDialog(
    subjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (PresetEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("POMODORO") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "General Study") }
    var topic by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("25") }
    var pomodoroFocusMin by remember { mutableStateOf("25") }
    var pomodoroShortBreakMin by remember { mutableStateOf("5") }
    var pomodoroLongBreakMin by remember { mutableStateOf("15") }
    var pomodoroRounds by remember { mutableStateOf("4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Preset Title") },
                    placeholder = { Text("e.g. Deep Math Sprint") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Mode", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("POMODORO", "COUNTDOWN", "STOPWATCH").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                if (selectedType == "POMODORO") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pomodoroFocusMin,
                            onValueChange = { pomodoroFocusMin = it },
                            label = { Text("Focus (m)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = pomodoroShortBreakMin,
                            onValueChange = { pomodoroShortBreakMin = it },
                            label = { Text("Break (m)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                } else if (selectedType == "COUNTDOWN") {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text("Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            PresetEntity(
                                title = title,
                                type = selectedType,
                                subject = selectedSubject,
                                topic = topic,
                                durationMinutes = durationMinutes.toIntOrNull() ?: 25,
                                pomodoroFocusMin = pomodoroFocusMin.toIntOrNull() ?: 25,
                                pomodoroShortBreakMin = pomodoroShortBreakMin.toIntOrNull() ?: 5,
                                pomodoroLongBreakMin = pomodoroLongBreakMin.toIntOrNull() ?: 15,
                                pomodoroRounds = pomodoroRounds.toIntOrNull() ?: 4
                            )
                        )
                    }
                }
            ) {
                Text("Save Preset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
