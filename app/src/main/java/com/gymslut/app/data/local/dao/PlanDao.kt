package com.gymslut.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WorkoutExerciseEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.model.WorkoutExerciseDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM training_plans ORDER BY isActive DESC, createdAtEpochMs DESC")
    fun observeAll(): Flow<List<TrainingPlanEntity>>

    @Query("SELECT * FROM training_plans WHERE isActive = 1 LIMIT 1")
    fun observeActivePlan(): Flow<TrainingPlanEntity?>

    @Query("SELECT * FROM training_plans WHERE id = :id")
    suspend fun getById(id: Long): TrainingPlanEntity?

    @Query("SELECT * FROM training_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlan(): TrainingPlanEntity?

    @Query("SELECT COUNT(*) FROM training_plans")
    suspend fun countPlans(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlan(plan: TrainingPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: TrainingPlanEntity)

    @Query("DELETE FROM training_plans WHERE id = :planId")
    suspend fun deletePlanById(planId: Long)

    @Query("UPDATE training_plans SET isActive = 0")
    suspend fun clearActivePlans()

    @Query("UPDATE training_plans SET isActive = 1 WHERE id = :planId")
    suspend fun markPlanActive(planId: Long)

    @Transaction
    suspend fun setActivePlan(planId: Long) {
        clearActivePlans()
        markPlanActive(planId)
    }

    @Query("SELECT * FROM workout_templates WHERE planId = :planId ORDER BY orderIndex ASC, id ASC")
    fun observeTemplatesForPlan(planId: Long): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE planId = :planId ORDER BY orderIndex ASC, id ASC")
    suspend fun getTemplatesForPlan(planId: Long): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: Long): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: WorkoutTemplateEntity)

    @Update
    suspend fun updateTemplates(templates: List<WorkoutTemplateEntity>)

    @Query("DELETE FROM workout_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: Long)

    @Query(
        """
        SELECT we.id, we.workoutTemplateId, we.exerciseId, we.orderIndex,
               we.targetSets, we.targetReps, we.restSeconds,
               e.name AS exerciseName, eq.name AS equipmentName
        FROM workout_exercises we
        INNER JOIN exercises e ON we.exerciseId = e.id
        INNER JOIN equipment eq ON e.equipmentId = eq.id
        WHERE we.workoutTemplateId = :workoutTemplateId
        ORDER BY we.orderIndex ASC, we.id ASC
        """
    )
    fun observeWorkoutExercisesWithDetails(workoutTemplateId: Long): Flow<List<WorkoutExerciseDetail>>

    @Query(
        """
        SELECT we.id, we.workoutTemplateId, we.exerciseId, we.orderIndex,
               we.targetSets, we.targetReps, we.restSeconds,
               e.name AS exerciseName, eq.name AS equipmentName
        FROM workout_exercises we
        INNER JOIN exercises e ON we.exerciseId = e.id
        INNER JOIN equipment eq ON e.equipmentId = eq.id
        WHERE we.workoutTemplateId = :workoutTemplateId
        ORDER BY we.orderIndex ASC, we.id ASC
        """
    )
    suspend fun getWorkoutExercisesWithDetails(workoutTemplateId: Long): List<WorkoutExerciseDetail>

    @Query("SELECT * FROM workout_exercises WHERE workoutTemplateId = :workoutTemplateId ORDER BY orderIndex ASC, id ASC")
    suspend fun getWorkoutExercises(workoutTemplateId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExerciseById(id: Long): WorkoutExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWorkoutExercise(item: WorkoutExerciseEntity): Long

    @Update
    suspend fun updateWorkoutExercise(item: WorkoutExerciseEntity)

    @Update
    suspend fun updateWorkoutExercises(items: List<WorkoutExerciseEntity>)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExerciseById(id: Long)
}
