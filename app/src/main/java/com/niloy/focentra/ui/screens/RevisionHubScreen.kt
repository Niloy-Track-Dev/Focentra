package com.niloy.focentra.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.focentra.data.local.entity.ChapterEntity
import com.niloy.focentra.engine.SessionMode
import com.niloy.focentra.engine.TimerStatus
import com.niloy.focentra.ui.components.TimerProgressRing
import com.niloy.focentra.ui.components.parseColorHex
import com.niloy.focentra.viewmodel.MainViewModel
import com.niloy.focentra.viewmodel.RevisionRecord
import java.text.SimpleDateFormat
import java.util.*

data class ScheduledRevisionItem(
    val chapter: ChapterEntity,
    val stageName: String,
    val dueDateMs: Long,
    val daysRemaining: Int
)

@Composable
fun RevisionHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Revision, 1: Scheduled, 2: History

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Tab Row
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = {}
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Revision", maxLines = 1, fontSize = 13.sp)
                }
            }

            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = {}
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scheduled", maxLines = 1, fontSize = 13.sp)
                }
            }

            SegmentedButton(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = {}
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("History", maxLines = 1, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> RevisionTimerSection(viewModel = viewModel)
            1 -> UpcomingScheduledRevisionsSection(viewModel = viewModel)
            2 -> RevisionHistorySection(viewModel = viewModel)
        }
    }
}

@Composable
private fun RevisionTimerSection(viewModel: MainViewModel) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val timerState by viewModel.timerEngine.uiState.collectAsStateWithLifecycle()

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "") }
    var selectedChapter by remember { mutableStateOf("") }

    var warningTitle by remember { mutableStateOf("") }
    var warningMessage by remember { mutableStateOf("") }
    var showSelectionWarningDialog by remember { mutableStateOf(false) }

    var pendingSaveData by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = "Cancel Revision Session?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to stop and cancel this revision session? Progress will not be recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.timerEngine.cancelSession()
                        viewModel.timerEngine.dismissCompletionSheet()
                        showCancelConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Continue Session")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSelectionWarningDialog) {
        AlertDialog(
            onDismissRequest = { showSelectionWarningDialog = false },
            title = {
                Text(
                    text = warningTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = warningMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSelectionWarningDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSaveDialog && pendingSaveData != null) {
        val (sub, ch, durMins) = pendingSaveData!!
        RevisionSaveDialog(
            subjectName = sub,
            chapterName = ch,
            durationMinutes = durMins,
            onDismiss = {
                showSaveDialog = false
                pendingSaveData = null
                viewModel.timerEngine.cancelSession()
                viewModel.timerEngine.dismissCompletionSheet()
            },
            onSave = { notes ->
                viewModel.recordRevision(
                    subjectName = sub,
                    chapterName = if (ch.isNotBlank()) ch else "General Revision",
                    durationMinutes = durMins,
                    stage = if (notes.isNotBlank()) notes else "Revision"
                )
                viewModel.timerEngine.cancelSession()
                viewModel.timerEngine.dismissCompletionSheet()
                showSaveDialog = false
                pendingSaveData = null
            }
        )
    }

    val matchingChapters = remember(chapters, selectedSubject) {
        if (selectedSubject.isBlank()) emptyList()
        else chapters.filter { it.subjectName.equals(selectedSubject, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Stopwatch Timer Ring inside Revision Hub
        if (timerState.status == TimerStatus.RUNNING || timerState.status == TimerStatus.PAUSED) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimerProgressRing(
                        state = timerState,
                        onPauseClick = { viewModel.timerEngine.pauseSession() },
                        onResumeClick = { viewModel.timerEngine.resumeSession() },
                        onFinishClick = {
                            val sub = timerState.subject
                            val ch = timerState.selectedChapter
                            val durMins = (timerState.focusedSeconds / 60).toInt().coerceAtLeast(1)
                            viewModel.timerEngine.finishSession(isEarly = true)
                            viewModel.timerEngine.dismissCompletionSheet()
                            pendingSaveData = Triple(sub, ch, durMins)
                            showSaveDialog = true
                        },
                        onCancelClick = {
                            showCancelConfirmDialog = true
                        },
                        onLogDistractionClick = { viewModel.timerEngine.addDistraction("Other") },
                        onToggleFullScreen = { viewModel.setFullScreenFocus(true) },
                        ringSize = 250.dp
                    )
                }
            }
        } else {
            // Subject Selector
            Column {
                Text(
                    text = "Select Subject",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects) { sub ->
                        val isSelected = sub.name == selectedSubject
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSubject = if (isSelected) "" else sub.name
                                selectedChapter = ""
                            },
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

            // Chapter Selector
            if (matchingChapters.isNotEmpty()) {
                Column {
                    Text(
                        text = "Select Chapter",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchingChapters) { ch ->
                            val cleanName = ch.chapterName.replace(Regex("(?i)\\bnoun\\b"), "").replace("Noun", "").replace("noun", "").trim()
                            val isSelected = ch.chapterName == selectedChapter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedChapter = if (isSelected) "" else ch.chapterName },
                                label = { Text(cleanName) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Revision Mode Info Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Revision Mode: Stopwatch",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (selectedSubject.isBlank() && selectedChapter.isBlank()) {
                        warningTitle = "Subject & Chapter Required"
                        warningMessage = "Please select both a subject and a chapter before starting your revision session."
                        showSelectionWarningDialog = true
                    } else if (selectedChapter.isBlank()) {
                        warningTitle = "Chapter Selection Required"
                        warningMessage = "Please select a chapter before starting your revision session."
                        showSelectionWarningDialog = true
                    } else {
                        if (timerState.status == TimerStatus.RUNNING || timerState.status == TimerStatus.PAUSED) {
                            viewModel.timerEngine.cancelSession()
                            viewModel.timerEngine.dismissCompletionSheet()
                        }
                        viewModel.timerEngine.configureSession(
                            mode = SessionMode.STOPWATCH,
                            subject = selectedSubject,
                            selectedChapter = selectedChapter,
                            topic = "Revision Stopwatch"
                        )
                        viewModel.timerEngine.startSession()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Stopwatch Revision",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun RevisionSaveDialog(
    subjectName: String,
    chapterName: String,
    durationMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (notes: String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Revision Completed!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$subjectName -> $chapterName",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Focused Duration: ${durationMinutes}m",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Revision Notes (Optional)") },
                    placeholder = { Text("Key formulas, weak points reviewed...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(notes) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UpcomingScheduledRevisionsSection(viewModel: MainViewModel) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    var selectedSubjectFilter by remember { mutableStateOf("All") }

    val scheduledList = remember(chapters, selectedSubjectFilter) {
        val list = mutableListOf<ScheduledRevisionItem>()
        val now = System.currentTimeMillis()
        val ONE_DAY = 86400000L

        val filteredChapters = if (selectedSubjectFilter == "All") {
            chapters
        } else {
            chapters.filter { it.subjectName.equals(selectedSubjectFilter, ignoreCase = true) }
        }

        filteredChapters.filter { it.tapCount >= 3 || it.status == "অধ্যায় Completely শেষ" }.forEach { ch ->
            val compTime = if (ch.completedTimestamp > 0) ch.completedTimestamp else ch.lastStudiedTimestamp

            val stages = listOf(
                Pair("1st Day (Quick Recap)", compTime + 1 * ONE_DAY),
                Pair("3rd Day (Detailed Review)", compTime + 3 * ONE_DAY),
                Pair("7th Day (Problem Solving)", compTime + 7 * ONE_DAY),
                Pair("15th Day (Final Master Test)", compTime + 15 * ONE_DAY)
            )

            stages.forEach { (stage, dueMs) ->
                if (dueMs >= now - ONE_DAY) {
                    val daysRemaining = ((dueMs - now) / ONE_DAY).toInt()
                    list.add(ScheduledRevisionItem(ch, stage, dueMs, daysRemaining))
                }
            }
        }
        list.sortedBy { it.dueDateMs }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Subject Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedSubjectFilter == "All",
                    onClick = { selectedSubjectFilter = "All" },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            items(subjects) { sub ->
                val isSelected = sub.name.equals(selectedSubjectFilter, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSubjectFilter = sub.name },
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

        if (scheduledList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Scheduled Revisions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "After completing a chapter, scientific revision schedules for days 1, 3, 7, and 15 will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(scheduledList) { item ->
                    ScheduledRevisionCard(
                        item = item,
                        onStart = {
                            if (viewModel.timerEngine.uiState.value.status == TimerStatus.RUNNING || viewModel.timerEngine.uiState.value.status == TimerStatus.PAUSED) {
                                viewModel.timerEngine.cancelSession()
                                viewModel.timerEngine.dismissCompletionSheet()
                            }
                            viewModel.timerEngine.configureSession(
                                mode = SessionMode.STOPWATCH,
                                subject = item.chapter.subjectName,
                                selectedChapter = item.chapter.chapterName,
                                topic = item.stageName
                            )
                            viewModel.timerEngine.startSession()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledRevisionCard(
    item: ScheduledRevisionItem,
    onStart: () -> Unit
) {
    val dateStr = remember(item.dueDateMs) {
        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(item.dueDateMs))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.stageName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = when {
                        item.daysRemaining < 0 -> "Overdue"
                        item.daysRemaining == 0 -> "Today"
                        item.daysRemaining == 1 -> "Tomorrow"
                        else -> "In ${item.daysRemaining} days"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (item.daysRemaining <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.chapter.subjectName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = item.chapter.chapterName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStart,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Stopwatch Revision", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun RevisionHistorySection(viewModel: MainViewModel) {
    val history by viewModel.revisionHistory.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var itemToDelete by remember { mutableStateOf<RevisionRecord?>(null) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete Revision History Item?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete «${itemToDelete?.subjectName} - ${itemToDelete?.chapterName}» from history?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteRevisionRecord(it.id) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val filteredHistory = remember(history, selectedSubjectFilter) {
        if (selectedSubjectFilter == "All") {
            history
        } else {
            history.filter { it.subjectName.equals(selectedSubjectFilter, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Subject Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedSubjectFilter == "All",
                    onClick = { selectedSubjectFilter = "All" },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            items(subjects) { sub ->
                val isSelected = sub.name.equals(selectedSubjectFilter, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSubjectFilter = sub.name },
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

        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryEdu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Revision History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed revision session logs will be stored here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredHistory, key = { it.id }) { rec ->
                    RevisionHistoryCard(
                        record = rec,
                        onLongClick = { itemToDelete = rec }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RevisionHistoryCard(
    record: RevisionRecord,
    onLongClick: () -> Unit
) {
    val dateAndRangeStr = remember(record.startTime, record.endTime, record.timestamp) {
        val dateSdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val dateText = dateSdf.format(Date(record.endTime))
        val startText = timeSdf.format(Date(record.startTime)).lowercase()
        val endText = timeSdf.format(Date(record.endTime)).lowercase()

        "$dateText • $startText - $endText"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.subjectName} -> ${record.chapterName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateAndRangeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${record.durationMinutes}m",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
