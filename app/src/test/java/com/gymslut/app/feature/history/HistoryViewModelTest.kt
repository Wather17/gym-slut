package com.gymslut.app.feature.history

import androidx.lifecycle.SavedStateHandle
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.model.ExerciseHistoryRow
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.model.SessionWithSets
import com.gymslut.app.data.repository.CatalogRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionRepo: FakeHistorySessionRepository
    private lateinit var catalogRepo: FakeHistoryCatalogRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepo = FakeHistorySessionRepository()
        catalogRepo = FakeHistoryCatalogRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun formatters_calculateDurationAndVolumeCorrectly() {
        assertEquals("45 min", HistoryFormatters.formatDuration(0L, 45 * 60_000L))
        assertEquals("1h 15min", HistoryFormatters.formatDuration(0L, 75 * 60_000L))
        assertEquals("< 1 min", HistoryFormatters.formatDuration(0L, 15_000L))

        val sets = listOf(
            SessionSetEntity(1L, 1L, 10L, "Supino", "Barra", 0, 1, 80.0, 10, completed = true),
            SessionSetEntity(2L, 1L, 10L, "Supino", "Barra", 0, 2, 80.0, 8, completed = false)
        )
        assertEquals(800.0, HistoryFormatters.calculateTotalVolumeKg(sets), 0.001)
    }

    @Test
    fun historyAndSessionDetailAndExerciseHistory_workEndToEnd() = runTest(testDispatcher) {
        val historyVm = HistoryViewModel(sessionRepo, catalogRepo)
        val job1 = launch { historyVm.uiState.collect() }
        advanceUntilIdle()

        assertEquals(2, historyVm.uiState.value.sessions.size)
        assertEquals(20L, historyVm.uiState.value.sessions.first().sessionId)
        assertEquals(1000.0, historyVm.uiState.value.sessions.first().totalVolumeKg, 0.001)

        val detailVm = SessionDetailViewModel(SavedStateHandle(mapOf("sessionId" to 20L)), sessionRepo)
        val job2 = launch { detailVm.uiState.collect() }
        advanceUntilIdle()

        assertEquals("Treino B", detailVm.uiState.value.session?.workoutNameSnapshot)
        assertEquals(1, detailVm.uiState.value.groups.size)

        val exVm = ExerciseHistoryViewModel(SavedStateHandle(mapOf("exerciseId" to 5L)), sessionRepo)
        val job3 = launch { exVm.uiState.collect() }
        advanceUntilIdle()

        assertEquals("Supino Reto", exVm.uiState.value.exerciseName)
        assertEquals(2, exVm.uiState.value.entries.size)

        var deleted = false
        detailVm.deleteSession { deleted = true }
        advanceUntilIdle()
        assertTrue(deleted)
        assertEquals(1, historyVm.uiState.value.sessions.size)

        job1.cancel()
        job2.cancel()
        job3.cancel()
    }

    private class FakeHistorySessionRepository : SessionRepository {
        private val sessionsFlow = MutableStateFlow(
            listOf(
                SessionWithSets(
                    session = WorkoutSessionEntity(20L, 1L, 2L, "Treino B", 2_000_000L, 2_000_000L + 30 * 60_000L),
                    sets = listOf(
                        SessionSetEntity(201L, 20L, 5L, "Supino Reto", "Barra", 0, 1, 100.0, 10, completed = true)
                    )
                ),
                SessionWithSets(
                    session = WorkoutSessionEntity(10L, 1L, 1L, "Treino A", 1_000_000L, 1_000_000L + 40 * 60_000L),
                    sets = listOf(
                        SessionSetEntity(101L, 10L, 5L, "Supino Reto", "Barra", 0, 1, 80.0, 10, completed = true)
                    )
                )
            )
        )

        override fun observeInProgressSession(): Flow<WorkoutSessionEntity?> = flowOf(null)
        override fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>> =
            sessionsFlow.map { list -> list.map { it.session } }

        override fun observeCompletedSessionsWithSets(): Flow<List<SessionWithSets>> = sessionsFlow

        override fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryRow>> =
            sessionsFlow.map { list ->
                list.flatMap { sws ->
                    sws.sets.filter { it.exerciseId == exerciseId }.map { set ->
                        ExerciseHistoryRow(
                            sessionId = sws.session.id,
                            workoutNameSnapshot = sws.session.workoutNameSnapshot,
                            startedAtEpochMs = sws.session.startedAtEpochMs,
                            finishedAtEpochMs = sws.session.finishedAtEpochMs ?: 0L,
                            exerciseNameSnapshot = set.exerciseNameSnapshot,
                            equipmentNameSnapshot = set.equipmentNameSnapshot,
                            setNumber = set.setNumber,
                            weightKg = set.weightKg,
                            reps = set.reps,
                            completed = set.completed
                        )
                    }
                }
            }

        override fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> =
            sessionsFlow.map { list -> list.firstOrNull { it.session.id == sessionId }?.session }

        override fun observeSetsForSession(sessionId: Long): Flow<List<SessionSetEntity>> =
            sessionsFlow.map { list -> list.firstOrNull { it.session.id == sessionId }?.sets.orEmpty() }

        override suspend fun startWorkoutSession(workoutTemplateId: Long, startedAtEpochMs: Long) = 0L
        override suspend fun updateSet(set: SessionSetEntity) = Unit
        override suspend fun addExtraSet(sessionId: Long, exerciseOrderIndex: Int) = 0L
        override suspend fun removeSet(setId: Long) = Unit
        override suspend fun finishSession(sessionId: Long, finishedAtEpochMs: Long) = Unit
        override suspend fun discardSession(sessionId: Long) {
            sessionsFlow.value = sessionsFlow.value.filterNot { it.session.id == sessionId }
        }
    }

    private class FakeHistoryCatalogRepository : CatalogRepository {
        override fun observeEquipment(): Flow<List<EquipmentEntity>> = flowOf(emptyList())
        override fun observeExercisesWithEquipment(): Flow<List<ExerciseWithEquipment>> = flowOf(emptyList())
        override suspend fun createEquipment(name: String, category: EquipmentCategory, notes: String) = 0L
        override suspend fun updateEquipment(id: Long, name: String, category: EquipmentCategory, notes: String) = Unit
        override suspend fun deleteEquipment(id: Long) = Unit
        override suspend fun createExercise(name: String, primaryMuscle: MuscleGroup, secondaryMuscle: MuscleGroup?, equipmentId: Long, notes: String) = 0L
        override suspend fun updateExercise(id: Long, name: String, primaryMuscle: MuscleGroup, secondaryMuscle: MuscleGroup?, equipmentId: Long, notes: String) = Unit
        override suspend fun deleteExercise(id: Long) = Unit
    }
}
