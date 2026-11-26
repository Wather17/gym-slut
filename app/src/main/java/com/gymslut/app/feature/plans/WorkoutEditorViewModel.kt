package com.gymslut.app.feature.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.model.WorkoutExerciseDetail
import com.gymslut.app.data.repository.CatalogRepository
import com.gymslut.app.data.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutEditorUiState(
    val template: WorkoutTemplateEntity? = null,
    val workoutExercises: List<WorkoutExerciseDetail> = emptyList(),
    val catalogExercises: List<ExerciseWithEquipment> = emptyList(),
    val pickerSearchQuery: String = "",
    val pickerMuscleFilter: MuscleGroup? = null,
    val formError: String? = null,
    val feedbackMessage: String? = null
)

@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    catalogRepository: CatalogRepository
) : ViewModel() {

    private val workoutTemplateId: Long = checkNotNull(savedStateHandle["workoutTemplateId"])
    private val templateFlow = MutableStateFlow<WorkoutTemplateEntity?>(null)
    private val pickerSearch = MutableStateFlow("")
    private val pickerMuscle = MutableStateFlow<MuscleGroup?>(null)
    private val formError = MutableStateFlow<String?>(null)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            templateFlow.value = planRepository.getTemplateById(workoutTemplateId)
        }
    }

    val uiState: StateFlow<WorkoutEditorUiState> = combine(
        templateFlow,
        planRepository.observeWorkoutExercises(workoutTemplateId),
        catalogRepository.observeExercisesWithEquipment(),
        combine(pickerSearch, pickerMuscle, formError, feedbackMessage) { q, m, err, fb ->
            FilterMeta(q, m, err, fb)
        }
    ) { template, workoutExercises, catalog, meta ->
        val query = meta.searchQuery.trim()
        val filteredCatalog = catalog.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.exercise.name.contains(query, ignoreCase = true) ||
                item.equipmentName.contains(query, ignoreCase = true)
            val matchesMuscle = meta.muscle == null ||
                item.exercise.primaryMuscle == meta.muscle ||
                item.exercise.secondaryMuscle == meta.muscle
            matchesQuery && matchesMuscle
        }
        WorkoutEditorUiState(
            template = template,
            workoutExercises = workoutExercises,
            catalogExercises = filteredCatalog,
            pickerSearchQuery = meta.searchQuery,
            pickerMuscleFilter = meta.muscle,
            formError = meta.formError,
            feedbackMessage = meta.feedbackMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutEditorUiState()
    )

    fun updatePickerSearch(query: String) {
        pickerSearch.value = query
    }

    fun selectPickerMuscle(muscle: MuscleGroup?) {
        pickerMuscle.value = muscle
    }

    fun clearFormError() {
        formError.value = null
    }

    fun clearFeedbackMessage() {
        feedbackMessage.value = null
    }

    fun addExercise(
        exerciseId: Long,
        targetSetsRaw: String,
        targetRepsRaw: String,
        restSecondsRaw: String,
        onSuccess: () -> Unit
    ) {
        val validated = validatePrescription(targetSetsRaw, targetRepsRaw, restSecondsRaw) ?: return
        viewModelScope.launch {
            planRepository.addExerciseToWorkout(
                workoutTemplateId = workoutTemplateId,
                exerciseId = exerciseId,
                targetSets = validated.sets,
                targetReps = validated.reps,
                restSeconds = validated.restSeconds
            )
            formError.value = null
            feedbackMessage.value = "Exercício adicionado ao treino."
            onSuccess()
        }
    }

    fun updatePrescription(
        workoutExerciseId: Long,
        targetSetsRaw: String,
        targetRepsRaw: String,
        restSecondsRaw: String,
        onSuccess: () -> Unit
    ) {
        val validated = validatePrescription(targetSetsRaw, targetRepsRaw, restSecondsRaw) ?: return
        viewModelScope.launch {
            planRepository.updateWorkoutExercise(
                id = workoutExerciseId,
                targetSets = validated.sets,
                targetReps = validated.reps,
                restSeconds = validated.restSeconds
            )
            formError.value = null
            feedbackMessage.value = "Prescrição atualizada."
            onSuccess()
        }
    }

    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            planRepository.removeWorkoutExercise(workoutExerciseId)
            feedbackMessage.value = "Exercício removido do treino."
        }
    }

    fun moveExercise(workoutExerciseId: Long, moveUp: Boolean) {
        viewModelScope.launch {
            planRepository.moveWorkoutExercise(workoutTemplateId, workoutExerciseId, moveUp)
        }
    }

    private fun validatePrescription(
        targetSetsRaw: String,
        targetRepsRaw: String,
        restSecondsRaw: String
    ): PrescriptionValues? {
        val sets = targetSetsRaw.trim().toIntOrNull()
        if (sets == null || sets < 1) {
            formError.value = "Informe um número de séries válido (>= 1)."
            return null
        }
        val reps = targetRepsRaw.trim().toIntOrNull()
        if (reps == null || reps < 1) {
            formError.value = "Informe um número de repetições válido (>= 1)."
            return null
        }
        val rest = restSecondsRaw.trim().toIntOrNull()
        if (rest == null || rest < 0) {
            formError.value = "Informe um descanso válido em segundos (>= 0)."
            return null
        }
        return PrescriptionValues(sets, reps, rest)
    }

    private data class PrescriptionValues(
        val sets: Int,
        val reps: Int,
        val restSeconds: Int
    )

    private data class FilterMeta(
        val searchQuery: String,
        val muscle: MuscleGroup?,
        val formError: String?,
        val feedbackMessage: String?
    )
}
