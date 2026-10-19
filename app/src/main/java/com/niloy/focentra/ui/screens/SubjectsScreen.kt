package com.niloy.focentra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.focentra.data.local.entity.ChapterEntity
import com.niloy.focentra.data.local.entity.SubjectEntity
import com.niloy.focentra.data.local.entity.TopicEntity
import com.niloy.focentra.ui.components.EmptyStateView
import com.niloy.focentra.ui.components.getSubjectIcon
import com.niloy.focentra.ui.components.parseColorHex
import com.niloy.focentra.viewmodel.MainViewModel

@Composable
fun SubjectsScreen(
    viewModel: MainViewModel,
    isFabVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToAddTopic by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToAddChapter by remember { mutableStateOf<SubjectEntity?>(null) }
    var chapterToEdit by remember { mutableStateOf<ChapterEntity?>(null) }
    var chapterToDelete by remember { mutableStateOf<ChapterEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicEntity?>(null) }

    // Map total study time per subject
    val studyMinsBySubject = remember(sessions) {
        sessions.groupBy { it.subject }
            .mapValues { entry -> entry.value.sumOf { it.actualFocusedSeconds } / 60 }
    }

    if (showCreateDialog) {
        CreateSubjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, colorHex, icon, targetHrs, desc ->
                viewModel.createSubject(name, colorHex, icon, targetHrs, desc)
                showCreateDialog = false
            }
        )
    }

    if (chapterToEdit != null) {
        EditChapterDialog(
            chapter = chapterToEdit!!,
            onDismiss = { chapterToEdit = null },
            onSave = { updated ->
                viewModel.updateChapter(updated)
                chapterToEdit = null
            }
        )
    }

    if (chapterToDelete != null) {
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("Delete Syllabus Chapter?") },
            text = { Text("Are you sure you want to delete chapter «${chapterToDelete!!.chapterName}»?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChapter(chapterToDelete!!)
                        chapterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (subjectToAddChapter != null) {
        AddChapterDialog(
            subject = subjectToAddChapter!!,
            onDismiss = { subjectToAddChapter = null },
            onAdd = { chapterName ->
                viewModel.addChapter(subjectToAddChapter!!.id, subjectToAddChapter!!.name, chapterName)
                subjectToAddChapter = null
            }
        )
    }

    if (subjectToEdit != null) {
        EditSubjectDialog(
            subject = subjectToEdit!!,
            onDismiss = { subjectToEdit = null },
            onUpdate = { updatedSubject ->
                viewModel.updateSubject(updatedSubject)
                subjectToEdit = null
            }
        )
    }

    if (topicToDelete != null) {
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            title = { Text("Delete Topic?") },
            text = { Text("Are you sure you want to delete topic «${topicToDelete!!.name}»?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTopic(topicToDelete!!)
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (subjectToAddTopic != null) {
        AddTopicDialog(
            subject = subjectToAddTopic!!,
            onDismiss = { subjectToAddTopic = null },
            onAdd = { topicName ->
                viewModel.createTopic(subjectToAddTopic!!.id, subjectToAddTopic!!.name, topicName)
                subjectToAddTopic = null
            }
        )
    }

    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject?") },
            text = { Text("Are you sure you want to delete «${subjectToDelete!!.name}»? Existing study sessions for this subject will remain in history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subjectToDelete!!)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (subjects.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.School,
                title = "No subjects added",
                description = "Create your study subjects and topics to organize your focus.",
                actionButtonText = "Create Subject",
                onActionClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp)
            ) {
                if (chapters.isNotEmpty()) {
                    item {
                        SyllabusOverviewHeaderCard(chapters = chapters)
                    }
                }

                items(subjects, key = { it.id }) { subject ->
                    val subTopics = topics.filter { it.subjectName.equals(subject.name, ignoreCase = true) }
                    val studiedMins = studyMinsBySubject[subject.name] ?: 0L
                    val studiedHrs = studiedMins / 60f
                    val targetHrs = subject.targetHours.coerceAtLeast(1f)
                    val progress = (studiedHrs / targetHrs).coerceIn(0f, 1f)

                    val subChapters = chapters.filter { it.subjectId == subject.id || it.subjectName.equals(subject.name, ignoreCase = true) }

                    SubjectDetailCard(
                        subject = subject,
                        topics = subTopics,
                        chapters = subChapters,
                        studiedMins = studiedMins,
                        progress = progress,
                        onEditSubjectClick = { subjectToEdit = subject },
                        onAddTopicClick = { subjectToAddTopic = subject },
                        onAddChapterClick = { subjectToAddChapter = subject },
                        onUpdateChapter = { updatedChapter -> viewModel.updateChapter(updatedChapter) },
                        onEditChapter = { chapter -> chapterToEdit = chapter },
                        onDeleteChapter = { chapter -> chapterToDelete = chapter },
                        onDeleteTopic = { topic -> topicToDelete = topic },
                        onDeleteSubjectClick = { subjectToDelete = subject }
                    )
                }
            }
        }

        if (subjects.isNotEmpty()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isFabVisible,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 12.dp)
            ) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Subject")
                }
            }
        }
    }
}

@Composable
fun SyllabusOverviewHeaderCard(
    chapters: List<ChapterEntity>
) {
    if (chapters.isEmpty()) return

    val totalChapters = chapters.size
    val finishedChapters = chapters.count { it.tapCount >= 3 || it.status == "অধ্যায় Completely শেষ" }
    val inProgressChapters = chapters.count { it.status != "পড়া শুরু হয় নাই" && it.status != "অধ্যায় Completely শেষ" && it.tapCount < 3 }
    val progressPct = if (totalChapters > 0) (finishedChapters.toFloat() / totalChapters * 100f).coerceIn(0f, 100f) else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সিলেবাস ট্র্যাকার (Syllabus Tracker)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "মোট $totalChapters টি অধ্যায় • $finishedChapters টি সম্পূর্ণ শেষ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${progressPct.toInt()}% সম্পূর্ণ",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressPct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "চলছে: $inProgressChapters টি অধ্যায়",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "সম্পূর্ণ শেষ: $finishedChapters / $totalChapters",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SubjectDetailCard(
    subject: SubjectEntity,
    topics: List<TopicEntity>,
    chapters: List<ChapterEntity>,
    studiedMins: Long,
    progress: Float,
    onEditSubjectClick: () -> Unit,
    onAddTopicClick: () -> Unit,
    onAddChapterClick: () -> Unit,
    onUpdateChapter: (ChapterEntity) -> Unit,
    onEditChapter: (ChapterEntity) -> Unit,
    onDeleteChapter: (ChapterEntity) -> Unit,
    onDeleteTopic: (TopicEntity) -> Unit,
    onDeleteSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = parseColorHex(subject.colorHex)
    val studiedHrs = studiedMins / 60
    val studiedRemMins = studiedMins % 60

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(subjectColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getSubjectIcon(subject.iconName),
                            contentDescription = null,
                            tint = subjectColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subject.description.isNotBlank()) {
                            Text(
                                text = subject.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditSubjectClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Subject",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteSubjectClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar vs Target
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = subjectColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${studiedHrs}h ${studiedRemMins}m studied",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = subjectColor
                )
                Text(
                    text = "Goal: ${subject.targetHours.toInt()}h (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown / Expandable Chapters Section
            var isChapterDropdownExpanded by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isChapterDropdownExpanded = !isChapterDropdownExpanded },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Syllabus Chapters (${chapters.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onAddChapterClick,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Chapter", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Icon(
                                imageVector = if (isChapterDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand Chapters",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isChapterDropdownExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (chapters.isEmpty()) {
                            Text(
                                text = "No chapters added yet. Tap 'Add Chapter' to structure syllabus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                chapters.forEach { chapter ->
                                    ChapterSequentialTrackerView(
                                        chapter = chapter,
                                        onUpdate = onUpdateChapter,
                                        onEdit = { onEditChapter(chapter) },
                                        onDelete = { onDeleteChapter(chapter) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val CHAPTER_STATUS_OPTIONS = listOf(
    "পড়া শুরু হয় নাই",
    "পড়া শুরু হয়েছে",
    "পড়া চলছে",
    "পড়া শেষ পর্যায়ে",
    "পড়া শেষ",
    "রিভিশন চলছে",
    "অধ্যায় Completely শেষ"
)

@Composable
fun ChapterSequentialTrackerView(
    chapter: ChapterEntity,
    onUpdate: (ChapterEntity) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val currentStatus = chapter.status.ifBlank { "পড়া শুরু হয় নাই" }
    val isFinished = chapter.tapCount >= 3 || currentStatus == "অধ্যায় Completely শেষ"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isFinished) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isFinished) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Chapter Title & 3-Tap Progress Indicator
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 3-Tap Button / Pill Indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isFinished -> MaterialTheme.colorScheme.primary
                            chapter.tapCount == 2 -> MaterialTheme.colorScheme.secondaryContainer
                            chapter.tapCount == 1 -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val nextTap = if (chapter.tapCount >= 3) 0 else chapter.tapCount + 1
                                val nextStatus = when (nextTap) {
                                    3 -> "অধ্যায় Completely শেষ"
                                    2 -> "পড়া শেষ"
                                    1 -> "পড়া শুরু হয়েছে"
                                    else -> "পড়া শুরু হয় নাই"
                                }
                                onUpdate(chapter.copy(tapCount = nextTap, status = nextStatus))
                            }
                    ) {
                        Text(
                            text = if (isFinished) "✓ 3/3" else "${chapter.tapCount}/3 Tap",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isFinished) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = chapter.chapterName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isFinished) FontWeight.Bold else FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Action Buttons (Edit & Delete)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Chapter",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Chapter",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Status Selector Dropdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: $currentStatus",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Change Status",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        CHAPTER_STATUS_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (option == currentStatus) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    val newTapCount = if (option == "অধ্যায় Completely শেষ") 3 else chapter.tapCount
                                    onUpdate(chapter.copy(status = option, tapCount = newTapCount))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateSubjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String, icon: String, targetHrs: Float, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var targetHours by remember { mutableStateOf("100") }
    var selectedColor by remember { mutableStateOf("#6366F1") }
    var selectedIcon by remember { mutableStateOf("School") }

    val colors = listOf("#6366F1", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#06B6D4", "#EF4444", "#3B82F6", "#14B8A6")
    val icons = listOf("School", "Code", "Science", "Calculate", "Biotech", "MenuBook", "HistoryEdu", "Psychology", "Language")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g. Organic Chemistry") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = targetHours,
                    onValueChange = { targetHours = it },
                    label = { Text("Target Study Hours") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Color Theme", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { c ->
                        val isSelected = selectedColor == c
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(c))
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                                )
                                .clickable { selectedColor = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, selectedColor, selectedIcon, targetHours.toFloatOrNull() ?: 100f, desc)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditSubjectDialog(
    subject: SubjectEntity,
    onDismiss: () -> Unit,
    onUpdate: (SubjectEntity) -> Unit
) {
    var name by remember { mutableStateOf(subject.name) }
    var desc by remember { mutableStateOf(subject.description) }
    var targetHours by remember { mutableStateOf(subject.targetHours.toInt().toString()) }
    var selectedColor by remember { mutableStateOf(subject.colorHex) }
    var selectedIcon by remember { mutableStateOf(subject.iconName) }

    val colors = listOf("#6366F1", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#06B6D4", "#EF4444", "#3B82F6", "#14B8A6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = targetHours,
                    onValueChange = { targetHours = it },
                    label = { Text("Target Study Hours") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Color Theme", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { c ->
                        val isSelected = selectedColor == c
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(c))
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                                )
                                .clickable { selectedColor = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onUpdate(
                            subject.copy(
                                name = name,
                                description = desc,
                                targetHours = targetHours.toFloatOrNull() ?: subject.targetHours,
                                colorHex = selectedColor,
                                iconName = selectedIcon
                            )
                        )
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditChapterDialog(
    chapter: ChapterEntity,
    onDismiss: () -> Unit,
    onSave: (ChapterEntity) -> Unit
) {
    var chapterName by remember { mutableStateOf(chapter.chapterName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Chapter Title") },
        text = {
            OutlinedTextField(
                value = chapterName,
                onValueChange = { chapterName = it },
                label = { Text("Chapter Name / Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chapterName.isNotBlank()) {
                        onSave(chapter.copy(chapterName = chapterName.trim()))
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddChapterDialog(
    subject: SubjectEntity,
    onDismiss: () -> Unit,
    onAdd: (chapterName: String) -> Unit
) {
    var chapterName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chapter to «${subject.name}»") },
        text = {
            OutlinedTextField(
                value = chapterName,
                onValueChange = { chapterName = it },
                label = { Text("Chapter Name / Title") },
                placeholder = { Text("e.g. ১. ভৌত জগৎ ও পরিমাপ") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chapterName.isNotBlank()) {
                        onAdd(chapterName.trim())
                    }
                }
            ) {
                Text("Add Chapter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTopicDialog(
    subject: SubjectEntity,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var topicName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Topic to «${subject.name}»") },
        text = {
            OutlinedTextField(
                value = topicName,
                onValueChange = { topicName = it },
                label = { Text("Topic Name") },
                placeholder = { Text("e.g. Chapter 4: Integration") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topicName.isNotBlank()) onAdd(topicName)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
