package com.gymslut.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Série executada dentro de uma sessão, com snapshots de exercício e aparelho.
 */
@Entity(
    tableName = "session_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"])
    ]
)
data class SessionSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long? = null,
    val exerciseNameSnapshot: String,
    val equipmentNameSnapshot: String,
    val exerciseOrderIndex: Int,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val completed: Boolean = false,
    val completedAtEpochMs: Long? = null
)
