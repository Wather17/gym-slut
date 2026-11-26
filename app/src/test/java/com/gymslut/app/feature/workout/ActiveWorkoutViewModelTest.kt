package com.gymslut.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionRepo: FakeWorkoutSessionRepository
    private lateinit var planRepo: FakeWorkoutPlanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepo = FakeWorkoutSessionRepository()
        planRepo = FakeWorkoutPlanRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun editWeightRepsToggleAddRemoveAndFinish_updatesSessionCorrectly() = runTest(testDispatcher) {
        val vm = ActiveWorkoutViewModel(SavedStateHandle(mapOf("sessionId" to 1L)), sessionRepo, planRepo)
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.exerciseGroups.size)
        val group = vm.uiState.value.exerciseGroups.first()
        assertEquals(90, group.restSeconds)
        assertEquals(2, group.sets.size)

        val firstSet = group.sets.first()
        vm.updateSetWeight(firstSet, "82,5")
        advanceUntilIdle()
        assertEquals(82.5, vm.uiState.value.exerciseGroups.first().sets.first().weightKg, 0.001)

        vm.updateSetReps(vm.uiState.value.exerciseGroups.first().sets.first(), "12")
        advanceUntilIdle()
        assertEquals(12, vm.uiState.value.exerciseGroups.first().sets.first().reps)

        vm.toggleSetCompleted(vm.uiState.value.exerciseGroups.first().sets.first(), group.restSeconds)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.completedSetsCount)

        vm.addExtraSet(0)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.totalSetsCount)

        val extraSetId = vm.uiState.value.exerciseGroups.first().sets.last().id
        vm.removeSet(extraSetId)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.totalSetsCount)

        var finished = false
        vm.finishWorkout { finished = true }
        advanceUntilIdle()
        assertTrue(finished)
        assertNotNull(sessionRepo.sessionFlow.value?.finishedAtEpochMs)
        job.cancel()
    }

    @Test
    fun discardWorkout_removesSession() = runTest(testDispatcher) {
        val vm = ActiveWorkoutViewModel(SavedStateHandle(mapOf("sessionId" to 1L)), sessionRepo, planRepo)
        var discarded = false
        vm.discardWorkout { discarded = true }
        advanceUntilIdle()
        assertTrue(discarded)
        assertNull(sessionRepo.sessionFlow.value)
    }

    private class FakeWorkoutSessionRepository : SessionRepository {
        val sessionFlow = MutableStateFlow<WorkoutSessionEntity?>(
            WorkoutSessionEntity(1L, 1L, 10L, "Treino A", 1000L, null)
        )
        private val setsFlow = MutableStateFlow(
            listOf(
                SessionSetEntity(1L, 1L, 5L, "Supino Reto", "Supino Reto", 0, 1, 80.0, 10),
                SessionSetEntity(2L, 1L, 5L, "Supino Reto", "Supino Reto", 0, 2, 80.0, 10)
            )
        )
        private var nextId = 10L

        override fun observeInProgressSession(): Flow<WorkoutSessionEntity?> = sessionFlow
        override fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>> = flowOf(emptyList())
        override fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> = sessionFlow
        override fun observeSetsForSession(sessionId: Long): Flow<List<SessionSetEntity>> = setsFlow

        override suspend fun startWorkoutSession(workoutTemplateId: Long, startedAtEpochMs: Long) = 1L

        override suspend fun updateSet(set: SessionSetEntity) {
            setsFlow.value = setsFlow.value.map { if (it.id == set.id) set else it }
        }

        override suspend fun addExtraSet(sessionId: Long, exerciseOrderIndex: Int): Long {
            val last = setsFlow.value.last { it.exerciseOrderIndex == exerciseOrderIndex }
            val id = nextId++
            setsFlow.value = setsFlow.value + last.copy(id = id, setNumber = last.setNumber + 1, completed = false)
            return id
        }

        override suspend fun removeSet(setId: Long) {
            setsFlow.value = setsFlow.value.filterNot { it.id == setId }
        }

        override suspend fun finishSession(sessionId: Long, finishedAtEpochMs: Long) {
            sessionFlow.value = sessionFlow.value?.copy(finishedAtEpochMs = finishedAtEpochMs)
        }

        override suspend fun discardSession(sessionId: Long) {
            sessionFlow.value = null
            setsFlow.value = emptyList()
        }
    }

    private class FakeWorkoutPlanRepository : PlanRepository {
        override fun observeAllPlans(): Flow<List<TrainingPlanEntity>> = flowOf(emptyList())
        override fun observeActivePlan(): Flow<TrainingPlanEntity?> = flowOf(null)
        override fun observeTemplatesForPlan(planId: Long): Flow<List<WorkoutTemplateEntity>> = flowOf(emptyList())
        override fun observeWorkoutExercises(workoutTemplateId: Long): Flow<List<WorkoutExerciseDetail>> =
            flowOf(
                listOf(
                    WorkoutExerciseDetail(1L, workoutTemplateId, 5L, 0, 2, 10, 90, "Supino Reto", "Supino Reto")
                )
            )

        override fun observeScheduleForPlan(planId: Long): Flow<List<WeeklyScheduleEntity>> = flowOf(emptyList())
        override suspend fun getPlanById(planId: Long) = null
        override suspend fun getTemplateById(templateId: Long) = null
        override suspend fun createPlan(name: String, createdAtEpochMs: Long) = 0L
        override suspend fun renamePlan(planId: Long, newName: String) = Unit
        override suspend fun deletePlan(planId: Long) = Unit
        override suspend fun setActivePlan(planId: Long) = Unit
        override suspend fun createWorkoutTemplate(planId: Long, name: String) = 0L
        override suspend fun renameWorkoutTemplate(templateId: Long, newName: String) = Unit
        override suspend fun deleteWorkoutTemplate(templateId: Long) = Unit
        override suspend fun moveWorkoutTemplate(planId: Long, templateId: Long, moveUp: Boolean) = Unit
        override suspend fun addExerciseToWorkout(workoutTemplateId: Long, exerciseId: Long, targetSets: Int, targetReps: Int, restSeconds: Int) = 0L
        override suspend fun updateWorkoutExercise(id: Long, targetSets: Int, targetReps: Int, restSeconds: Int) = Unit
        override suspend fun removeWorkoutExercise(id: Long) = Unit
        override suspend fun moveWorkoutExercise(workoutTemplateId: Long, workoutExerciseId: Long, moveUp: Boolean) = Unit
        override suspend fun setScheduleDay(planId: Long, dayOfWeek: Int, workoutTemplateId: Long?) = Unit
    }
}
