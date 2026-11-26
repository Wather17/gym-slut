package com.gymslut.app.data.model

import androidx.room.Embedded
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.ExerciseEntity

/**
 * Projeção de exercício combinado com os dados do aparelho associado.
 */
data class ExerciseWithEquipment(
    @Embedded val exercise: ExerciseEntity,
    val equipmentName: String,
    val equipmentCategory: EquipmentCategory
)

/**
 * Projeção de exercício prescrito em uma ficha com detalhes do catálogo.
 */
data class WorkoutExerciseDetail(
    val id: Long,
    val workoutTemplateId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val exerciseName: String,
    val equipmentName: String
)

/**
 * Exceção de domínio lançada quando uma exclusão viola integridade referencial em uso.
 */
class ResourceInUseException(message: String) : IllegalStateException(message)

/**
 * Exceção de domínio lançada quando um registro único (ex.: nome de aparelho) já existe.
 */
class DuplicateResourceException(message: String) : IllegalArgumentException(message)
