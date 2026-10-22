package com.niloy.focentra.ui.state

import com.niloy.focentra.data.local.entity.*
import com.niloy.focentra.data.repository.StreakInfo
import com.niloy.focentra.engine.HeatmapCell
import com.niloy.focentra.engine.PeriodStatistics
import com.niloy.focentra.engine.PersonalRecords
import com.niloy.focentra.engine.TimePeriod
import com.niloy.focentra.viewmodel.*

// --- Dashboard State, Events, Effects ---
data class DashboardUiState(
    val todayFocusedMinutes: Long = 0L,
    val streakInfo: StreakInfo = StreakInfo(0, 0, false),
    val dailyTasks: List<StudyTaskEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val brainDumpNotes: List<BrainDumpNote> = emptyList(),
    val activeNotifications: List<AppNotification> = emptyList(),
    val periodStats: PeriodStatistics = PeriodStatistics(TimePeriod.THIS_WEEK),
    val isLoading: Boolean = false
)

sealed interface DashboardUiEvent {
    data class AddTask(val title: String, val subject: String = "General") : DashboardUiEvent
    data class ToggleTask(val task: StudyTaskEntity) : DashboardUiEvent
    data class DeleteTask(val task: StudyTaskEntity) : DashboardUiEvent
    object ClearCompletedTasks : DashboardUiEvent
    data class AddBrainDumpNote(val text: String, val subject: String = "General") : DashboardUiEvent
    data class ToggleBrainDumpDone(val id: String) : DashboardUiEvent
    data class DeleteBrainDumpNote(val id: String) : DashboardUiEvent
    object UseStreakSaverPass : DashboardUiEvent
}

sealed interface DashboardUiEffect {
    data class ShowSnackbar(val message: String) : DashboardUiEffect
    data class NavigateTo(val tab: NavigationTab) : DashboardUiEffect
}

// --- Subjects State, Events, Effects ---
data class SubjectsUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface SubjectsUiEvent {
    data class CreateSubject(
        val name: String,
        val colorHex: String,
        val iconName: String,
        val targetHours: Float,
        val description: String
    ) : SubjectsUiEvent
    data class UpdateSubject(val subject: SubjectEntity) : SubjectsUiEvent
    data class DeleteSubject(val subject: SubjectEntity) : SubjectsUiEvent
    data class AddChapter(val subjectId: Long, val subjectName: String, val chapterName: String) : SubjectsUiEvent
    data class UpdateChapter(val chapter: ChapterEntity) : SubjectsUiEvent
    data class DeleteChapter(val chapter: ChapterEntity) : SubjectsUiEvent
    data class HandleChapterTap(val chapter: ChapterEntity) : SubjectsUiEvent
    data class UpdateChapterStatus(val chapter: ChapterEntity, val newStatus: String) : SubjectsUiEvent
}

sealed interface SubjectsUiEffect {
    data class ShowSnackbar(val message: String) : SubjectsUiEffect
}

// --- Analytics State, Events, Effects ---
data class AnalyticsUiState(
    val periodStats: PeriodStatistics = PeriodStatistics(TimePeriod.THIS_WEEK),
    val selectedPeriod: TimePeriod = TimePeriod.THIS_WEEK,
    val personalRecords: PersonalRecords = PersonalRecords(),
    val heatmapData: List<HeatmapCell> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList()
)

sealed interface AnalyticsUiEvent {
    data class SelectPeriod(val period: TimePeriod) : AnalyticsUiEvent
    data class UpdateGoalTarget(val periodType: String, val newTargetMinutes: Int) : AnalyticsUiEvent
}

// --- Revision Hub State, Events, Effects ---
data class RevisionHubUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val revisionHistory: List<RevisionRecord> = emptyList(),
    val activeNotifications: List<AppNotification> = emptyList()
)

sealed interface RevisionHubUiEvent {
    data class RecordRevision(
        val subjectName: String,
        val chapterName: String,
        val durationMinutes: Int,
        val startTime: Long,
        val endTime: Long,
        val stage: String
    ) : RevisionHubUiEvent
    data class DeleteRevisionRecord(val id: String) : RevisionHubUiEvent
}
