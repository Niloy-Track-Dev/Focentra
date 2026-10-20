package com.niloy.focentra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.focentra.data.local.entity.ChapterEntity
import com.niloy.focentra.engine.SessionMode
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Quick Timer, 1: Upcoming Scheduled, 2: History

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Tab Selector
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text("Quick Revision", maxLines = 1)
            }

            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text("Scheduled", maxLines = 1)
            }

            SegmentedButton(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text("History", maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> QuickRevisionTimerSection(viewModel = viewModel)
            1 -> UpcomingScheduledRevisionsSection(viewModel = viewModel)
            2 -> RevisionHistorySection(viewModel = viewModel)
        }
    }
}

@Composable
private fun QuickRevisionTimerSection(viewModel: MainViewModel) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "General") }
    var selectedChapter by remember { mutableStateOf("") }
    var durationMins by remember { mutableIntStateOf(30) }

    val matchingChapters = remember(chapters, selectedSubject) {
        chapters.filter { it.subjectName.equals(selectedSubject, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                            selectedSubject = sub.name
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
                        val isSelected = ch.chapterName == selectedChapter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedChapter = ch.chapterName },
                            label = { Text(ch.chapterName) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Duration Selection
        Column {
            Text(
                text = "Revision Duration",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            val options = listOf(15, 30, 45, 60, 90)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { dur ->
                    val isSelected = durationMins == dur
                    FilterChip(
                        selected = isSelected,
                        onClick = { durationMins = dur },
                        label = { Text("${dur}m") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.timerEngine.configureSession(
                    mode = SessionMode.COUNTDOWN,
                    subject = selectedSubject,
                    selectedChapter = selectedChapter,
                    topic = "Spaced Revision",
                    durationMinutes = durationMins
                )
                viewModel.timerEngine.startSession()
                viewModel.navigateTo(com.niloy.focentra.viewmodel.NavigationTab.TIMER)
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
                text = "Start $durationMins Mins Revision Timer",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun UpcomingScheduledRevisionsSection(viewModel: MainViewModel) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    val scheduledList = remember(chapters) {
        val list = mutableListOf<ScheduledRevisionItem>()
        val now = System.currentTimeMillis()
        val ONE_DAY = 86400000L

        chapters.filter { it.tapCount >= 3 || it.status == "অধ্যায় Completely শেষ" }.forEach { ch ->
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

    if (scheduledList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "কোনো শিডিউল্ড রিভিশন নেই",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "অধ্যায় শেষ করার পর ১, ৩, ৭ এবং ১৫তম দিনের সায়েন্টিফিক রিভিশন শিডিউল এখানে দেখা যাবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                        viewModel.timerEngine.configureSession(
                            mode = SessionMode.COUNTDOWN,
                            subject = item.chapter.subjectName,
                            selectedChapter = item.chapter.chapterName,
                            topic = item.stageName,
                            durationMinutes = 30
                        )
                        viewModel.timerEngine.startSession()
                        viewModel.navigateTo(com.niloy.focentra.viewmodel.NavigationTab.TIMER)
                    }
                )
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
                Text("Start Revision Timer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun RevisionHistorySection(viewModel: MainViewModel) {
    val history by viewModel.revisionHistory.collectAsStateWithLifecycle()

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "কোনো রিভিশন হিস্ট্রি নেই",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "আপনার সম্পন্ন করা সকল রিভিশনের তথ্য ও হিস্ট্রি লগ এখানে সংরক্ষিত হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(history, key = { it.id }) { rec ->
                RevisionHistoryCard(record = rec)
            }
        }
    }
}

@Composable
private fun RevisionHistoryCard(record: RevisionRecord) {
    val dateStr = remember(record.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
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
                    text = "${record.subjectName} • ${record.chapterName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr • ${record.revisionStage}",
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
