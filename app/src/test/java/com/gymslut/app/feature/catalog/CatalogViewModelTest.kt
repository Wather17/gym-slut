package com.gymslut.app.feature.catalog

import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.model.DuplicateResourceException
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.model.ResourceInUseException
import com.gymslut.app.data.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
class CatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeCatalogRepository
    private lateinit var viewModel: CatalogViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeCatalogRepository()
        viewModel = CatalogViewModel(fakeRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filters_combineSearchMuscleEquipmentAndCategoryTabs() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasActiveFilters)

        // Filtro por músculo secundário também encontra o exercício
        viewModel.selectMuscleFilter(MuscleGroup.TRICEPS)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        assertEquals(1, viewModel.uiState.value.exercises.size)

        viewModel.selectMuscleFilter(MuscleGroup.CHEST)
        viewModel.selectEquipmentFilter(1L)
        viewModel.updateSearchQuery("Supino")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.exercises.size)
        assertEquals("Supino Reto", state.exercises.first().exercise.name)

        // Trocar para aba Aparelhos e filtrar por categoria
        viewModel.selectTab(CatalogTab.EQUIPMENT)
        viewModel.selectCategoryFilter(EquipmentCategory.CABLE)
        viewModel.updateSearchQuery("Polia")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        assertEquals(1, viewModel.uiState.value.filteredEquipment.size)

        viewModel.clearFilters()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasActiveFilters)
        assertEquals(2, viewModel.uiState.value.filteredEquipment.size)
        assertEquals(2, viewModel.uiState.value.exercises.size)
        collectJob.cancel()
    }

    @Test
    fun saveEquipmentAndExercise_validatesCreatesEditsAndClearsMessages() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        var succeeded = false
        viewModel.saveEquipment(null, "   ", EquipmentCategory.MACHINE, "") { succeeded = true }
        advanceUntilIdle()
        assertFalse(succeeded)
        assertNotNull(viewModel.uiState.value.formError)
        viewModel.clearFormError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.formError)

        viewModel.saveEquipment(null, "Supino Reto", EquipmentCategory.BENCH_RACK, "") { succeeded = true }
        advanceUntilIdle()
        assertFalse(succeeded)
        assertTrue(viewModel.uiState.value.formError!!.contains("Já existe"))

        viewModel.saveEquipment(null, "Hack 45", EquipmentCategory.MACHINE, "Novo") { succeeded = true }
        advanceUntilIdle()
        assertTrue(succeeded)
        assertNull(viewModel.uiState.value.formError)
        assertTrue(viewModel.uiState.value.feedbackMessage!!.contains("adicionado"))

        // Edição de aparelho
        val createdEq = viewModel.uiState.value.allEquipment.last()
        var editedEq = false
        viewModel.saveEquipment(createdEq.id, "Hack 45 Pro", EquipmentCategory.MACHINE, "Edit") { editedEq = true }
        advanceUntilIdle()
        assertTrue(editedEq)
        assertTrue(viewModel.uiState.value.feedbackMessage!!.contains("atualizado"))
        viewModel.clearFeedbackMessage()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.feedbackMessage)

        // Validações e CRUD de exercício
        var exSaved = false
        viewModel.saveExercise(null, "  ", MuscleGroup.QUADS, null, 1L, "") { exSaved = true }
        advanceUntilIdle()
        assertFalse(exSaved)
        assertNotNull(viewModel.uiState.value.formError)

        viewModel.saveExercise(null, "Hack Squat", MuscleGroup.QUADS, null, null, "") { exSaved = true }
        advanceUntilIdle()
        assertFalse(exSaved)
        assertTrue(viewModel.uiState.value.formError!!.contains("aparelho"))

        viewModel.saveExercise(null, "Hack Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES, createdEq.id, "Descer bem") {
            exSaved = true
        }
        advanceUntilIdle()
        assertTrue(exSaved)

        val createdEx = viewModel.uiState.value.exercises.first { it.exercise.name == "Hack Squat" }
        var exEdited = false
        viewModel.saveExercise(
            createdEx.exercise.id,
            "Hack Squat Profundo",
            MuscleGroup.QUADS,
            null,
            createdEq.id,
            ""
        ) { exEdited = true }
        advanceUntilIdle()
        assertTrue(exEdited)
        assertTrue(viewModel.uiState.value.exercises.any { it.exercise.name == "Hack Squat Profundo" })

        collectJob.cancel()
    }

    @Test
    fun deleteResources_handlesSuccessAndInUseErrors() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val eqInUse = viewModel.uiState.value.allEquipment.first()
        viewModel.deleteEquipment(eqInUse)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.feedbackMessage!!.contains("Não é possível excluir"))

        val exInUse = viewModel.uiState.value.exercises.first()
        fakeRepo.inUseExerciseIds.add(exInUse.exercise.id)
        viewModel.deleteExercise(exInUse)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.feedbackMessage!!.contains("fichas"))

        // Exclusão bem-sucedida quando não está em uso
        val freeEx = viewModel.uiState.value.exercises.last()
        viewModel.deleteExercise(freeEx)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.feedbackMessage!!.contains("excluído"))

        val freeEq = viewModel.uiState.value.allEquipment.last()
        viewModel.deleteEquipment(freeEq)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.feedbackMessage!!.contains("excluído"))

        collectJob.cancel()
    }

    private class FakeCatalogRepository : CatalogRepository {
        private val equipmentFlow = MutableStateFlow(
            listOf(
                EquipmentEntity(id = 1L, name = "Supino Reto", category = EquipmentCategory.BENCH_RACK),
                EquipmentEntity(id = 2L, name = "Polia Alta", category = EquipmentCategory.CABLE)
            )
        )
        private val exercisesFlow = MutableStateFlow(
            listOf(
                ExerciseEntity(
                    id = 10L,
                    name = "Supino Reto",
                    primaryMuscle = MuscleGroup.CHEST,
                    secondaryMuscle = MuscleGroup.TRICEPS,
                    equipmentId = 1L
                ),
                ExerciseEntity(
                    id = 11L,
                    name = "Puxada Frontal",
                    primaryMuscle = MuscleGroup.BACK,
                    secondaryMuscle = MuscleGroup.BICEPS,
                    equipmentId = 2L
                )
            )
        )
        val inUseExerciseIds = mutableSetOf<Long>()
        private var nextId = 100L

        override fun observeEquipment(): Flow<List<EquipmentEntity>> = equipmentFlow

        override fun observeExercisesWithEquipment(): Flow<List<ExerciseWithEquipment>> =
            exercisesFlow.map { list ->
                val eqById = equipmentFlow.value.associateBy { it.id }
                list.mapNotNull { ex ->
                    val eq = eqById[ex.equipmentId] ?: return@mapNotNull null
                    ExerciseWithEquipment(ex, eq.name, eq.category)
                }
            }

        override suspend fun createEquipment(
            name: String,
            category: EquipmentCategory,
            notes: String
        ): Long {
            if (equipmentFlow.value.any { it.name.equals(name.trim(), ignoreCase = true) }) {
                throw DuplicateResourceException("Já existe um aparelho com o nome \"$name\".")
            }
            val id = nextId++
            equipmentFlow.value = equipmentFlow.value + EquipmentEntity(id, name.trim(), category, notes, true)
            return id
        }

        override suspend fun updateEquipment(
            id: Long,
            name: String,
            category: EquipmentCategory,
            notes: String
        ) {
            equipmentFlow.value = equipmentFlow.value.map {
                if (it.id == id) it.copy(name = name.trim(), category = category, notes = notes) else it
            }
        }

        override suspend fun deleteEquipment(id: Long) {
            if (exercisesFlow.value.any { it.equipmentId == id }) {
                throw ResourceInUseException("Não é possível excluir porque há exercícios vinculados.")
            }
            equipmentFlow.value = equipmentFlow.value.filterNot { it.id == id }
        }

        override suspend fun createExercise(
            name: String,
            primaryMuscle: MuscleGroup,
            secondaryMuscle: MuscleGroup?,
            equipmentId: Long,
            notes: String
        ): Long {
            val id = nextId++
            exercisesFlow.value = exercisesFlow.value + ExerciseEntity(
                id = id,
                name = name.trim(),
                primaryMuscle = primaryMuscle,
                secondaryMuscle = secondaryMuscle,
                equipmentId = equipmentId,
                notes = notes,
                isCustom = true
            )
            return id
        }

        override suspend fun updateExercise(
            id: Long,
            name: String,
            primaryMuscle: MuscleGroup,
            secondaryMuscle: MuscleGroup?,
            equipmentId: Long,
            notes: String
        ) {
            exercisesFlow.value = exercisesFlow.value.map {
                if (it.id == id) {
                    it.copy(
                        name = name.trim(),
                        primaryMuscle = primaryMuscle,
                        secondaryMuscle = secondaryMuscle,
                        equipmentId = equipmentId,
                        notes = notes
                    )
                } else {
                    it
                }
            }
        }

        override suspend fun deleteExercise(id: Long) {
            if (inUseExerciseIds.contains(id)) {
                throw ResourceInUseException("Não é possível excluir porque faz parte de fichas de treino.")
            }
            exercisesFlow.value = exercisesFlow.value.filterNot { it.id == id }
        }
    }
}
