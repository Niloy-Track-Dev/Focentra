package com.niloy.focentra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niloy.focentra.data.repository.StudyRepository
import com.niloy.focentra.ui.state.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: StudyRepository,
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _effect = MutableSharedFlow<DashboardUiEffect>()
    val effect: SharedFlow<DashboardUiEffect> = _effect.asSharedFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            mainViewModel.todayFocusedMinutes,
            mainViewModel.streakInfo,
            repository.allTasks,
            repository.allSubjects,
            repository.allChapters
        ) { mins, streak, tasks, subs, chaps ->
            Triple(mins, streak, Triple(tasks, subs, chaps))
        },
        combine(
            mainViewModel.brainDumpNotes,
            mainViewModel.activeNotifications,
            mainViewModel.periodStatistics
        ) { brainDumps, notifs, stats ->
            Triple(brainDumps, notifs, stats)
        }
    ) { part1, part2 ->
        val mins = part1.first
        val streak = part1.second
        val tasks = part1.third.first
        val subs = part1.third.second
        val chaps = part1.third.third
        val brainDumps = part2.first
        val notifs = part2.second
        val stats = part2.third

        DashboardUiState(
            todayFocusedMinutes = mins,
            streakInfo = streak,
            dailyTasks = tasks,
            subjects = subs,
            chapters = chaps,
            brainDumpNotes = brainDumps,
            activeNotifications = notifs,
            periodStats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.AddTask -> mainViewModel.addTask(event.title, event.subject)
            is DashboardUiEvent.ToggleTask -> mainViewModel.toggleTask(event.task)
            is DashboardUiEvent.DeleteTask -> mainViewModel.deleteTask(event.task)
            DashboardUiEvent.ClearCompletedTasks -> mainViewModel.clearCompletedTasks()
            is DashboardUiEvent.AddBrainDumpNote -> mainViewModel.addBrainDumpNote(event.text, event.subject)
            is DashboardUiEvent.ToggleBrainDumpDone -> mainViewModel.toggleBrainDumpDone(event.id)
            is DashboardUiEvent.DeleteBrainDumpNote -> mainViewModel.deleteBrainDumpNote(event.id)
            DashboardUiEvent.UseStreakSaverPass -> mainViewModel.useStreakSaverPass()
        }
    }
}

class SubjectsViewModel(
    private val repository: StudyRepository,
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _effect = MutableSharedFlow<SubjectsUiEffect>()
    val effect: SharedFlow<SubjectsUiEffect> = _effect.asSharedFlow()

    val uiState: StateFlow<SubjectsUiState> = combine(
        repository.allSubjects,
        repository.allChapters,
        repository.allSessions
    ) { subs, chaps, sess ->
        SubjectsUiState(
            subjects = subs,
            chapters = chaps,
            sessions = sess
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectsUiState()
    )

    fun onEvent(event: SubjectsUiEvent) {
        when (event) {
            is SubjectsUiEvent.CreateSubject -> mainViewModel.createSubject(
                event.name, event.colorHex, event.iconName, event.targetHours, event.description
            )
            is SubjectsUiEvent.UpdateSubject -> mainViewModel.updateSubject(event.subject)
            is SubjectsUiEvent.DeleteSubject -> mainViewModel.deleteSubject(event.subject)
            is SubjectsUiEvent.AddChapter -> mainViewModel.addChapter(event.subjectId, event.subjectName, event.chapterName)
            is SubjectsUiEvent.UpdateChapter -> mainViewModel.updateChapter(event.chapter)
            is SubjectsUiEvent.DeleteChapter -> mainViewModel.deleteChapter(event.chapter)
            is SubjectsUiEvent.HandleChapterTap -> mainViewModel.handleChapterTap(event.chapter)
            is SubjectsUiEvent.UpdateChapterStatus -> mainViewModel.updateChapterStatus(event.chapter, event.newStatus)
        }
    }
}

class AnalyticsViewModel(
    private val repository: StudyRepository,
    private val mainViewModel: MainViewModel
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        combine(
            mainViewModel.periodStatistics,
            mainViewModel.selectedPeriod,
            mainViewModel.personalRecords
        ) { stats, period, records ->
            Triple(stats, period, records)
        },
        combine(
            mainViewModel.heatmapData,
            repository.allSessions,
            repository.allSubjects
        ) { heatmap, sessions, subjects ->
            Triple(heatmap, sessions, subjects)
        }
    ) { part1, part2 ->
        AnalyticsUiState(
            periodStats = part1.first,
            selectedPeriod = part1.second,
            personalRecords = part1.third,
            heatmapData = part2.first,
            sessions = part2.second,
            subjects = part2.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun onEvent(event: AnalyticsUiEvent) {
        when (event) {
            is AnalyticsUiEvent.SelectPeriod -> mainViewModel.setPeriod(event.period)
            is AnalyticsUiEvent.UpdateGoalTarget -> mainViewModel.updateGoalTarget(event.periodType, event.newTargetMinutes)
        }
    }
}

class RevisionHubViewModel(
    private val repository: StudyRepository,
    private val mainViewModel: MainViewModel
) : ViewModel() {

    val uiState: StateFlow<RevisionHubUiState> = combine(
        repository.allSubjects,
        repository.allChapters,
        mainViewModel.revisionHistory,
        mainViewModel.activeNotifications
    ) { subs, chaps, revHist, notifs ->
        RevisionHubUiState(
            subjects = subs,
            chapters = chaps,
            revisionHistory = revHist,
            activeNotifications = notifs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RevisionHubUiState()
    )

    fun onEvent(event: RevisionHubUiEvent) {
        when (event) {
            is RevisionHubUiEvent.RecordRevision -> mainViewModel.recordRevision(
                event.subjectName, event.chapterName, event.durationMinutes, event.startTime, event.endTime, event.stage
            )
            is RevisionHubUiEvent.DeleteRevisionRecord -> mainViewModel.deleteRevisionRecord(event.id)
        }
    }
}
