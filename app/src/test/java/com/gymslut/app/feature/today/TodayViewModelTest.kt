package com.gymslut.app.feature.today

import com.gymslut.app.core.time.CurrentDateProvider
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.model.ExerciseHistoryRow
import com.gymslut.app.data.model.SessionWithSets
import com.gymslut.app.data.model.WorkoutExerciseDetail
import com.gymslut.app.data.repository.PlanRepository
import com.gymslut.app.data.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: FakeTodayPlanRepository
    private lateinit var sessionRepo: FakeTodaySessionRepository
    private var fixedDate: LocalDate = LocalDate.of(2026, 11, 25) // Quarta-feira (dayOfWeek = 3)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = FakeTodayPlanRepository()
        sessionRepo = FakeTodaySessionRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noActivePlan_showsNullActivePlanAndEmptySchedule() = runTest(testDispatcher) {
        val vm = TodayViewModel(planRepo, sessionRepo, CurrentDateProvider { fixedDate })
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertNull(vm.uiState.value.activePlan)
        assertNull(vm.uiState.value.scheduledTemplate)
        job.cancel()
    }

    @Test
    fun scheduledWorkoutRestDayAndDiscardAndStart_workAsExpected() = runTest(testDispatcher) {
        val planId = planRepo.createPlan("ABC")
        val treinoA = planRepo.createWorkoutTemplate(planId, "Treino A")
        val treinoB = planRepo.createWorkoutTemplate(planId, "Treino B")
        planRepo.addExerciseToWorkout(treinoA, 1L, 3, 10, 60)
        planRepo.setScheduleDay(planId, 3, treinoA) // Quarta = Treino A
        planRepo.setScheduleDay(planId, 4, null) // Quinta = Descanso

        // Quarta-feira
        val vmWednesday = TodayViewModel(planRepo, sessionRepo, CurrentDateProvider { fixedDate })
        val job1 = launch { vmWednesday.uiState.collect() }
        advanceUntilIdle()

        assertEquals(3, vmWednesday.uiState.value.todayDayOfWeek)
        assertEquals("Treino A", vmWednesday.uiState.value.scheduledTemplate?.name)
        assertEquals(1, vmWednesday.uiState.value.scheduledExercises.size)
        assertEquals(2, vmWednesday.uiState.value.allTemplates.size)

        var startedSessionId: Long? = null
        vmWednesday.startWorkout(treinoB) { startedSessionId = it }
        advanceUntilIdle()
        assertNotNull(startedSessionId)
        assertNotNull(vmWednesday.uiState.value.inProgressSession)

        // Descartar sessão existente e iniciar outra
        var replacedSessionId: Long? = null
        vmWednesday.discardAndStartWorkout(startedSessionId!!, treinoA) { replacedSessionId = it }
        advanceUntilIdle()
        assertNotNull(replacedSessionId)
        assertEquals(replacedSessionId, vmWednesday.uiState.value.inProgressSession?.id)
        job1.cancel()

        // Quinta-feira (Descanso)
        fixedDate = LocalDate.of(2026, 11, 26)
        val vmThursday = TodayViewModel(planRepo, sessionRepo, CurrentDateProvider { fixedDate })
        val job2 = launch { vmThursday.uiState.collect() }
        advanceUntilIdle()

        assertEquals(4, vmThursday.uiState.value.todayDayOfWeek)
        assertNull(vmThursday.uiState.value.scheduledTemplate)
        assertEquals(2, vmThursday.uiState.value.allTemplates.size)

        vmThursday.discardInProgressSession(replacedSessionId!!)
        advanceUntilIdle()
        assertNull(vmThursday.uiState.value.inProgressSession)
        job2.cancel()
    }

    private class FakeTodayPlanRepository : PlanRepository {
        private val plans = MutableStateFlow<List<TrainingPlanEntity>>(emptyList())
        private val templates = MutableStateFlow<List<WorkoutTemplateEntity>>(emptyList())
        private val schedule = MutableStateFlow<List<WeeklyScheduleEntity>>(emptyList())
        private val exercises = MutableStateFlow<List<WorkoutExerciseDetail>>(emptyList())
        private var nextId = 1L

        override fun observeAllPlans(): Flow<List<TrainingPlanEntity>> = plans
        override fun observeActivePlan(): Flow<TrainingPlanEntity?> =
            plans.map { list -> list.firstOrNull { it.isActive } }

        override fun observeTemplatesForPlan(planId: Long): Flow<List<WorkoutTemplateEntity>> =
            templates.map { list -> list.filter { it.planId == planId } }

        override fun observeWorkoutExercises(workoutTemplateId: Long): Flow<List<WorkoutExerciseDetail>> =
            exercises.map { list -> list.filter { it.workoutTemplateId == workoutTemplateId } }

        override fun observeScheduleForPlan(planId: Long): Flow<List<WeeklyScheduleEntity>> =
            schedule.map { list -> list.filter { it.planId == planId } }

        override suspend fun getPlanById(planId: Long) = plans.value.firstOrNull { it.id == planId }
        override suspend fun getTemplateById(templateId: Long) = templates.value.firstOrNull { it.id == templateId }

        override suspend fun createPlan(name: String, createdAtEpochMs: Long): Long {
            val id = nextId++
            plans.value = plans.value + TrainingPlanEntity(id, name, plans.value.isEmpty(), createdAtEpochMs)
            return id
        }

        override suspend fun renamePlan(planId: Long, newName: String) = Unit
        override suspend fun deletePlan(planId: Long) = Unit
        override suspend fun setActivePlan(planId: Long) = Unit

        override suspend fun createWorkoutTemplate(planId: Long, name: String): Long {
            val id = nextId++
            templates.value = templates.value + WorkoutTemplateEntity(id, planId, name, 0)
            return id
        }

        override suspend fun renameWorkoutTemplate(templateId: Long, newName: String) = Unit
        override suspend fun deleteWorkoutTemplate(templateId: Long) = Unit
        override suspend fun moveWorkoutTemplate(planId: Long, templateId: Long, moveUp: Boolean) = Unit

        override suspend fun addExerciseToWorkout(
            workoutTemplateId: Long,
            exerciseId: Long,
            targetSets: Int,
            targetReps: Int,
            restSeconds: Int
        ): Long {
            val id = nextId++
            exercises.value = exercises.value + WorkoutExerciseDetail(
                id, workoutTemplateId, exerciseId, 0, targetSets, targetReps, restSeconds, "Supino", "Barra"
            )
            return id
        }

        override suspend fun updateWorkoutExercise(id: Long, targetSets: Int, targetReps: Int, restSeconds: Int) = Unit
        override suspend fun removeWorkoutExercise(id: Long) = Unit
        override suspend fun moveWorkoutExercise(workoutTemplateId: Long, workoutExerciseId: Long, moveUp: Boolean) = Unit

        override suspend fun setScheduleDay(planId: Long, dayOfWeek: Int, workoutTemplateId: Long?) {
            schedule.value = schedule.value.filterNot { it.planId == planId && it.dayOfWeek == dayOfWeek } +
                WeeklyScheduleEntity(planId, dayOfWeek, workoutTemplateId)
        }
    }

    private class FakeTodaySessionRepository : SessionRepository {
        private val inProgress = MutableStateFlow<WorkoutSessionEntity?>(null)
        private var nextSessionId = 1L

        override fun observeInProgressSession(): Flow<WorkoutSessionEntity?> = inProgress
        override fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>> = flowOf(emptyList())
        override fun observeCompletedSessionsWithSets(): Flow<List<SessionWithSets>> = flowOf(emptyList())
        override fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryRow>> = flowOf(emptyList())
        override fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> = inProgress
        override fun observeSetsForSession(sessionId: Long): Flow<List<SessionSetEntity>> = flowOf(emptyList())

        override suspend fun startWorkoutSession(workoutTemplateId: Long, startedAtEpochMs: Long): Long {
            val id = nextSessionId++
            inProgress.value = WorkoutSessionEntity(id, 1L, workoutTemplateId, "Treino", startedAtEpochMs, null)
            return id
        }

        override suspend fun updateSet(set: SessionSetEntity) = Unit
        override suspend fun addExtraSet(sessionId: Long, exerciseOrderIndex: Int) = 0L
        override suspend fun removeSet(setId: Long) = Unit
        override suspend fun finishSession(sessionId: Long, finishedAtEpochMs: Long) {
            inProgress.value = null
        }

        override suspend fun discardSession(sessionId: Long) {
            if (inProgress.value?.id == sessionId) {
                inProgress.value = null
            }
        }
    }
}
