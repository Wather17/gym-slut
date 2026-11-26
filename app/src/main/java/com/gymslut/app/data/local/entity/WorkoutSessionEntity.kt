package com.gymslut.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sessão de treino executada ou em andamento, preservando o nome da ficha por snapshot.
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutTemplateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["workoutTemplateId"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val planId: Long? = null,
    val workoutTemplateId: Long? = null,
    val workoutNameSnapshot: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null
)
