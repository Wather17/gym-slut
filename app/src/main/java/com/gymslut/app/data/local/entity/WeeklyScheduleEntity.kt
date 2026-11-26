package com.gymslut.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Associação de um dia da semana ISO-8601 (1=Segunda .. 7=Domingo) a um treino ou descanso (null).
 */
@Entity(
    tableName = "weekly_schedule",
    primaryKeys = ["planId", "dayOfWeek"],
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutTemplateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["workoutTemplateId"])]
)
data class WeeklyScheduleEntity(
    val planId: Long,
    val dayOfWeek: Int,
    val workoutTemplateId: Long? = null
)
