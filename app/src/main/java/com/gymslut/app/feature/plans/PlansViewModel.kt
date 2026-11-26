package com.gymslut.app.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansUiState(
    val plans: List<TrainingPlanEntity> = emptyList(),
    val formError: String? = null,
    val feedbackMessage: String? = null
)

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    private val formError = MutableStateFlow<String?>(null)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlansUiState> = combine(
        planRepository.observeAllPlans(),
        formError,
        feedbackMessage
    ) { plans, error, feedback ->
        PlansUiState(
            plans = plans,
            formError = error,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlansUiState()
    )

    fun clearFormError() {
        formError.value = null
    }

    fun clearFeedbackMessage() {
        feedbackMessage.value = null
    }

    fun savePlan(editingId: Long?, name: String, onSuccess: () -> Unit) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            formError.value = "Informe o nome do plano."
            return
        }
        viewModelScope.launch {
            if (editingId == null) {
                planRepository.createPlan(cleanName)
                feedbackMessage.value = "Plano \"$cleanName\" criado."
            } else {
                planRepository.renamePlan(editingId, cleanName)
                feedbackMessage.value = "Plano renomeado para \"$cleanName\"."
            }
            formError.value = null
            onSuccess()
        }
    }

    fun activatePlan(plan: TrainingPlanEntity) {
        viewModelScope.launch {
            planRepository.setActivePlan(plan.id)
            feedbackMessage.value = "Plano \"${plan.name}\" definido como ativo."
        }
    }

    fun deletePlan(plan: TrainingPlanEntity) {
        viewModelScope.launch {
            planRepository.deletePlan(plan.id)
            feedbackMessage.value = "Plano \"${plan.name}\" excluído."
        }
    }
}
