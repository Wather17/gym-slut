package com.gymslut.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.core.notification.RestAlertNotifier
import com.gymslut.app.core.preferences.PreferencesRepository
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.model.WorkoutExerciseDetail
import com.gymslut.app.data.repository.PlanRepository
import com.gymslut.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val planRepository: PlanRepository,
    private val preferencesRepository: PreferencesRepository,
    elapsedTimeProvider: ElapsedTimeProvider,
    private val restAlertNotifier: RestAlertNotifier
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val restTimerController = RestTimerController(viewModelScope, elapsedTimeProvider)
    private var isClosingSession = false

    val timerState: StateFlow<RestTimerState> = restTimerController.state

    init {
        viewModelScope.launch {
            restTimerController.finishedEvents.collect { exerciseName ->
                val vibrate = preferencesRepository.workoutPreferences.first().restTimerVibrationEnabled
                restAlertNotifier.notifyRestFinished(exerciseName, vibrate = vibrate)
            }
        }
    }

    private val sessionFlow = sessionRepository.observeSession(sessionId)

    private val templateExercisesFlow: Flow<List<WorkoutExerciseDetail>> =
        sessionFlow.flatMapLatest { session ->
            val templateId = session?.workoutTemplateId
            if (templateId == null) {
                flowOf(emptyList())
            } else {
                planRepository.observeWorkoutExercises(templateId)
            }
        }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        sessionFlow,
        sessionRepository.observeSetsForSession(sessionId),
        templateExercisesFlow,
        preferencesRepository.workoutPreferences
    ) { session, sets, templateDetails, workoutPrefs ->
        val restByExerciseId = templateDetails.associate { it.exerciseId to it.restSeconds }

        val groups = sets
            .groupBy { it.exerciseOrderIndex }
            .toSortedMap()
            .map { (orderIndex, exerciseSets) ->
                val first = exerciseSets.first()
                val restSeconds = templateDetails.getOrNull(orderIndex)
                    ?.takeIf { it.exerciseId == first.exerciseId }
                    ?.restSeconds
                    ?: first.exerciseId?.let { restByExerciseId[it] }
                    ?: DEFAULT_REST_SECONDS
                ActiveExerciseGroup(
                    exerciseOrderIndex = orderIndex,
                    exerciseId = first.exerciseId,
                    exerciseName = first.exerciseNameSnapshot,
                    equipmentName = first.equipmentNameSnapshot,
                    restSeconds = restSeconds,
                    sets = exerciseSets.sortedBy { it.setNumber }
                )
            }

        ActiveWorkoutUiState(
            session = session,
            exerciseGroups = groups,
            workoutPreferences = workoutPrefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveWorkoutUiState()
    )

    fun updateSetWeight(set: SessionSetEntity, rawWeight: String) {
        val parsed = parseWeight(rawWeight) ?: return
        viewModelScope.launch {
            sessionRepository.updateSetWeight(set.id, parsed)
        }
    }

    fun updateSetReps(set: SessionSetEntity, rawReps: String) {
        val parsed = rawReps.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return
        viewModelScope.launch {
            sessionRepository.updateSetReps(set.id, parsed)
        }
    }

    fun toggleSetCompleted(set: SessionSetEntity, restSeconds: Int) {
        val newCompleted = !set.completed
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        viewModelScope.launch {
            sessionRepository.updateSetCompleted(set.id, newCompleted, completedAt)
            val autoStart = preferencesRepository.workoutPreferences.first().autoStartRestTimer
            if (newCompleted && restSeconds > 0 && autoStart) {
                restTimerController.start(set.exerciseNameSnapshot, restSeconds)
            }
        }
    }

    fun addTimerSeconds(deltaSeconds: Int) {
        restTimerController.addSeconds(deltaSeconds)
    }

    fun toggleTimerPause() {
        if (timerState.value.isPaused) {
            restTimerController.resume()
        } else {
            restTimerController.pause()
        }
    }

    fun skipTimer() {
        restTimerController.skip()
    }

    fun addExtraSet(exerciseOrderIndex: Int) {
        viewModelScope.launch {
            sessionRepository.addExtraSet(sessionId, exerciseOrderIndex)
        }
    }

    fun removeSet(setId: Long) {
        viewModelScope.launch {
            sessionRepository.removeSet(setId)
        }
    }

    fun finishWorkout(onFinished: () -> Unit) {
        if (isClosingSession) return
        isClosingSession = true
        restTimerController.cancel()
        restAlertNotifier.cancelNotification()
        viewModelScope.launch {
            try {
                sessionRepository.finishSession(sessionId)
                onFinished()
            } finally {
                isClosingSession = false
            }
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        if (isClosingSession) return
        isClosingSession = true
        restTimerController.cancel()
        restAlertNotifier.cancelNotification()
        viewModelScope.launch {
            try {
                sessionRepository.discardSession(sessionId)
                onDiscarded()
            } finally {
                isClosingSession = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        restTimerController.cancel()
    }

    companion object {
        const val DEFAULT_REST_SECONDS = 60

        fun parseWeight(raw: String): Double? {
            val normalized = raw.trim().replace(',', '.')
            val parsed = normalized.toDoubleOrNull() ?: return null
            return parsed.takeIf { it >= 0.0 }
        }
    }
}
