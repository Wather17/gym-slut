package com.gymslut.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.model.WorkoutExerciseDetail
import com.gymslut.app.data.repository.PlanRepository
import com.gymslut.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestTriggerEvent(
    val exerciseName: String,
    val restSeconds: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _restTriggers = MutableSharedFlow<RestTriggerEvent>(extraBufferCapacity = 4)
    val restTriggers: SharedFlow<RestTriggerEvent> = _restTriggers.asSharedFlow()

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
        templateExercisesFlow
    ) { session, sets, templateDetails ->
        val restByOrder = templateDetails.associate { it.orderIndex to it.restSeconds }
        val restByExerciseId = templateDetails.associate { it.exerciseId to it.restSeconds }

        val groups = sets
            .groupBy { it.exerciseOrderIndex }
            .toSortedMap()
            .map { (orderIndex, exerciseSets) ->
                val first = exerciseSets.first()
                val restSeconds = restByOrder[orderIndex]
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
            exerciseGroups = groups
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveWorkoutUiState()
    )

    fun updateSetWeight(set: SessionSetEntity, rawWeight: String) {
        val parsed = parseWeight(rawWeight) ?: return
        viewModelScope.launch {
            sessionRepository.updateSet(set.copy(weightKg = parsed))
        }
    }

    fun updateSetReps(set: SessionSetEntity, rawReps: String) {
        val parsed = rawReps.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return
        viewModelScope.launch {
            sessionRepository.updateSet(set.copy(reps = parsed))
        }
    }

    fun toggleSetCompleted(set: SessionSetEntity, restSeconds: Int) {
        val newCompleted = !set.completed
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        viewModelScope.launch {
            sessionRepository.updateSet(
                set.copy(
                    completed = newCompleted,
                    completedAtEpochMs = completedAt
                )
            )
            if (newCompleted && restSeconds > 0) {
                _restTriggers.tryEmit(
                    RestTriggerEvent(
                        exerciseName = set.exerciseNameSnapshot,
                        restSeconds = restSeconds
                    )
                )
            }
        }
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
        viewModelScope.launch {
            sessionRepository.finishSession(sessionId)
            onFinished()
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.discardSession(sessionId)
            onDiscarded()
        }
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
