package com.gymslut.app.feature.workout

import com.gymslut.app.core.preferences.WorkoutPreferences
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity

data class ActiveExerciseGroup(
    val exerciseOrderIndex: Int,
    val exerciseId: Long?,
    val exerciseName: String,
    val equipmentName: String,
    val restSeconds: Int,
    val sets: List<SessionSetEntity>
)

data class ActiveWorkoutUiState(
    val session: WorkoutSessionEntity? = null,
    val exerciseGroups: List<ActiveExerciseGroup> = emptyList(),
    val workoutPreferences: WorkoutPreferences = WorkoutPreferences()
) {
    val totalSetsCount: Int get() = exerciseGroups.sumOf { it.sets.size }
    val completedSetsCount: Int get() = exerciseGroups.sumOf { group -> group.sets.count { it.completed } }
}
