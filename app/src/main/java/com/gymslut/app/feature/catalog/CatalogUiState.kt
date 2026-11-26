package com.gymslut.app.feature.catalog

import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.model.ExerciseWithEquipment

enum class CatalogTab(val labelPtBr: String) {
    EXERCISES("Exercícios"),
    EQUIPMENT("Aparelhos")
}

data class CatalogUiState(
    val activeTab: CatalogTab = CatalogTab.EXERCISES,
    val searchQuery: String = "",
    val selectedMuscle: MuscleGroup? = null,
    val selectedEquipmentId: Long? = null,
    val selectedCategory: EquipmentCategory? = null,
    val exercises: List<ExerciseWithEquipment> = emptyList(),
    val allEquipment: List<EquipmentEntity> = emptyList(),
    val filteredEquipment: List<EquipmentEntity> = emptyList(),
    val feedbackMessage: String? = null,
    val formError: String? = null
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            (activeTab == CatalogTab.EXERCISES && (selectedMuscle != null || selectedEquipmentId != null)) ||
            (activeTab == CatalogTab.EQUIPMENT && selectedCategory != null)
}
