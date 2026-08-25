package com.niloy.focentra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.focentra.data.local.entity.StudySessionEntity
import com.niloy.focentra.ui.components.EmptyStateView
import com.niloy.focentra.ui.components.RecentSessionItem
import com.niloy.focentra.ui.components.SectionHeader
import com.niloy.focentra.ui.theme.HeatmapLevel1
import com.niloy.focentra.ui.theme.HeatmapLevel2
import com.niloy.focentra.ui.theme.HeatmapLevel3
import com.niloy.focentra.ui.theme.HeatmapLevel4
import com.niloy.focentra.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedEpochMs by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()

    var calendarMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Map sessions by dateKey
    val sessionsByDate = remember(sessions) {
        val map = mutableMapOf<String, MutableList<StudySessionEntity>>()
        val cal = Calendar.getInstance()
        sessions.forEach { s ->
            cal.timeInMillis = s.startTime
            val k = dayKeyFormat.format(cal.time)
            map.getOrPut(k) { mutableListOf() }.add(s)
        }
        map
    }

    // Days in current month grid
    val daysGrid = remember(calendarMonth) {
        val cal = calendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon ...
        val leadingBlanks = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<CalendarDayItem?>()
        for (i in 0 until leadingBlanks) {
            list.add(null)
        }
        for (d in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            list.add(
                CalendarDayItem(
                    dayOfMonth = d,
                    epochMs = cal.timeInMillis,
                    dateKey = dayKeyFormat.format(cal.time)
                )
            )
        }
        while (list.size % 7 != 0) {
            list.add(null)
        }
        list
    }

    val selectedDateKey = remember(selectedEpochMs) {
        dayKeyFormat.format(Date(selectedEpochMs))
    }

    val selectedSessions = remember(selectedDateKey, sessionsByDate) {
        sessionsByDate[selectedDateKey] ?: emptyList()
    }

    val totalSelectedMins = selectedSessions.sumOf { it.actualFocusedSeconds } / 60
    val totalSelectedHrs = totalSelectedMins / 60
    val totalSelectedRemMins = totalSelectedMins % 60

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
    ) {
        // Month Navigation Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val prev = calendarMonth.clone() as Calendar
                                prev.add(Calendar.MONTH, -1)
                                calendarMonth = prev
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Text(
                            text = monthFormat.format(calendarMonth.time),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                val next = calendarMonth.clone() as Calendar
                                next.add(Calendar.MONTH, 1)
                                calendarMonth = next
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Day of Week Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dow ->
                            Text(
                                text = dow,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid Layout
                    val rows = daysGrid.chunked(7)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (item in row) {
                                if (item == null) {
                                    Spacer(modifier = Modifier.size(38.dp))
                                } else {
                                    val isSelected = item.dateKey == selectedDateKey
                                    val daySessions = sessionsByDate[item.dateKey] ?: emptyList()
                                    val studyMins = daySessions.sumOf { it.actualFocusedSeconds } / 60

                                    val dotColor = when {
                                        studyMins == 0L -> Color.Transparent
                                        studyMins <= 60L -> HeatmapLevel1
                                        studyMins <= 180L -> HeatmapLevel2
                                        studyMins <= 360L -> HeatmapLevel3
                                        else -> HeatmapLevel4
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                ) else Modifier
                                            )
                                            .clickable {
                                                viewModel.setCalendarDate(item.epochMs)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${item.dayOfMonth}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (dotColor != Color.Transparent) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
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
        }

        // Selected Day Details Header
        item {
            val selectedDateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedEpochMs))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedDateStr,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedSessions.size} sessions recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${String.format("%02d", totalSelectedHrs)}h ${String.format("%02d", totalSelectedRemMins)}m",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Sessions on Selected Date
        item {
            SectionHeader(title = "Day Sessions (${selectedSessions.size})")

            if (selectedSessions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.EventBusy,
                    title = "No sessions on this date",
                    description = "Select another date or start a timer."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedSessions.forEach { s ->
                        RecentSessionItem(session = s)
                    }
                }
            }
        }
    }
}

data class CalendarDayItem(
    val dayOfMonth: Int,
    val epochMs: Long,
    val dateKey: String
)
