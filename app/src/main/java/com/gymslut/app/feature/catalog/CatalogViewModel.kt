package com.gymslut.app.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.model.DuplicateResourceException
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.model.ResourceInUseException
import com.gymslut.app.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private data class FilterState(
        val activeTab: CatalogTab = CatalogTab.EXERCISES,
        val searchQuery: String = "",
        val selectedMuscle: MuscleGroup? = null,
        val selectedEquipmentId: Long? = null,
        val selectedCategory: EquipmentCategory? = null
    )

    private val filters = MutableStateFlow(FilterState())
    private val feedbackMessage = MutableStateFlow<String?>(null)
    private val formError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CatalogUiState> = combine(
        catalogRepository.observeExercisesWithEquipment(),
        catalogRepository.observeEquipment(),
        filters,
        feedbackMessage,
        formError
    ) { exercises, equipment, filterState, feedback, error ->
        val query = filterState.searchQuery.trim()
        val filteredExercises = filterExercises(exercises, query, filterState.selectedMuscle, filterState.selectedEquipmentId)
        val filteredEquipment = filterEquipment(equipment, query, filterState.selectedCategory)
        CatalogUiState(
            activeTab = filterState.activeTab,
            searchQuery = filterState.searchQuery,
            selectedMuscle = filterState.selectedMuscle,
            selectedEquipmentId = filterState.selectedEquipmentId,
            selectedCategory = filterState.selectedCategory,
            exercises = filteredExercises,
            allEquipment = equipment,
            filteredEquipment = filteredEquipment,
            feedbackMessage = feedback,
            formError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState()
    )

    fun selectTab(tab: CatalogTab) {
        filters.value = filters.value.copy(activeTab = tab)
    }

    fun updateSearchQuery(query: String) {
        filters.value = filters.value.copy(searchQuery = query)
    }

    fun selectMuscleFilter(muscle: MuscleGroup?) {
        filters.value = filters.value.copy(selectedMuscle = muscle)
    }

    fun selectEquipmentFilter(equipmentId: Long?) {
        filters.value = filters.value.copy(selectedEquipmentId = equipmentId)
    }

    fun selectCategoryFilter(category: EquipmentCategory?) {
        filters.value = filters.value.copy(selectedCategory = category)
    }

    fun clearFilters() {
        filters.value = filters.value.copy(
            searchQuery = "",
            selectedMuscle = null,
            selectedEquipmentId = null,
            selectedCategory = null
        )
    }

    fun clearFormError() {
        formError.value = null
    }

    fun clearFeedbackMessage() {
        feedbackMessage.value = null
    }

    fun saveEquipment(
        editingId: Long?,
        name: String,
        category: EquipmentCategory,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            formError.value = "Informe o nome do aparelho."
            return
        }
        viewModelScope.launch {
            try {
                if (editingId == null) {
                    catalogRepository.createEquipment(cleanName, category, notes)
                    feedbackMessage.value = "Aparelho \"$cleanName\" adicionado."
                } else {
                    catalogRepository.updateEquipment(editingId, cleanName, category, notes)
                    feedbackMessage.value = "Aparelho \"$cleanName\" atualizado."
                }
                formError.value = null
                onSuccess()
            } catch (e: DuplicateResourceException) {
                formError.value = e.message
            } catch (e: IllegalArgumentException) {
                formError.value = e.message ?: "Dados inválidos."
            }
        }
    }

    fun deleteEquipment(equipment: EquipmentEntity) {
        viewModelScope.launch {
            try {
                catalogRepository.deleteEquipment(equipment.id)
                feedbackMessage.value = "Aparelho \"${equipment.name}\" excluído."
            } catch (e: ResourceInUseException) {
                feedbackMessage.value = e.message
            }
        }
    }

    fun saveExercise(
        editingId: Long?,
        name: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscle: MuscleGroup?,
        equipmentId: Long?,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            formError.value = "Informe o nome do exercício."
            return
        }
        if (equipmentId == null) {
            formError.value = "Selecione um aparelho."
            return
        }
        viewModelScope.launch {
            try {
                if (editingId == null) {
                    catalogRepository.createExercise(
                        name = cleanName,
                        primaryMuscle = primaryMuscle,
                        secondaryMuscle = secondaryMuscle,
                        equipmentId = equipmentId,
                        notes = notes
                    )
                    feedbackMessage.value = "Exercício \"$cleanName\" adicionado."
                } else {
                    catalogRepository.updateExercise(
                        id = editingId,
                        name = cleanName,
                        primaryMuscle = primaryMuscle,
                        secondaryMuscle = secondaryMuscle,
                        equipmentId = equipmentId,
                        notes = notes
                    )
                    feedbackMessage.value = "Exercício \"$cleanName\" atualizado."
                }
                formError.value = null
                onSuccess()
            } catch (e: IllegalArgumentException) {
                formError.value = e.message ?: "Dados inválidos."
            }
        }
    }

    fun deleteExercise(item: ExerciseWithEquipment) {
        viewModelScope.launch {
            try {
                catalogRepository.deleteExercise(item.exercise.id)
                feedbackMessage.value = "Exercício \"${item.exercise.name}\" excluído."
            } catch (e: ResourceInUseException) {
                feedbackMessage.value = e.message
            }
        }
    }

    private fun filterExercises(
        items: List<ExerciseWithEquipment>,
        query: String,
        muscle: MuscleGroup?,
        equipmentId: Long?
    ): List<ExerciseWithEquipment> {
        return items.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.exercise.name.contains(query, ignoreCase = true) ||
                item.exercise.notes.contains(query, ignoreCase = true) ||
                item.equipmentName.contains(query, ignoreCase = true)
            val matchesMuscle = muscle == null ||
                item.exercise.primaryMuscle == muscle ||
                item.exercise.secondaryMuscle == muscle
            val matchesEquipment = equipmentId == null || item.exercise.equipmentId == equipmentId
            matchesQuery && matchesMuscle && matchesEquipment
        }
    }

    private fun filterEquipment(
        items: List<EquipmentEntity>,
        query: String,
        category: EquipmentCategory?
    ): List<EquipmentEntity> {
        return items.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.name.contains(query, ignoreCase = true) ||
                item.notes.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            matchesQuery && matchesCategory
        }
    }
}
