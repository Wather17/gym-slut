package com.gymslut.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exercício de musculação vinculado a um aparelho do catálogo.
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["equipmentId"])]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscle: MuscleGroup? = null,
    val equipmentId: Long,
    val notes: String = "",
    val isCustom: Boolean = false
)
