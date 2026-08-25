package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.getSubjectIcon
import com.example.ui.components.parseColorHex
import com.example.viewmodel.MainViewModel

@Composable
fun SubjectsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var subjectToAddTopic by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Subject") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 84.dp)
                )
            }
        }
    ) { padding ->
        if (subjects.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.School,
                title = "No subjects added",
                description = "Create your study subjects and topics to organize your focus.",
                actionButtonText = "Create Subject",
                onActionClick = { showCreateDialog = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
            ) {
                items(subjects, key = { it.id }) { subject ->
                    val subTopics = topics.filter { it.subjectName.equals(subject.name, ignoreCase = true) }
                    val studiedMins = studyMinsBySubject[subject.name] ?: 0L
                    val studiedHrs = studiedMins / 60f
                    val targetHrs = subject.targetHours.coerceAtLeast(1f)
                    val progress = (studiedHrs / targetHrs).coerceIn(0f, 1f)

                    SubjectDetailCard(
                        subject = subject,
                        topics = subTopics,
                        studiedMins = studiedMins,
                        progress = progress,
                        onAddTopicClick = { subjectToAddTopic = subject },
                        onDeleteTopic = { topic -> viewModel.deleteTopic(topic) },
                        onDeleteSubjectClick = { subjectToDelete = subject }
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectDetailCard(
    subject: SubjectEntity,
    topics: List<TopicEntity>,
    studiedMins: Long,
    progress: Float,
    onAddTopicClick: () -> Unit,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Column {
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

                IconButton(onClick = onDeleteSubjectClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
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

            // Topics list & add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Topics (${topics.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onAddTopicClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Topic")
                }
            }

            if (topics.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(topics) { topic ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topic.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete topic",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onDeleteTopic(topic) }
                                )
                            }
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
