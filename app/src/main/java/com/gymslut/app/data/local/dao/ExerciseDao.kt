package com.gymslut.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.model.ExerciseWithEquipment
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query(
        """
        SELECT e.*, eq.name AS equipmentName, eq.category AS equipmentCategory
        FROM exercises e
        INNER JOIN equipment eq ON e.equipmentId = eq.id
        ORDER BY e.name COLLATE NOCASE ASC
        """
    )
    fun observeAllWithEquipment(): Flow<List<ExerciseWithEquipment>>

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query(
        """
        SELECT * FROM exercises
        WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND equipmentId = :equipmentId
        LIMIT 1
        """
    )
    suspend fun getByNameAndEquipment(name: String, equipmentId: Long): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE exerciseId = :exerciseId")
    suspend fun countWorkoutUsages(exerciseId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ExerciseEntity): Long

    @Update
    suspend fun update(entity: ExerciseEntity)

    @Delete
    suspend fun delete(entity: ExerciseEntity)
}
