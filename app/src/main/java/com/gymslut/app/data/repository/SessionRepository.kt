package com.gymslut.app.data.repository

import com.gymslut.app.data.local.dao.PlanDao
import com.gymslut.app.data.local.dao.SessionDao
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.model.ExerciseHistoryRow
import com.gymslut.app.data.model.SessionWithSets
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contrato de execução de sessões de treino e consulta de histórico.
 */
interface SessionRepository {
    fun observeInProgressSession(): Flow<WorkoutSessionEntity?>
    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>>
    fun observeCompletedSessionsWithSets(): Flow<List<SessionWithSets>>
    fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryRow>>
    fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?>
    fun observeSetsForSession(sessionId: Long): Flow<List<SessionSetEntity>>

    suspend fun startWorkoutSession(
        workoutTemplateId: Long,
        startedAtEpochMs: Long = System.currentTimeMillis()
    ): Long

    suspend fun updateSet(set: SessionSetEntity)
    suspend fun updateSetWeight(setId: Long, weightKg: Double)
    suspend fun updateSetReps(setId: Long, reps: Int)
    suspend fun updateSetCompleted(setId: Long, completed: Boolean, completedAtEpochMs: Long?)
    suspend fun addExtraSet(sessionId: Long, exerciseOrderIndex: Int): Long
    suspend fun removeSet(setId: Long)
    suspend fun finishSession(sessionId: Long, finishedAtEpochMs: Long = System.currentTimeMillis())
    suspend fun discardSession(sessionId: Long)
}

@Singleton
class RoomSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val planDao: PlanDao
) : SessionRepository {

    override fun observeInProgressSession(): Flow<WorkoutSessionEntity?> =
        sessionDao.observeInProgressSession()

    override fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>> =
        sessionDao.observeCompletedSessions()

    override fun observeCompletedSessionsWithSets(): Flow<List<SessionWithSets>> =
        sessionDao.observeCompletedSessionsWithSets()

    override fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryRow>> =
        sessionDao.observeCompletedExerciseHistory(exerciseId)

    override fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> =
        sessionDao.observeSessionById(sessionId)

    override fun observeSetsForSession(sessionId: Long): Flow<List<SessionSetEntity>> =
        sessionDao.observeSetsForSession(sessionId)

    override suspend fun startWorkoutSession(
        workoutTemplateId: Long,
        startedAtEpochMs: Long
    ): Long {
        val template = planDao.getTemplateById(workoutTemplateId)
            ?: throw IllegalArgumentException("Treino não encontrado.")
        val exercises = planDao.getWorkoutExercisesWithDetails(workoutTemplateId)

        val setsToInsert = mutableListOf<SessionSetEntity>()
        exercises.forEachIndexed { orderIndex, detail ->
            val previousSets = sessionDao.getLatestCompletedSetsForExercise(detail.exerciseId)
            val fallbackLastSet = previousSets.lastOrNull()

            for (setNumber in 1..detail.targetSets) {
                val suggested = previousSets.firstOrNull { it.setNumber == setNumber } ?: fallbackLastSet
                setsToInsert += SessionSetEntity(
                    sessionId = 0L,
                    exerciseId = detail.exerciseId,
                    exerciseNameSnapshot = detail.exerciseName,
                    equipmentNameSnapshot = detail.equipmentName,
                    exerciseOrderIndex = orderIndex,
                    setNumber = setNumber,
                    weightKg = suggested?.weightKg ?: 0.0,
                    reps = suggested?.reps ?: detail.targetReps,
                    completed = false,
                    completedAtEpochMs = null
                )
            }
        }

        return sessionDao.insertSessionWithSets(
            session = WorkoutSessionEntity(
                planId = template.planId,
                workoutTemplateId = template.id,
                workoutNameSnapshot = template.name,
                startedAtEpochMs = startedAtEpochMs,
                finishedAtEpochMs = null
            ),
            sets = setsToInsert
        )
    }

    override suspend fun updateSet(set: SessionSetEntity) {
        sessionDao.updateSet(set)
    }

    override suspend fun updateSetWeight(setId: Long, weightKg: Double) {
        sessionDao.updateSetWeight(setId, weightKg)
    }

    override suspend fun updateSetReps(setId: Long, reps: Int) {
        sessionDao.updateSetReps(setId, reps)
    }

    override suspend fun updateSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAtEpochMs: Long?
    ) {
        sessionDao.updateSetCompleted(setId, completed, completedAtEpochMs)
    }

    override suspend fun addExtraSet(sessionId: Long, exerciseOrderIndex: Int): Long {
        val currentSets = sessionDao.getSetsForSession(sessionId)
            .filter { it.exerciseOrderIndex == exerciseOrderIndex }
        val templateSet = currentSets.lastOrNull()
            ?: throw IllegalArgumentException("Exercício não encontrado na sessão.")
        val nextSetNumber = (currentSets.maxOfOrNull { it.setNumber } ?: 0) + 1
        return sessionDao.insertSet(
            SessionSetEntity(
                sessionId = sessionId,
                exerciseId = templateSet.exerciseId,
                exerciseNameSnapshot = templateSet.exerciseNameSnapshot,
                equipmentNameSnapshot = templateSet.equipmentNameSnapshot,
                exerciseOrderIndex = exerciseOrderIndex,
                setNumber = nextSetNumber,
                weightKg = templateSet.weightKg,
                reps = templateSet.reps,
                completed = false,
                completedAtEpochMs = null
            )
        )
    }

    override suspend fun removeSet(setId: Long) {
        sessionDao.deleteSetById(setId)
    }

    override suspend fun finishSession(sessionId: Long, finishedAtEpochMs: Long) {
        val current = sessionDao.getSessionById(sessionId) ?: return
        sessionDao.updateSession(current.copy(finishedAtEpochMs = finishedAtEpochMs))
    }

    override suspend fun discardSession(sessionId: Long) {
        sessionDao.deleteSessionById(sessionId)
    }
}
