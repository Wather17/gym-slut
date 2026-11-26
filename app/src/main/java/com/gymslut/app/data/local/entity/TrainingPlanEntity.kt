package com.gymslut.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Plano ou divisão de treino salva pelo usuário (apenas um plano fica ativo por vez).
 */
@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val isActive: Boolean = false,
    val createdAtEpochMs: Long
)
