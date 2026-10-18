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
            text = { Text("Are you sure you want to delete chapter «${chapterToDelete!!.chapterName}»? All sequential class progress for this chapter will be removed.") },
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
            onAdd = { chapterName, totalClasses ->
                viewModel.addChapter(subjectToAddChapter!!.id, subjectToAddChapter!!.name, chapterName, totalClasses)
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

    val totalClasses = chapters.sumOf { it.totalClasses }
    val completedClasses = chapters.sumOf { it.completedClassesCount }
    val completedChapters = chapters.count { it.completedClassesCount >= it.totalClasses && it.totalClasses > 0 }
    val totalRevisions = chapters.size * 2
    val completedRevisions = chapters.count { it.isRevision1Completed } + chapters.count { it.isRevision2Completed }

    val readinessScore = if (totalClasses > 0) {
        val classProgress = (completedClasses.toFloat() / totalClasses) * 70f
        val revProgress = if (totalRevisions > 0) (completedRevisions.toFloat() / totalRevisions) * 30f else 0f
        (classProgress + revProgress).coerceIn(0f, 100f)
    } else 0f

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
                            text = "Syllabus Readiness System",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${chapters.size} Syllabus Chapters Total",
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
                        text = "${readinessScore.toInt()}% Exam Ready",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            LinearProgressIndicator(
                progress = { readinessScore / 100f },
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
                    text = "Classes: $completedClasses/$totalClasses",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Chapters: $completedChapters/${chapters.size}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Revisions: $completedRevisions/$totalRevisions",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
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

@Composable
fun ChapterSequentialTrackerView(
    chapter: ChapterEntity,
    onUpdate: (ChapterEntity) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.chapterName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Classes: ${chapter.completedClassesCount}/${chapter.totalClasses} • Rev 1: ${if (chapter.isRevision1Completed) "✓" else "✗"} • Rev 2: ${if (chapter.isRevision2Completed) "✓" else "✗"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Sequential Classes Checkbox Grid
                Text(
                    text = "Sequential Classes Progress (Auto-Unlocks Next):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..chapter.totalClasses) {
                        val isChecked = i <= chapter.completedClassesCount
                        val isEnabled = i <= chapter.completedClassesCount + 1

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isChecked,
                                enabled = isEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onUpdate(chapter.copy(completedClassesCount = i))
                                    } else {
                                        onUpdate(chapter.copy(completedClassesCount = i - 1))
                                    }
                                }
                            )
                            Text(
                                text = String.format("Class %02d", i),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // Revision Classes shown after regular classes completed
                    val allRegularDone = chapter.completedClassesCount >= chapter.totalClasses
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = chapter.isRevision1Completed,
                            enabled = allRegularDone,
                            onCheckedChange = { checked ->
                                onUpdate(chapter.copy(isRevision1Completed = checked))
                            }
                        )
                        Text(
                            text = "Revision Class 01",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (allRegularDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = chapter.isRevision2Completed,
                            enabled = allRegularDone && chapter.isRevision1Completed,
                            onCheckedChange = { checked ->
                                onUpdate(chapter.copy(isRevision2Completed = checked))
                            }
                        )
                        Text(
                            text = "Revision Class 02",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (allRegularDone && chapter.isRevision1Completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
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
    var totalClassesText by remember { mutableStateOf(chapter.totalClasses.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Chapter Syllabus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = chapterName,
                    onValueChange = { chapterName = it },
                    label = { Text("Chapter Name / Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = totalClassesText,
                    onValueChange = { totalClassesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Class Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chapterName.isNotBlank()) {
                        val count = totalClassesText.toIntOrNull()?.coerceAtLeast(1) ?: chapter.totalClasses
                        val completedCount = chapter.completedClassesCount.coerceAtMost(count)
                        onSave(chapter.copy(chapterName = chapterName.trim(), totalClasses = count, completedClassesCount = completedCount))
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
    onAdd: (chapterName: String, totalClasses: Int) -> Unit
) {
    var chapterName by remember { mutableStateOf("") }
    var totalClassesText by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chapter to «${subject.name}»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = chapterName,
                    onValueChange = { chapterName = it },
                    label = { Text("Chapter Name / Title") },
                    placeholder = { Text("e.g. Chapter 01: Kinematics") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = totalClassesText,
                    onValueChange = { totalClassesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Class Number (e.g. 20)") },
                    placeholder = { Text("20") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chapterName.isNotBlank()) {
                        val count = totalClassesText.toIntOrNull() ?: 10
                        onAdd(chapterName, count)
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
