package com.gymslut.app.feature.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanDetailUiState(
    val plan: TrainingPlanEntity? = null,
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val formError: String? = null,
    val feedbackMessage: String? = null
)

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val planId: Long = checkNotNull(savedStateHandle["planId"])
    private val formError = MutableStateFlow<String?>(null)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlanDetailUiState> = combine(
        planRepository.observeAllPlans().map { plans -> plans.firstOrNull { it.id == planId } },
        planRepository.observeTemplatesForPlan(planId),
        formError,
        feedbackMessage
    ) { plan, templates, error, feedback ->
        PlanDetailUiState(
            plan = plan,
            templates = templates,
            formError = error,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanDetailUiState()
    )

    fun clearFormError() {
        formError.value = null
    }

    fun clearFeedbackMessage() {
        feedbackMessage.value = null
    }

    fun activatePlan() {
        viewModelScope.launch {
            planRepository.setActivePlan(planId)
            feedbackMessage.value = "Plano definido como ativo."
        }
    }

    fun saveWorkoutTemplate(editingId: Long?, name: String, onSuccess: () -> Unit) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            formError.value = "Informe o nome do treino."
            return
        }
        viewModelScope.launch {
            if (editingId == null) {
                planRepository.createWorkoutTemplate(planId, cleanName)
                feedbackMessage.value = "Treino \"$cleanName\" criado."
            } else {
                planRepository.renameWorkoutTemplate(editingId, cleanName)
                feedbackMessage.value = "Treino renomeado para \"$cleanName\"."
            }
            formError.value = null
            onSuccess()
        }
    }

    fun deleteWorkoutTemplate(template: WorkoutTemplateEntity) {
        viewModelScope.launch {
            planRepository.deleteWorkoutTemplate(template.id)
            feedbackMessage.value = "Treino \"${template.name}\" excluído."
        }
    }

    fun moveWorkoutTemplate(template: WorkoutTemplateEntity, moveUp: Boolean) {
        viewModelScope.launch {
            planRepository.moveWorkoutTemplate(planId, template.id, moveUp)
        }
    }
}
