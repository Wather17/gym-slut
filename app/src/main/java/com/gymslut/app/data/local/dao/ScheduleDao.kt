package com.gymslut.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM weekly_schedule WHERE planId = :planId ORDER BY dayOfWeek ASC")
    fun observeScheduleForPlan(planId: Long): Flow<List<WeeklyScheduleEntity>>

    @Query("SELECT * FROM weekly_schedule WHERE planId = :planId ORDER BY dayOfWeek ASC")
    suspend fun getScheduleForPlan(planId: Long): List<WeeklyScheduleEntity>

    @Query("SELECT * FROM weekly_schedule WHERE planId = :planId AND dayOfWeek = :dayOfWeek")
    suspend fun getScheduleEntry(planId: Long, dayOfWeek: Int): WeeklyScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleEntry(entry: WeeklyScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleEntries(entries: List<WeeklyScheduleEntity>)
}
