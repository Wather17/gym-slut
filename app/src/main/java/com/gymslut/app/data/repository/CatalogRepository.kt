package com.gymslut.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.gymslut.app.data.local.dao.EquipmentDao
import com.gymslut.app.data.local.dao.ExerciseDao
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.model.DuplicateResourceException
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.model.ResourceInUseException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contrato de acesso e mutação do catálogo de aparelhos e exercícios.
 */
interface CatalogRepository {
    fun observeEquipment(): Flow<List<EquipmentEntity>>
    fun observeExercisesWithEquipment(): Flow<List<ExerciseWithEquipment>>

    suspend fun createEquipment(
        name: String,
        category: EquipmentCategory,
        notes: String = ""
    ): Long

    suspend fun updateEquipment(
        id: Long,
        name: String,
        category: EquipmentCategory,
        notes: String = ""
    )

    suspend fun deleteEquipment(id: Long)

    suspend fun createExercise(
        name: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscle: MuscleGroup?,
        equipmentId: Long,
        notes: String = ""
    ): Long

    suspend fun updateExercise(
        id: Long,
        name: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscle: MuscleGroup?,
        equipmentId: Long,
        notes: String = ""
    )

    suspend fun deleteExercise(id: Long)
}

@Singleton
class RoomCatalogRepository @Inject constructor(
    private val equipmentDao: EquipmentDao,
    private val exerciseDao: ExerciseDao
) : CatalogRepository {

    override fun observeEquipment(): Flow<List<EquipmentEntity>> = equipmentDao.observeAll()

    override fun observeExercisesWithEquipment(): Flow<List<ExerciseWithEquipment>> =
        exerciseDao.observeAllWithEquipment()

    override suspend fun createEquipment(
        name: String,
        category: EquipmentCategory,
        notes: String
    ): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do aparelho." }
        if (equipmentDao.getByNameIgnoreCase(cleanName) != null) {
            throw DuplicateResourceException("Já existe um aparelho com o nome \"$cleanName\".")
        }
        return equipmentDao.insert(
            EquipmentEntity(
                name = cleanName,
                category = category,
                notes = notes.trim(),
                isCustom = true
            )
        )
    }

    override suspend fun updateEquipment(
        id: Long,
        name: String,
        category: EquipmentCategory,
        notes: String
    ) {
        val current = equipmentDao.getById(id)
            ?: throw IllegalArgumentException("Aparelho não encontrado.")
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do aparelho." }
        val existingWithName = equipmentDao.getByNameIgnoreCase(cleanName)
        if (existingWithName != null && existingWithName.id != id) {
            throw DuplicateResourceException("Já existe um aparelho com o nome \"$cleanName\".")
        }
        equipmentDao.update(
            current.copy(
                name = cleanName,
                category = category,
                notes = notes.trim()
            )
        )
    }

    override suspend fun deleteEquipment(id: Long) {
        val current = equipmentDao.getById(id) ?: return
        if (equipmentDao.countExercisesUsingEquipment(id) > 0) {
            throw ResourceInUseException(
                "Não é possível excluir \"${current.name}\" porque há exercícios vinculados a este aparelho."
            )
        }
        try {
            equipmentDao.delete(current)
        } catch (e: SQLiteConstraintException) {
            throw ResourceInUseException(
                "Não é possível excluir \"${current.name}\" porque ele está em uso."
            )
        }
    }

    override suspend fun createExercise(
        name: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscle: MuscleGroup?,
        equipmentId: Long,
        notes: String
    ): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do exercício." }
        require(equipmentDao.getById(equipmentId) != null) { "Selecione um aparelho válido." }
        return exerciseDao.insert(
            ExerciseEntity(
                name = cleanName,
                primaryMuscle = primaryMuscle,
                secondaryMuscle = secondaryMuscle?.takeIf { it != primaryMuscle },
                equipmentId = equipmentId,
                notes = notes.trim(),
                isCustom = true
            )
        )
    }

    override suspend fun updateExercise(
        id: Long,
        name: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscle: MuscleGroup?,
        equipmentId: Long,
        notes: String
    ) {
        val current = exerciseDao.getById(id)
            ?: throw IllegalArgumentException("Exercício não encontrado.")
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do exercício." }
        require(equipmentDao.getById(equipmentId) != null) { "Selecione um aparelho válido." }
        exerciseDao.update(
            current.copy(
                name = cleanName,
                primaryMuscle = primaryMuscle,
                secondaryMuscle = secondaryMuscle?.takeIf { it != primaryMuscle },
                equipmentId = equipmentId,
                notes = notes.trim()
            )
        )
    }

    override suspend fun deleteExercise(id: Long) {
        val current = exerciseDao.getById(id) ?: return
        if (exerciseDao.countWorkoutUsages(id) > 0) {
            throw ResourceInUseException(
                "Não é possível excluir \"${current.name}\" porque ele faz parte de uma ou mais fichas de treino."
            )
        }
        try {
            exerciseDao.delete(current)
        } catch (e: SQLiteConstraintException) {
            throw ResourceInUseException(
                "Não é possível excluir \"${current.name}\" porque ele está em uso."
            )
        }
    }
}
