package com.gymslut.app.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity

/**
 * Relação Room de uma sessão com suas séries executadas.
 */
data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val sets: List<SessionSetEntity>
)

/**
 * Linha retornada pelo DAO para compor o histórico de cargas de um exercício.
 */
data class ExerciseHistoryRow(
    val sessionId: Long,
    val workoutNameSnapshot: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val exerciseNameSnapshot: String,
    val equipmentNameSnapshot: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val completed: Boolean
)
