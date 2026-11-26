package com.gymslut.app.feature.workout

import androidx.lifecycle.SavedStateHandle
import com.gymslut.app.core.notification.RestAlertNotifier
import com.gymslut.app.core.preferences.PreferencesRepository
import com.gymslut.app.core.preferences.ThemeMode
import com.gymslut.app.core.preferences.WorkoutPreferences
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private lateinit var prefsRepo: FakeWorkoutPreferencesRepository
    private lateinit var notifier: FakeRestNotifier
    private var nowMs = 1_000L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepo = FakeWorkoutSessionRepository()
        planRepo = FakeWorkoutPlanRepository()
        prefsRepo = FakeWorkoutPreferencesRepository()
        notifier = FakeRestNotifier()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun parseWeight_handlesValidInvalidAndDecimalCommaInputs() {
        assertEquals(80.0, ActiveWorkoutViewModel.parseWeight("80")!!, 0.001)
        assertEquals(82.5, ActiveWorkoutViewModel.parseWeight("82,5")!!, 0.001)
        assertEquals(82.5, ActiveWorkoutViewModel.parseWeight(" 82.5 ")!!, 0.001)
        assertEquals(0.0, ActiveWorkoutViewModel.parseWeight("0")!!, 0.001)
        assertNull(ActiveWorkoutViewModel.parseWeight(""))
        assertNull(ActiveWorkoutViewModel.parseWeight("abc"))
        assertNull(ActiveWorkoutViewModel.parseWeight("-1"))
    }

    @Test
    fun editWeightRepsTimerControlsAddRemoveAndFinish_workAsExpected() = runTest(testDispatcher) {
        val vm = ActiveWorkoutViewModel(
            SavedStateHandle(mapOf("sessionId" to 1L)),
            sessionRepo,
            planRepo,
            prefsRepo,
            ElapsedTimeProvider { nowMs },
            notifier
        )
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.exerciseGroups.size)
        val group = vm.uiState.value.exerciseGroups.first()
        assertEquals(90, group.restSeconds)
        assertEquals(2, group.sets.size)

        // Entradas inválidas são ignoradas sem alterar valores existentes
        val firstSet = group.sets.first()
        vm.updateSetWeight(firstSet, "-5")
        vm.updateSetWeight(firstSet, "abc")
        vm.updateSetReps(firstSet, "-2")
        vm.updateSetReps(firstSet, "xyz")
        runCurrent()
        assertEquals(80.0, vm.uiState.value.exerciseGroups.first().sets.first().weightKg, 0.001)
        assertEquals(10, vm.uiState.value.exerciseGroups.first().sets.first().reps)

        vm.updateSetWeight(firstSet, "82,5")
        runCurrent()
        assertEquals(82.5, vm.uiState.value.exerciseGroups.first().sets.first().weightKg, 0.001)

        vm.updateSetReps(vm.uiState.value.exerciseGroups.first().sets.first(), "12")
        runCurrent()
        assertEquals(12, vm.uiState.value.exerciseGroups.first().sets.first().reps)

        // restSeconds == 0 não inicia timer
        vm.toggleSetCompleted(vm.uiState.value.exerciseGroups.first().sets[1], 0)
        runCurrent()
        assertFalse(vm.timerState.value.isActive)

        // Desmarcar série não inicia timer
        vm.toggleSetCompleted(vm.uiState.value.exerciseGroups.first().sets[1], 90)
        runCurrent()
        assertFalse(vm.timerState.value.isActive)

        // Marcar série com restSeconds > 0 inicia timer e suporta +15s, pausa, retomada, notificação e skip
        vm.toggleSetCompleted(vm.uiState.value.exerciseGroups.first().sets.first(), group.restSeconds)
        runCurrent()
        assertEquals(1, vm.uiState.value.completedSetsCount)
        assertTrue(vm.timerState.value.isActive)
        assertEquals(90, vm.timerState.value.remainingSeconds)

        vm.addTimerSeconds(15)
        runCurrent()
        assertEquals(105, vm.timerState.value.remainingSeconds)

        vm.toggleTimerPause()
        runCurrent()
        assertTrue(vm.timerState.value.isPaused)

        vm.toggleTimerPause()
        runCurrent()
        assertFalse(vm.timerState.value.isPaused)

        vm.skipTimer()
        runCurrent()
        assertFalse(vm.timerState.value.isActive)

        // Deixar o timer expirar naturalmente dispara notificação
        vm.toggleSetCompleted(vm.uiState.value.exerciseGroups.first().sets[1], 2)
        runCurrent()
        nowMs += 2_000L
        advanceTimeBy(2_500L)
        runCurrent()
        assertEquals(listOf("Supino Reto"), notifier.notified)

        vm.addExtraSet(0)
        runCurrent()
        assertEquals(3, vm.uiState.value.totalSetsCount)

        val extraSetId = vm.uiState.value.exerciseGroups.first().sets.last().id
        vm.removeSet(extraSetId)
        runCurrent()
        assertEquals(2, vm.uiState.value.totalSetsCount)

        var finished = false
        vm.finishWorkout { finished = true }
        advanceUntilIdle()
        assertTrue(finished)
        assertFalse(vm.timerState.value.isActive)
        assertNotNull(sessionRepo.sessionFlow.value?.finishedAtEpochMs)
        job.cancel()
    }

    @Test
    fun sessionWithoutTemplate_usesDefaultRestSecondsAndDiscardCancelsTimer() = runTest(testDispatcher) {
        sessionRepo.sessionFlow.value = WorkoutSessionEntity(1L, null, null, "Treino Antigo", 1000L, null)
        val vm = ActiveWorkoutViewModel(
            SavedStateHandle(mapOf("sessionId" to 1L)),
            sessionRepo,
            planRepo,
            prefsRepo,
            ElapsedTimeProvider { nowMs },
            notifier
        )
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        val group = vm.uiState.value.exerciseGroups.first()
        assertEquals(ActiveWorkoutViewModel.DEFAULT_REST_SECONDS, group.restSeconds)

        vm.toggleSetCompleted(group.sets.first(), group.restSeconds)
        runCurrent()
        assertTrue(vm.timerState.value.isActive)

        var discarded = false
        vm.discardWorkout { discarded = true }
        advanceUntilIdle()
        assertTrue(discarded)
        assertFalse(vm.timerState.value.isActive)
        assertNull(sessionRepo.sessionFlow.value)
        job.cancel()
    }

    @Test
    fun concurrentStaleSetUpdatesAndTemplateReorder_doNotOverwriteFieldsOrMismatchRest() = runTest(testDispatcher) {
        // Template tem outro exercício (id=99, rest=180s) na posição 0 e o exercício da sessão (id=5, rest=75s) na posição 1
        planRepo.exercises = listOf(
            WorkoutExerciseDetail(1L, 10L, 99L, 0, 3, 10, 180, "Outro", "Máquina"),
            WorkoutExerciseDetail(2L, 10L, 5L, 1, 2, 10, 75, "Supino Reto", "Supino Reto")
        )
        val vm = ActiveWorkoutViewModel(
            SavedStateHandle(mapOf("sessionId" to 1L)),
            sessionRepo,
            planRepo,
            prefsRepo,
            ElapsedTimeProvider { nowMs },
            notifier
        )
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        val group = vm.uiState.value.exerciseGroups.first()
        // Deve usar 75s do exercício id=5, nunca 180s do exercício id=99 que ocupou a posição 0
        assertEquals(75, group.restSeconds)

        // Passa o MESMO snapshot obsoleto para peso, reps e conclusão antes do Flow re-emitir
        val staleSet = group.sets.first()
        vm.updateSetWeight(staleSet, "95,5")
        vm.updateSetReps(staleSet, "15")
        vm.toggleSetCompleted(staleSet, group.restSeconds)
        runCurrent()

        val updated = vm.uiState.value.exerciseGroups.first().sets.first()
        assertEquals(95.5, updated.weightKg, 0.001)
        assertEquals(15, updated.reps)
        assertTrue(updated.completed)
        assertNotNull(updated.completedAtEpochMs)

        // Duplo clique em finishWorkout executa apenas uma vez enquanto em andamento
        var finishCallbacks = 0
        vm.finishWorkout { finishCallbacks++ }
        vm.finishWorkout { finishCallbacks++ }
        advanceUntilIdle()
        assertEquals(1, finishCallbacks)

        job.cancel()
    }

    @Test
    fun workoutPreferencesDisabled_skipsAutoStartAndDisablesRestVibration() = runTest(testDispatcher) {
        prefsRepo.setAutoStartRestTimer(false)
        prefsRepo.setRestTimerVibrationEnabled(false)
        val vm = ActiveWorkoutViewModel(
            SavedStateHandle(mapOf("sessionId" to 1L)),
            sessionRepo,
            planRepo,
            prefsRepo,
            ElapsedTimeProvider { nowMs },
            notifier
        )
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        val firstSet = vm.uiState.value.exerciseGroups.first().sets.first()
        vm.toggleSetCompleted(firstSet, 90)
        runCurrent()
        assertTrue(vm.uiState.value.exerciseGroups.first().sets.first().completed)
        assertFalse(vm.timerState.value.isActive)

        // Reabilita autoStart com vibração desligada -> alerta chega com vibrate=false
        prefsRepo.setAutoStartRestTimer(true)
        vm.toggleSetCompleted(vm.uiState.value.exerciseGroups.first().sets[1], 2)
        runCurrent()
        assertTrue(vm.timerState.value.isActive)
        nowMs += 2_000L
        advanceTimeBy(2_500L)
        runCurrent()
        assertEquals(listOf("Supino Reto" to false), notifier.notifiedWithVibrate)
        job.cancel()
    }

    private class FakeRestNotifier : RestAlertNotifier {
        val notified = mutableListOf<String>()
        val notifiedWithVibrate = mutableListOf<Pair<String, Boolean>>()
        override fun notifyRestFinished(exerciseName: String, vibrate: Boolean) {
            notified += exerciseName
            notifiedWithVibrate += exerciseName to vibrate
        }

        override fun cancelNotification() = Unit
    }

    private class FakeWorkoutPreferencesRepository : PreferencesRepository {
        private val workoutFlow = MutableStateFlow(WorkoutPreferences())
        override val themeMode: Flow<ThemeMode> = flowOf(ThemeMode.DARK)
        override val workoutPreferences: Flow<WorkoutPreferences> = workoutFlow
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setKeepScreenOn(enabled: Boolean) {
            workoutFlow.value = workoutFlow.value.copy(keepScreenOn = enabled)
        }

        override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
            workoutFlow.value = workoutFlow.value.copy(hapticFeedbackEnabled = enabled)
        }

        override suspend fun setAutoStartRestTimer(enabled: Boolean) {
            workoutFlow.value = workoutFlow.value.copy(autoStartRestTimer = enabled)
        }

        override suspend fun setRestTimerVibrationEnabled(enabled: Boolean) {
            workoutFlow.value = workoutFlow.value.copy(restTimerVibrationEnabled = enabled)
        }
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
        override fun observeCompletedSessionsWithSets(): Flow<List<SessionWithSets>> = flowOf(emptyList())
        override fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryRow>> = flowOf(emptyList())
        override fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> = sessionFlow
        override fun observeSetsForSession(sessionId: Long): Flow<List<SessionSetEntity>> = setsFlow

        override suspend fun startWorkoutSession(workoutTemplateId: Long, startedAtEpochMs: Long) = 1L

        override suspend fun updateSet(set: SessionSetEntity) {
            setsFlow.value = setsFlow.value.map { if (it.id == set.id) set else it }
        }

        override suspend fun updateSetWeight(setId: Long, weightKg: Double) {
            setsFlow.value = setsFlow.value.map { if (it.id == setId) it.copy(weightKg = weightKg) else it }
        }

        override suspend fun updateSetReps(setId: Long, reps: Int) {
            setsFlow.value = setsFlow.value.map { if (it.id == setId) it.copy(reps = reps) else it }
        }

        override suspend fun updateSetCompleted(
            setId: Long,
            completed: Boolean,
            completedAtEpochMs: Long?
        ) {
            setsFlow.value = setsFlow.value.map {
                if (it.id == setId) it.copy(completed = completed, completedAtEpochMs = completedAtEpochMs) else it
            }
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
        var exercises: List<WorkoutExerciseDetail> = listOf(
            WorkoutExerciseDetail(1L, 10L, 5L, 0, 2, 10, 90, "Supino Reto", "Supino Reto")
        )

        override fun observeAllPlans(): Flow<List<TrainingPlanEntity>> = flowOf(emptyList())
        override fun observeActivePlan(): Flow<TrainingPlanEntity?> = flowOf(null)
        override fun observeTemplatesForPlan(planId: Long): Flow<List<WorkoutTemplateEntity>> = flowOf(emptyList())
        override fun observeWorkoutExercises(workoutTemplateId: Long): Flow<List<WorkoutExerciseDetail>> =
            flowOf(exercises)

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
