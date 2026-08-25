package com.example.engine

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.entity.StudySessionEntity
import com.example.data.repository.StudyRepository
import com.example.service.StudyTimerService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionMode {
    COUNTDOWN,
    STOPWATCH,
    POMODORO
}

enum class PomodoroPhase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class DistractionLog(
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TimerUiState(
    val status: TimerStatus = TimerStatus.IDLE,
    val mode: SessionMode = SessionMode.COUNTDOWN,
    val subject: String = "Mathematics",
    val topic: String = "",
    val goal: String = "",
    val tags: List<String> = emptyList(),
    val targetDurationSeconds: Long = 25 * 60L,
    val elapsedSeconds: Long = 0L, // Total time elapsed since start
    val focusedSeconds: Long = 0L, // Focused study time excluding pauses
    val pausedSeconds: Long = 0L,  // Accumulated pause time
    val remainingSeconds: Long = 25 * 60L, // For countdown / pomodoro
    val progress: Float = 0f, // 0.0 to 1.0
    // Pomodoro specifics
    val pomodoroPhase: PomodoroPhase = PomodoroPhase.FOCUS,
    val currentRound: Int = 1,
    val totalRounds: Int = 4,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
    // Distraction tracking
    val distractionCount: Int = 0,
    val distractions: List<DistractionLog> = emptyList(),
    // Metadata
    val sessionStartTime: Long = 0L,
    val keepScreenAwake: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showCompletionSheet: Boolean = false,
    val completedSessionData: StudySessionEntity? = null
)

class TimerEngine private constructor(
    private val context: Context,
    private val repository: StudyRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    // Time tracking variables
    private var sessionStartMs: Long = 0L
    private var runningSegmentStartMs: Long = 0L
    private var priorFocusedSeconds: Long = 0L
    private var priorPausedSeconds: Long = 0L
    private var pauseSegmentStartMs: Long = 0L

    companion object {
        @Volatile
        private var INSTANCE: TimerEngine? = null

        fun getInstance(context: Context, repository: StudyRepository): TimerEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = TimerEngine(context.applicationContext, repository)
                INSTANCE = instance
                instance
            }
        }
    }

    fun configureSession(
        mode: SessionMode,
        subject: String,
        topic: String = "",
        goal: String = "",
        tags: List<String> = emptyList(),
        durationMinutes: Int = 25,
        pomodoroFocusMin: Int = 25,
        pomodoroShortBreakMin: Int = 5,
        pomodoroLongBreakMin: Int = 15,
        pomodoroRounds: Int = 4,
        autoStartBreaks: Boolean = false,
        autoStartFocus: Boolean = false,
        keepScreenAwake: Boolean = true,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        if (_uiState.value.status == TimerStatus.RUNNING || _uiState.value.status == TimerStatus.PAUSED) {
            return // Don't reconfigure while running
        }

        val targetSec = when (mode) {
            SessionMode.COUNTDOWN -> durationMinutes * 60L
            SessionMode.STOPWATCH -> 0L
            SessionMode.POMODORO -> pomodoroFocusMin * 60L
        }

        _uiState.value = _uiState.value.copy(
            status = TimerStatus.IDLE,
            mode = mode,
            subject = subject,
            topic = topic,
            goal = goal,
            tags = tags,
            targetDurationSeconds = targetSec,
            remainingSeconds = targetSec,
            elapsedSeconds = 0L,
            focusedSeconds = 0L,
            pausedSeconds = 0L,
            progress = 0f,
            pomodoroPhase = PomodoroPhase.FOCUS,
            currentRound = 1,
            totalRounds = pomodoroRounds,
            pomodoroFocusMinutes = pomodoroFocusMin,
            pomodoroShortBreakMinutes = pomodoroShortBreakMin,
            pomodoroLongBreakMinutes = pomodoroLongBreakMin,
            autoStartBreaks = autoStartBreaks,
            autoStartFocus = autoStartFocus,
            distractionCount = 0,
            distractions = emptyList(),
            keepScreenAwake = keepScreenAwake,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            showCompletionSheet = false,
            completedSessionData = null
        )
    }

    fun startSession() {
        val now = System.currentTimeMillis()
        sessionStartMs = now
        runningSegmentStartMs = now
        priorFocusedSeconds = 0L
        priorPausedSeconds = 0L
        pauseSegmentStartMs = 0L

        _uiState.value = _uiState.value.copy(
            status = TimerStatus.RUNNING,
            sessionStartTime = now,
            showCompletionSheet = false
        )

        startTicker()
        updateServiceState(TimerStatus.RUNNING)
    }

    fun pauseSession() {
        if (_uiState.value.status != TimerStatus.RUNNING) return
        val now = System.currentTimeMillis()
        val currentSegmentSec = (now - runningSegmentStartMs) / 1000
        priorFocusedSeconds += currentSegmentSec
        pauseSegmentStartMs = now

        _uiState.value = _uiState.value.copy(
            status = TimerStatus.PAUSED,
            focusedSeconds = priorFocusedSeconds
        )

        updateServiceState(TimerStatus.PAUSED)
    }

    fun resumeSession() {
        if (_uiState.value.status != TimerStatus.PAUSED) return
        val now = System.currentTimeMillis()
        val pauseSegmentSec = (now - pauseSegmentStartMs) / 1000
        priorPausedSeconds += pauseSegmentSec
        runningSegmentStartMs = now

        _uiState.value = _uiState.value.copy(
            status = TimerStatus.RUNNING,
            pausedSeconds = priorPausedSeconds
        )

        updateServiceState(TimerStatus.RUNNING)
    }

    fun addDistraction(category: String) {
        val current = _uiState.value.distractions.toMutableList()
        current.add(DistractionLog(category))
        _uiState.value = _uiState.value.copy(
            distractionCount = current.size,
            distractions = current
        )
    }

    fun finishSession(isEarly: Boolean = false) {
        stopTicker()
        val now = System.currentTimeMillis()
        var finalFocused = priorFocusedSeconds
        var finalPaused = priorPausedSeconds

        if (_uiState.value.status == TimerStatus.RUNNING) {
            finalFocused += (now - runningSegmentStartMs) / 1000
        } else if (_uiState.value.status == TimerStatus.PAUSED) {
            finalPaused += (now - pauseSegmentStartMs) / 1000
        }

        val totalDuration = finalFocused + finalPaused
        val currentState = _uiState.value

        // Only save session if it has at least 5 seconds of focused time
        if (finalFocused >= 5L) {
            val distractionSummary = currentState.distractions
                .groupBy { it.category }
                .map { "${it.key}: ${it.value.size}" }
                .joinToString(", ")

            val sessionEntity = StudySessionEntity(
                startTime = if (sessionStartMs > 0) sessionStartMs else now - (totalDuration * 1000),
                endTime = now,
                durationSeconds = totalDuration,
                actualFocusedSeconds = finalFocused,
                pausedSeconds = finalPaused,
                sessionType = currentState.mode.name,
                subject = currentState.subject,
                topic = currentState.topic,
                goal = currentState.goal,
                tags = currentState.tags.joinToString(","),
                distractionCount = currentState.distractionCount,
                distractionDetails = distractionSummary,
                completionStatus = if (isEarly) "EARLY_FINISH" else "COMPLETED"
            )

            _uiState.value = currentState.copy(
                status = TimerStatus.COMPLETED,
                focusedSeconds = finalFocused,
                pausedSeconds = finalPaused,
                elapsedSeconds = totalDuration,
                showCompletionSheet = true,
                completedSessionData = sessionEntity
            )
        } else {
            // Dismiss without saving if negligible
            _uiState.value = currentState.copy(
                status = TimerStatus.IDLE,
                focusedSeconds = 0L,
                elapsedSeconds = 0L,
                showCompletionSheet = false,
                completedSessionData = null
            )
        }

        updateServiceState(TimerStatus.IDLE)
        playCompletionSignals()
    }

    fun saveCompletedSessionWithFeedback(
        productivityRating: Int,
        mood: String,
        energyLevel: String,
        notes: String,
        location: String
    ) {
        val completed = _uiState.value.completedSessionData ?: return
        scope.launch {
            val updated = completed.copy(
                productivityRating = productivityRating,
                mood = mood,
                energyLevel = energyLevel,
                notes = notes,
                location = location,
                updatedTimestamp = System.currentTimeMillis()
            )
            repository.insertSession(updated)
            _uiState.value = _uiState.value.copy(
                showCompletionSheet = false,
                status = TimerStatus.IDLE,
                completedSessionData = null
            )
        }
    }

    fun dismissCompletionSheet() {
        val completed = _uiState.value.completedSessionData
        if (completed != null) {
            scope.launch {
                repository.insertSession(completed)
            }
        }
        _uiState.value = _uiState.value.copy(
            showCompletionSheet = false,
            status = TimerStatus.IDLE,
            completedSessionData = null
        )
    }

    fun cancelSession() {
        stopTicker()
        _uiState.value = _uiState.value.copy(
            status = TimerStatus.IDLE,
            focusedSeconds = 0L,
            pausedSeconds = 0L,
            elapsedSeconds = 0L,
            distractionCount = 0,
            distractions = emptyList(),
            showCompletionSheet = false,
            completedSessionData = null
        )
        updateServiceState(TimerStatus.IDLE)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                tick()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun tick() {
        if (_uiState.value.status != TimerStatus.RUNNING) return

        val now = System.currentTimeMillis()
        val currentSegmentSec = (now - runningSegmentStartMs) / 1000
        val currentFocused = priorFocusedSeconds + currentSegmentSec
        val totalElapsed = currentFocused + priorPausedSeconds

        val currentState = _uiState.value

        when (currentState.mode) {
            SessionMode.COUNTDOWN -> {
                val remaining = (currentState.targetDurationSeconds - currentFocused).coerceAtLeast(0L)
                val progress = if (currentState.targetDurationSeconds > 0) {
                    (currentFocused.toFloat() / currentState.targetDurationSeconds).coerceIn(0f, 1f)
                } else 1f

                _uiState.value = currentState.copy(
                    focusedSeconds = currentFocused,
                    elapsedSeconds = totalElapsed,
                    remainingSeconds = remaining,
                    progress = progress
                )

                if (remaining <= 0L) {
                    finishSession(isEarly = false)
                }
            }
            SessionMode.STOPWATCH -> {
                _uiState.value = currentState.copy(
                    focusedSeconds = currentFocused,
                    elapsedSeconds = totalElapsed,
                    progress = 1f
                )
            }
            SessionMode.POMODORO -> {
                val targetPhaseSec = when (currentState.pomodoroPhase) {
                    PomodoroPhase.FOCUS -> currentState.pomodoroFocusMinutes * 60L
                    PomodoroPhase.SHORT_BREAK -> currentState.pomodoroShortBreakMinutes * 60L
                    PomodoroPhase.LONG_BREAK -> currentState.pomodoroLongBreakMinutes * 60L
                }

                val remaining = (targetPhaseSec - currentFocused).coerceAtLeast(0L)
                val progress = if (targetPhaseSec > 0) {
                    (currentFocused.toFloat() / targetPhaseSec).coerceIn(0f, 1f)
                } else 1f

                _uiState.value = currentState.copy(
                    focusedSeconds = currentFocused,
                    elapsedSeconds = totalElapsed,
                    remainingSeconds = remaining,
                    progress = progress
                )

                if (remaining <= 0L) {
                    handlePomodoroPhaseTransition()
                }
            }
        }

        if (_uiState.value.status == TimerStatus.RUNNING) {
            updateServiceState(TimerStatus.RUNNING)
        }
    }

    private fun handlePomodoroPhaseTransition() {
        playCompletionSignals()
        val currentState = _uiState.value
        when (currentState.pomodoroPhase) {
            PomodoroPhase.FOCUS -> {
                // Focus ended -> determine if short or long break
                if (currentState.currentRound >= currentState.totalRounds) {
                    // Final round -> Long break or finish
                    _uiState.value = currentState.copy(
                        pomodoroPhase = PomodoroPhase.LONG_BREAK,
                        targetDurationSeconds = currentState.pomodoroLongBreakMinutes * 60L,
                        remainingSeconds = currentState.pomodoroLongBreakMinutes * 60L,
                        progress = 0f
                    )
                } else {
                    // Short break
                    _uiState.value = currentState.copy(
                        pomodoroPhase = PomodoroPhase.SHORT_BREAK,
                        targetDurationSeconds = currentState.pomodoroShortBreakMinutes * 60L,
                        remainingSeconds = currentState.pomodoroShortBreakMinutes * 60L,
                        progress = 0f
                    )
                }
                resetPhaseTimers(currentState.autoStartBreaks)
            }
            PomodoroPhase.SHORT_BREAK -> {
                // Break ended -> next focus round
                val nextRound = currentState.currentRound + 1
                _uiState.value = currentState.copy(
                    pomodoroPhase = PomodoroPhase.FOCUS,
                    currentRound = nextRound,
                    targetDurationSeconds = currentState.pomodoroFocusMinutes * 60L,
                    remainingSeconds = currentState.pomodoroFocusMinutes * 60L,
                    progress = 0f
                )
                resetPhaseTimers(currentState.autoStartFocus)
            }
            PomodoroPhase.LONG_BREAK -> {
                // All rounds finished
                finishSession(isEarly = false)
            }
        }
    }

    private fun resetPhaseTimers(autoStart: Boolean) {
        val now = System.currentTimeMillis()
        runningSegmentStartMs = now
        priorFocusedSeconds = 0L
        priorPausedSeconds = 0L
        pauseSegmentStartMs = 0L

        if (autoStart) {
            _uiState.value = _uiState.value.copy(status = TimerStatus.RUNNING)
            updateServiceState(TimerStatus.RUNNING)
        } else {
            _uiState.value = _uiState.value.copy(status = TimerStatus.PAUSED)
            updateServiceState(TimerStatus.PAUSED)
        }
    }

    private fun playCompletionSignals() {
        try {
            if (_uiState.value.soundEnabled) {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            }
            if (_uiState.value.vibrationEnabled) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateServiceState(status: TimerStatus) {
        try {
            when (status) {
                TimerStatus.RUNNING -> StudyTimerService.startOrUpdate(context, _uiState.value)
                TimerStatus.PAUSED -> StudyTimerService.startOrUpdate(context, _uiState.value)
                TimerStatus.IDLE, TimerStatus.COMPLETED -> StudyTimerService.stop(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
