package com.gymslut.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymslut.app.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {

    @Query("SELECT * FROM equipment ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getById(id: Long): EquipmentEntity?

    @Query("SELECT * FROM equipment WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getByNameIgnoreCase(name: String): EquipmentEntity?

    @Query("SELECT COUNT(*) FROM exercises WHERE equipmentId = :equipmentId")
    suspend fun countExercisesUsingEquipment(equipmentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: EquipmentEntity): Long

    @Update
    suspend fun update(entity: EquipmentEntity)

    @Delete
    suspend fun delete(entity: EquipmentEntity)
}
