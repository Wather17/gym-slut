package com.gymslut.app.feature.today

import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.model.WorkoutExerciseDetail

data class WeekDaySummary(
    val dayOfWeek: Int,
    val shortLabel: String,
    val isToday: Boolean,
    val workoutName: String?
)

data class TodayUiState(
    val activePlan: TrainingPlanEntity? = null,
    val todayDayOfWeek: Int = 1,
    val weekDays: List<WeekDaySummary> = emptyList(),
    val scheduledTemplate: WorkoutTemplateEntity? = null,
    val scheduledExercises: List<WorkoutExerciseDetail> = emptyList(),
    val allTemplates: List<WorkoutTemplateEntity> = emptyList(),
    val inProgressSession: WorkoutSessionEntity? = null
)
