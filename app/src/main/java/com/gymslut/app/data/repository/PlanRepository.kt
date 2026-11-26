package com.gymslut.app.data.repository

import com.gymslut.app.data.local.dao.PlanDao
import com.gymslut.app.data.local.dao.ScheduleDao
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutExerciseEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.model.WorkoutExerciseDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contrato de gerenciamento de planos de treino, fichas e cronograma semanal.
 */
interface PlanRepository {
    fun observeAllPlans(): Flow<List<TrainingPlanEntity>>
    fun observeActivePlan(): Flow<TrainingPlanEntity?>
    fun observeTemplatesForPlan(planId: Long): Flow<List<WorkoutTemplateEntity>>
    fun observeWorkoutExercises(workoutTemplateId: Long): Flow<List<WorkoutExerciseDetail>>
    fun observeScheduleForPlan(planId: Long): Flow<List<WeeklyScheduleEntity>>

    suspend fun getPlanById(planId: Long): TrainingPlanEntity?
    suspend fun getTemplateById(templateId: Long): WorkoutTemplateEntity?

    suspend fun createPlan(name: String, createdAtEpochMs: Long = System.currentTimeMillis()): Long
    suspend fun renamePlan(planId: Long, newName: String)
    suspend fun deletePlan(planId: Long)
    suspend fun setActivePlan(planId: Long)

    suspend fun createWorkoutTemplate(planId: Long, name: String): Long
    suspend fun renameWorkoutTemplate(templateId: Long, newName: String)
    suspend fun deleteWorkoutTemplate(templateId: Long)
    suspend fun moveWorkoutTemplate(planId: Long, templateId: Long, moveUp: Boolean)

    suspend fun addExerciseToWorkout(
        workoutTemplateId: Long,
        exerciseId: Long,
        targetSets: Int,
        targetReps: Int,
        restSeconds: Int
    ): Long

    suspend fun updateWorkoutExercise(
        id: Long,
        targetSets: Int,
        targetReps: Int,
        restSeconds: Int
    )

    suspend fun removeWorkoutExercise(id: Long)
    suspend fun moveWorkoutExercise(workoutTemplateId: Long, workoutExerciseId: Long, moveUp: Boolean)

    suspend fun setScheduleDay(planId: Long, dayOfWeek: Int, workoutTemplateId: Long?)
}

@Singleton
class RoomPlanRepository @Inject constructor(
    private val planDao: PlanDao,
    private val scheduleDao: ScheduleDao
) : PlanRepository {

    override fun observeAllPlans(): Flow<List<TrainingPlanEntity>> = planDao.observeAll()

    override fun observeActivePlan(): Flow<TrainingPlanEntity?> = planDao.observeActivePlan()

    override fun observeTemplatesForPlan(planId: Long): Flow<List<WorkoutTemplateEntity>> =
        planDao.observeTemplatesForPlan(planId)

    override fun observeWorkoutExercises(workoutTemplateId: Long): Flow<List<WorkoutExerciseDetail>> =
        planDao.observeWorkoutExercisesWithDetails(workoutTemplateId)

    override fun observeScheduleForPlan(planId: Long): Flow<List<WeeklyScheduleEntity>> =
        scheduleDao.observeScheduleForPlan(planId)

    override suspend fun getPlanById(planId: Long): TrainingPlanEntity? = planDao.getById(planId)

    override suspend fun getTemplateById(templateId: Long): WorkoutTemplateEntity? =
        planDao.getTemplateById(templateId)

    override suspend fun createPlan(name: String, createdAtEpochMs: Long): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do plano." }
        val shouldActivate = planDao.countPlans() == 0
        val planId = planDao.insertPlan(
            TrainingPlanEntity(
                name = cleanName,
                isActive = shouldActivate,
                createdAtEpochMs = createdAtEpochMs
            )
        )
        val defaultSchedule = (1..7).map { day ->
            WeeklyScheduleEntity(planId = planId, dayOfWeek = day, workoutTemplateId = null)
        }
        scheduleDao.upsertScheduleEntries(defaultSchedule)
        return planId
    }

    override suspend fun renamePlan(planId: Long, newName: String) {
        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do plano." }
        val current = planDao.getById(planId) ?: return
        planDao.updatePlan(current.copy(name = cleanName))
    }

    override suspend fun deletePlan(planId: Long) {
        planDao.deletePlanById(planId)
    }

    override suspend fun setActivePlan(planId: Long) {
        if (planDao.getById(planId) == null) return
        planDao.setActivePlan(planId)
    }

    override suspend fun createWorkoutTemplate(planId: Long, name: String): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do treino." }
        val nextOrder = (planDao.getTemplatesForPlan(planId).maxOfOrNull { it.orderIndex } ?: -1) + 1
        return planDao.insertTemplate(
            WorkoutTemplateEntity(
                planId = planId,
                name = cleanName,
                orderIndex = nextOrder
            )
        )
    }

    override suspend fun renameWorkoutTemplate(templateId: Long, newName: String) {
        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome do treino." }
        val current = planDao.getTemplateById(templateId) ?: return
        planDao.updateTemplate(current.copy(name = cleanName))
    }

    override suspend fun deleteWorkoutTemplate(templateId: Long) {
        planDao.deleteTemplateById(templateId)
    }

    override suspend fun moveWorkoutTemplate(planId: Long, templateId: Long, moveUp: Boolean) {
        val items = planDao.getTemplatesForPlan(planId).toMutableList()
        val index = items.indexOfFirst { it.id == templateId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in items.indices) return
        val swapped = items[index]
        items[index] = items[targetIndex]
        items[targetIndex] = swapped
        val reindexed = items.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
        planDao.updateTemplates(reindexed)
    }

    override suspend fun addExerciseToWorkout(
        workoutTemplateId: Long,
        exerciseId: Long,
        targetSets: Int,
        targetReps: Int,
        restSeconds: Int
    ): Long {
        require(targetSets >= 1) { "Número de séries deve ser maior que zero." }
        require(targetReps >= 1) { "Número de repetições deve ser maior que zero." }
        require(restSeconds >= 0) { "Descanso não pode ser negativo." }
        val nextOrder =
            (planDao.getWorkoutExercises(workoutTemplateId).maxOfOrNull { it.orderIndex } ?: -1) + 1
        return planDao.insertWorkoutExercise(
            WorkoutExerciseEntity(
                workoutTemplateId = workoutTemplateId,
                exerciseId = exerciseId,
                orderIndex = nextOrder,
                targetSets = targetSets,
                targetReps = targetReps,
                restSeconds = restSeconds
            )
        )
    }

    override suspend fun updateWorkoutExercise(
        id: Long,
        targetSets: Int,
        targetReps: Int,
        restSeconds: Int
    ) {
        require(targetSets >= 1) { "Número de séries deve ser maior que zero." }
        require(targetReps >= 1) { "Número de repetições deve ser maior que zero." }
        require(restSeconds >= 0) { "Descanso não pode ser negativo." }
        val current = planDao.getWorkoutExerciseById(id) ?: return
        planDao.updateWorkoutExercise(
            current.copy(
                targetSets = targetSets,
                targetReps = targetReps,
                restSeconds = restSeconds
            )
        )
    }

    override suspend fun removeWorkoutExercise(id: Long) {
        planDao.deleteWorkoutExerciseById(id)
    }

    override suspend fun moveWorkoutExercise(
        workoutTemplateId: Long,
        workoutExerciseId: Long,
        moveUp: Boolean
    ) {
        val items = planDao.getWorkoutExercises(workoutTemplateId).toMutableList()
        val index = items.indexOfFirst { it.id == workoutExerciseId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in items.indices) return
        val swapped = items[index]
        items[index] = items[targetIndex]
        items[targetIndex] = swapped
        val reindexed = items.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
        planDao.updateWorkoutExercises(reindexed)
    }

    override suspend fun setScheduleDay(planId: Long, dayOfWeek: Int, workoutTemplateId: Long?) {
        require(dayOfWeek in 1..7) { "Dia da semana inválido: $dayOfWeek" }
        scheduleDao.upsertScheduleEntry(
            WeeklyScheduleEntity(
                planId = planId,
                dayOfWeek = dayOfWeek,
                workoutTemplateId = workoutTemplateId
            )
        )
    }
}
