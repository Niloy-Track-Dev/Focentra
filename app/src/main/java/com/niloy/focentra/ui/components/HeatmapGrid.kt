package com.niloy.focentra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.focentra.engine.HeatmapCell
import com.niloy.focentra.ui.theme.*

@Composable
fun HeatmapGrid(
    cells: List<HeatmapCell>,
    modifier: Modifier = Modifier,
    onCellClick: ((HeatmapCell) -> Unit)? = null
) {
    if (cells.isEmpty()) return

    var selectedCell by remember { mutableStateOf<HeatmapCell?>(null) }
    val scrollState = rememberScrollState()

    // Group cells into weeks (each column has 7 days)
    val weeks = remember(cells) {
        cells.chunked(7)
    }

    LaunchedEffect(weeks.size) {
        // Auto scroll to latest week
        if (weeks.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Intensity Heatmap",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Intensity Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(HeatmapLevel0, HeatmapLevel1, HeatmapLevel2, HeatmapLevel3, HeatmapLevel4).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day of week labels on left
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEach { d ->
                        Box(
                            modifier = Modifier.size(13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Columns of weeks
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { cell ->
                                val cellColor = when (cell.level) {
                                    1 -> HeatmapLevel1
                                    2 -> HeatmapLevel2
                                    3 -> HeatmapLevel3
                                    4 -> HeatmapLevel4
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                }

                                val isSelected = selectedCell?.dateKey == cell.dateKey

                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cellColor)
                                        .then(
                                            if (isSelected) Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(3.dp)
                                            ) else Modifier
                                        )
                                        .clickable {
                                            selectedCell = cell
                                            onCellClick?.invoke(cell)
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Cell Detail Banner on Click
            if (selectedCell != null) {
                val c = selectedCell!!
                val hrs = c.studyMinutes / 60
                val mins = c.studyMinutes % 60
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = c.dateKey,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val studyText = if (c.studyMinutes > 0) {
                                "${if (hrs > 0) "${hrs}h " else ""}${mins}m in ${c.sessionCount} sessions"
                            } else "No study logged"
                            Text(
                                text = studyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (c.topSubject.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = c.topSubject,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
