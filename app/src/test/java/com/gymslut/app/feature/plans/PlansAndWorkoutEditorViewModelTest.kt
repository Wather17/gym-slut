package com.gymslut.app.feature.plans

import androidx.lifecycle.SavedStateHandle
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.model.WorkoutExerciseDetail
import com.gymslut.app.data.repository.CatalogRepository
import com.gymslut.app.data.repository.PlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlansAndWorkoutEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: FakePlanRepository
    private lateinit var catalogRepo: FakeCatalogRepoForPlans

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = FakePlanRepository()
        catalogRepo = FakeCatalogRepoForPlans()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun plansViewModel_createsRenamesValidatesActivatesAndDeletesPlans() = runTest(testDispatcher) {
        val vm = PlansViewModel(planRepo)
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        var ok = false
        vm.savePlan(null, "   ") { ok = true }
        advanceUntilIdle()
        assertFalse(ok)
        assertNotNull(vm.uiState.value.formError)
        vm.clearFormError()
        advanceUntilIdle()
        assertNull(vm.uiState.value.formError)

        vm.savePlan(null, "ABC") { ok = true }
        advanceUntilIdle()
        assertTrue(ok)
        assertNull(vm.uiState.value.formError)
        vm.clearFeedbackMessage()
        advanceUntilIdle()
        assertNull(vm.uiState.value.feedbackMessage)

        val firstPlan = vm.uiState.value.plans.first()
        var renamed = false
        vm.savePlan(firstPlan.id, "ABC Atualizado") { renamed = true }
        advanceUntilIdle()
        assertTrue(renamed)
        assertEquals("ABC Atualizado", vm.uiState.value.plans.first().name)

        vm.savePlan(null, "Upper/Lower") {}
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.plans.size)

        val second = vm.uiState.value.plans.last()
        vm.activatePlan(second)
        advanceUntilIdle()
        assertEquals(second.id, vm.uiState.value.plans.first { it.isActive }.id)

        vm.deletePlan(second)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.plans.size)
        job.cancel()
    }

    @Test
    fun planDetailViewModel_createsRenamesReordersSchedulesActivatesAndDeletes() = runTest(testDispatcher) {
        val p1 = planRepo.createPlan("Push Pull")
        val p2 = planRepo.createPlan("Full Body")
        val vm = PlanDetailViewModel(SavedStateHandle(mapOf("planId" to p2)), planRepo)
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        var invalidSaved = false
        vm.saveWorkoutTemplate(null, "   ") { invalidSaved = true }
        advanceUntilIdle()
        assertFalse(invalidSaved)
        assertNotNull(vm.uiState.value.formError)
        vm.clearFormError()
        advanceUntilIdle()
        assertNull(vm.uiState.value.formError)

        vm.activatePlan()
        advanceUntilIdle()
        assertEquals(p2, planRepo.observeActivePlan().first()?.id)
        assertTrue(vm.uiState.value.feedbackMessage!!.contains("ativo"))
        vm.clearFeedbackMessage()
        advanceUntilIdle()
        assertNull(vm.uiState.value.feedbackMessage)

        vm.saveWorkoutTemplate(null, "Treino A") {}
        vm.saveWorkoutTemplate(null, "Treino B") {}
        advanceUntilIdle()
        assertEquals(listOf("Treino A", "Treino B"), vm.uiState.value.templates.map { it.name })

        val firstTemplate = vm.uiState.value.templates.first()
        vm.saveWorkoutTemplate(firstTemplate.id, "Treino A+") {}
        advanceUntilIdle()
        assertEquals("Treino A+", vm.uiState.value.templates.first().name)

        vm.moveWorkoutTemplate(vm.uiState.value.templates[1], moveUp = true)
        advanceUntilIdle()
        assertEquals(listOf("Treino B", "Treino A+"), vm.uiState.value.templates.map { it.name })

        vm.setScheduleDay(1, firstTemplate.id)
        advanceUntilIdle()
        assertEquals(firstTemplate.id, vm.uiState.value.scheduleByDay[1])

        vm.deleteWorkoutTemplate(vm.uiState.value.templates.first())
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.templates.size)
        assertEquals(p1, 1L)
        job.cancel()
    }

    @Test
    fun workoutEditorViewModel_filtersValidatesUpdatesAndReordersExercises() = runTest(testDispatcher) {
        val planId = planRepo.createPlan("ABC")
        val templateId = planRepo.createWorkoutTemplate(planId, "Treino A")
        val vm = WorkoutEditorViewModel(
            SavedStateHandle(mapOf("workoutTemplateId" to templateId)),
            planRepo,
            catalogRepo
        )
        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        // Filtros do seletor de exercícios
        assertEquals(2, vm.uiState.value.catalogExercises.size)
        vm.updatePickerSearch("Supino")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.catalogExercises.size)
        vm.updatePickerSearch("")
        vm.selectPickerMuscle(MuscleGroup.BACK)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.catalogExercises.size)
        assertEquals("Puxada Frontal", vm.uiState.value.catalogExercises.first().exercise.name)
        vm.selectPickerMuscle(null)
        advanceUntilIdle()

        // Validações numéricas
        var added = false
        vm.addExercise(1L, "0", "10", "60") { added = true }
        advanceUntilIdle()
        assertFalse(added)
        assertNotNull(vm.uiState.value.formError)
        vm.clearFormError()
        advanceUntilIdle()
        assertNull(vm.uiState.value.formError)

        vm.addExercise(1L, "3", "0", "60") { added = true }
        advanceUntilIdle()
        assertFalse(added)

        vm.addExercise(1L, "3", "10", "-5") { added = true }
        advanceUntilIdle()
        assertFalse(added)

        vm.addExercise(1L, "abc", "10", "60") { added = true }
        advanceUntilIdle()
        assertFalse(added)

        vm.addExercise(1L, "4", "10", "90") { added = true }
        vm.addExercise(2L, "3", "12", "60") {}
        advanceUntilIdle()
        assertTrue(added)
        assertEquals(2, vm.uiState.value.workoutExercises.size)
        vm.clearFeedbackMessage()
        advanceUntilIdle()
        assertNull(vm.uiState.value.feedbackMessage)

        val firstId = vm.uiState.value.workoutExercises.first().id
        var updated = false
        vm.updatePrescription(firstId, "0", "12", "60") { updated = true }
        advanceUntilIdle()
        assertFalse(updated)
        vm.updatePrescription(firstId, "5", "8", "120") { updated = true }
        advanceUntilIdle()
        assertTrue(updated)
        assertEquals(5, vm.uiState.value.workoutExercises.first().targetSets)
        assertEquals(120, vm.uiState.value.workoutExercises.first().restSeconds)

        val secondId = vm.uiState.value.workoutExercises[1].id
        vm.moveExercise(secondId, moveUp = true)
        advanceUntilIdle()
        assertEquals(2L, vm.uiState.value.workoutExercises.first().exerciseId)

        vm.removeExercise(secondId)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.workoutExercises.size)
        job.cancel()
    }

    private class FakePlanRepository : PlanRepository {
        private val plansFlow = MutableStateFlow<List<TrainingPlanEntity>>(emptyList())
        private val templatesFlow = MutableStateFlow<List<WorkoutTemplateEntity>>(emptyList())
        private val scheduleFlow = MutableStateFlow<List<WeeklyScheduleEntity>>(emptyList())
        private val workoutExercisesFlow = MutableStateFlow<List<WorkoutExerciseDetail>>(emptyList())
        private var nextId = 1L

        override fun observeAllPlans(): Flow<List<TrainingPlanEntity>> = plansFlow
        override fun observeActivePlan(): Flow<TrainingPlanEntity?> =
            plansFlow.map { list -> list.firstOrNull { it.isActive } }

        override fun observeTemplatesForPlan(planId: Long): Flow<List<WorkoutTemplateEntity>> =
            templatesFlow.map { list -> list.filter { it.planId == planId }.sortedBy { it.orderIndex } }

        override fun observeWorkoutExercises(workoutTemplateId: Long): Flow<List<WorkoutExerciseDetail>> =
            workoutExercisesFlow.map { list ->
                list.filter { it.workoutTemplateId == workoutTemplateId }.sortedBy { it.orderIndex }
            }

        override fun observeScheduleForPlan(planId: Long): Flow<List<WeeklyScheduleEntity>> =
            scheduleFlow.map { list -> list.filter { it.planId == planId } }

        override suspend fun getPlanById(planId: Long) = plansFlow.value.firstOrNull { it.id == planId }
        override suspend fun getTemplateById(templateId: Long) = templatesFlow.value.firstOrNull { it.id == templateId }

        override suspend fun createPlan(name: String, createdAtEpochMs: Long): Long {
            val id = nextId++
            val active = plansFlow.value.isEmpty()
            plansFlow.value = plansFlow.value + TrainingPlanEntity(id, name, active, createdAtEpochMs)
            return id
        }

        override suspend fun renamePlan(planId: Long, newName: String) {
            plansFlow.value = plansFlow.value.map { if (it.id == planId) it.copy(name = newName) else it }
        }

        override suspend fun deletePlan(planId: Long) {
            plansFlow.value = plansFlow.value.filterNot { it.id == planId }
        }

        override suspend fun setActivePlan(planId: Long) {
            plansFlow.value = plansFlow.value.map { it.copy(isActive = it.id == planId) }
        }

        override suspend fun createWorkoutTemplate(planId: Long, name: String): Long {
            val id = nextId++
            val order = templatesFlow.value.count { it.planId == planId }
            templatesFlow.value = templatesFlow.value + WorkoutTemplateEntity(id, planId, name, order)
            return id
        }

        override suspend fun renameWorkoutTemplate(templateId: Long, newName: String) {
            templatesFlow.value = templatesFlow.value.map { if (it.id == templateId) it.copy(name = newName) else it }
        }

        override suspend fun deleteWorkoutTemplate(templateId: Long) {
            templatesFlow.value = templatesFlow.value.filterNot { it.id == templateId }
        }

        override suspend fun moveWorkoutTemplate(planId: Long, templateId: Long, moveUp: Boolean) {
            val list = templatesFlow.value.filter { it.planId == planId }.sortedBy { it.orderIndex }.toMutableList()
            val idx = list.indexOfFirst { it.id == templateId }
            val target = if (moveUp) idx - 1 else idx + 1
            if (idx == -1 || target !in list.indices) return
            val swapped = list[idx]
            list[idx] = list[target]
            list[target] = swapped
            val reindexed = list.mapIndexed { i, item -> item.copy(orderIndex = i) }
            templatesFlow.value = templatesFlow.value.filterNot { it.planId == planId } + reindexed
        }

        override suspend fun addExerciseToWorkout(
            workoutTemplateId: Long,
            exerciseId: Long,
            targetSets: Int,
            targetReps: Int,
            restSeconds: Int
        ): Long {
            val id = nextId++
            val order = workoutExercisesFlow.value.count { it.workoutTemplateId == workoutTemplateId }
            workoutExercisesFlow.value = workoutExercisesFlow.value + WorkoutExerciseDetail(
                id = id,
                workoutTemplateId = workoutTemplateId,
                exerciseId = exerciseId,
                orderIndex = order,
                targetSets = targetSets,
                targetReps = targetReps,
                restSeconds = restSeconds,
                exerciseName = "Exercício $exerciseId",
                equipmentName = "Aparelho"
            )
            return id
        }

        override suspend fun updateWorkoutExercise(id: Long, targetSets: Int, targetReps: Int, restSeconds: Int) {
            workoutExercisesFlow.value = workoutExercisesFlow.value.map {
                if (it.id == id) it.copy(targetSets = targetSets, targetReps = targetReps, restSeconds = restSeconds) else it
            }
        }

        override suspend fun removeWorkoutExercise(id: Long) {
            workoutExercisesFlow.value = workoutExercisesFlow.value.filterNot { it.id == id }
        }

        override suspend fun moveWorkoutExercise(workoutTemplateId: Long, workoutExerciseId: Long, moveUp: Boolean) {
            val list = workoutExercisesFlow.value
                .filter { it.workoutTemplateId == workoutTemplateId }
                .sortedBy { it.orderIndex }
                .toMutableList()
            val idx = list.indexOfFirst { it.id == workoutExerciseId }
            val target = if (moveUp) idx - 1 else idx + 1
            if (idx == -1 || target !in list.indices) return
            val swapped = list[idx]
            list[idx] = list[target]
            list[target] = swapped
            val reindexed = list.mapIndexed { i, item -> item.copy(orderIndex = i) }
            workoutExercisesFlow.value =
                workoutExercisesFlow.value.filterNot { it.workoutTemplateId == workoutTemplateId } + reindexed
        }

        override suspend fun setScheduleDay(planId: Long, dayOfWeek: Int, workoutTemplateId: Long?) {
            scheduleFlow.value = scheduleFlow.value.filterNot { it.planId == planId && it.dayOfWeek == dayOfWeek } +
                WeeklyScheduleEntity(planId, dayOfWeek, workoutTemplateId)
        }
    }

    private class FakeCatalogRepoForPlans : CatalogRepository {
        private val list = listOf(
            ExerciseWithEquipment(
                ExerciseEntity(1L, "Supino Reto", MuscleGroup.CHEST, null, 1L),
                "Supino Reto",
                EquipmentCategory.BENCH_RACK
            ),
            ExerciseWithEquipment(
                ExerciseEntity(2L, "Puxada Frontal", MuscleGroup.BACK, null, 2L),
                "Polia Alta",
                EquipmentCategory.CABLE
            )
        )

        override fun observeEquipment(): Flow<List<EquipmentEntity>> = flowOf(emptyList())
        override fun observeExercisesWithEquipment(): Flow<List<ExerciseWithEquipment>> = flowOf(list)
        override suspend fun createEquipment(name: String, category: EquipmentCategory, notes: String) = 0L
        override suspend fun updateEquipment(id: Long, name: String, category: EquipmentCategory, notes: String) = Unit
        override suspend fun deleteEquipment(id: Long) = Unit
        override suspend fun createExercise(name: String, primaryMuscle: MuscleGroup, secondaryMuscle: MuscleGroup?, equipmentId: Long, notes: String) = 0L
        override suspend fun updateExercise(id: Long, name: String, primaryMuscle: MuscleGroup, secondaryMuscle: MuscleGroup?, equipmentId: Long, notes: String) = Unit
        override suspend fun deleteExercise(id: Long) = Unit
    }
}
