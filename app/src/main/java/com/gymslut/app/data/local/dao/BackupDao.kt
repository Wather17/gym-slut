package com.gymslut.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutExerciseEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity

@Dao
interface BackupDao {

    @Query("SELECT * FROM equipment ORDER BY id ASC")
    suspend fun getAllEquipment(): List<EquipmentEntity>

    @Query("SELECT * FROM exercises ORDER BY id ASC")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM training_plans ORDER BY id ASC")
    suspend fun getAllPlans(): List<TrainingPlanEntity>

    @Query("SELECT * FROM workout_templates ORDER BY id ASC")
    suspend fun getAllTemplates(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_exercises ORDER BY id ASC")
    suspend fun getAllWorkoutExercises(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM weekly_schedule ORDER BY planId ASC, dayOfWeek ASC")
    suspend fun getAllWeeklySchedules(): List<WeeklyScheduleEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY id ASC")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM session_sets ORDER BY id ASC")
    suspend fun getAllSessionSets(): List<SessionSetEntity>

    @Query("DELETE FROM session_sets")
    suspend fun clearSessionSets()

    @Query("DELETE FROM workout_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM weekly_schedule")
    suspend fun clearWeeklySchedule()

    @Query("DELETE FROM workout_exercises")
    suspend fun clearWorkoutExercises()

    @Query("DELETE FROM workout_templates")
    suspend fun clearTemplates()

    @Query("DELETE FROM training_plans")
    suspend fun clearPlans()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM equipment")
    suspend fun clearEquipment()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(items: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(items: List<TrainingPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(items: List<WorkoutTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(items: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySchedule(items: List<WeeklyScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(items: List<WorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionSets(items: List<SessionSetEntity>)

    @Transaction
    suspend fun replaceAllData(
        equipment: List<EquipmentEntity>,
        exercises: List<ExerciseEntity>,
        plans: List<TrainingPlanEntity>,
        templates: List<WorkoutTemplateEntity>,
        workoutExercises: List<WorkoutExerciseEntity>,
        weeklySchedule: List<WeeklyScheduleEntity>,
        sessions: List<WorkoutSessionEntity>,
        sessionSets: List<SessionSetEntity>
    ) {
        clearSessionSets()
        clearSessions()
        clearWeeklySchedule()
        clearWorkoutExercises()
        clearTemplates()
        clearPlans()
        clearExercises()
        clearEquipment()

        insertEquipment(equipment)
        insertExercises(exercises)
        insertPlans(plans)
        insertTemplates(templates)
        insertWorkoutExercises(workoutExercises)
        insertWeeklySchedule(weeklySchedule)
        insertSessions(sessions)
        insertSessionSets(sessionSets)
    }
}
