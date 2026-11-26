package com.gymslut.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymslut.app.data.local.CatalogSeedData
import com.gymslut.app.data.local.GymSlutDatabase
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.model.DuplicateResourceException
import com.gymslut.app.data.model.ResourceInUseException
import com.gymslut.app.data.repository.RoomCatalogRepository
import com.gymslut.app.data.repository.RoomPlanRepository
import com.gymslut.app.data.repository.RoomSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GymSlutDatabaseTest {

    private lateinit var db: GymSlutDatabase
    private lateinit var catalogRepo: RoomCatalogRepository
    private lateinit var planRepo: RoomPlanRepository
    private lateinit var sessionRepo: RoomSessionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymSlutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalogRepo = RoomCatalogRepository(db.equipmentDao(), db.exerciseDao())
        planRepo = RoomPlanRepository(db.planDao(), db.scheduleDao())
        sessionRepo = RoomSessionRepository(db.sessionDao(), db.planDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun catalogSeed_populatesExpectedCountsAndIsIdempotent() = runTest {
        CatalogSeedData.seedIfNeeded(db.equipmentDao(), db.exerciseDao())
        val firstEquipmentCount = db.equipmentDao().getAll().size
        val firstExerciseCount = db.exerciseDao().getAll().size

        assertTrue(firstEquipmentCount >= 18)
        assertTrue(firstExerciseCount >= 45)

        val muscles = db.exerciseDao().getAll().map { it.primaryMuscle }.toSet()
        assertTrue(muscles.containsAll(MuscleGroup.entries))

        CatalogSeedData.seedIfNeeded(db.equipmentDao(), db.exerciseDao())
        assertEquals(firstEquipmentCount, db.equipmentDao().getAll().size)
        assertEquals(firstExerciseCount, db.exerciseDao().getAll().size)
    }

    @Test
    fun catalogRepository_fullCrudAndValidationEdgeCases() = runTest {
        // Nome em branco rejeitado
        try {
            catalogRepo.createEquipment("   ", EquipmentCategory.MACHINE)
            fail("Esperado IllegalArgumentException para aparelho sem nome")
        } catch (_: IllegalArgumentException) {
        }

        val eq1 = catalogRepo.createEquipment("Polia Articulada", EquipmentCategory.CABLE, "  obs  ")
        val eq2 = catalogRepo.createEquipment("Barra Livre", EquipmentCategory.FREE_WEIGHT)

        // Duplicidade ignorando caixa e espaços
        try {
            catalogRepo.createEquipment("  polia articulada ", EquipmentCategory.CABLE)
            fail("Deveria lançar DuplicateResourceException")
        } catch (_: DuplicateResourceException) {
        }

        // Update próprio nome com case diferente permitido; colidir com eq2 proibido; id inexistente falha
        catalogRepo.updateEquipment(eq1, "polia articulada v2", EquipmentCategory.MACHINE, "nova obs")
        assertEquals("polia articulada v2", db.equipmentDao().getById(eq1)?.name)

        try {
            catalogRepo.updateEquipment(eq1, "Barra Livre", EquipmentCategory.MACHINE)
            fail("Deveria impedir rename para nome já existente")
        } catch (_: DuplicateResourceException) {
        }
        try {
            catalogRepo.updateEquipment(eq1, "  ", EquipmentCategory.MACHINE)
            fail("Deveria impedir rename em branco")
        } catch (_: IllegalArgumentException) {
        }
        try {
            catalogRepo.updateEquipment(9999L, "Inexistente", EquipmentCategory.OTHER)
            fail("Deveria falhar para ID inexistente")
        } catch (_: IllegalArgumentException) {
        }

        // Exercício com secondaryMuscle == primaryMuscle deve normalizar para null
        val exId = catalogRepo.createExercise(
            name = "Tríceps Unilateral",
            primaryMuscle = MuscleGroup.TRICEPS,
            secondaryMuscle = MuscleGroup.TRICEPS,
            equipmentId = eq1,
            notes = "  foco cotovelo "
        )
        val joined = catalogRepo.observeExercisesWithEquipment().first()
        assertEquals(1, joined.size)
        assertNull(joined.first().exercise.secondaryMuscle)
        assertEquals("foco cotovelo", joined.first().exercise.notes)

        // Erros ao criar/atualizar exercício
        try {
            catalogRepo.createExercise(" ", MuscleGroup.CHEST, null, eq1)
            fail("Esperado erro nome vazio")
        } catch (_: IllegalArgumentException) {
        }
        try {
            catalogRepo.createExercise("Teste", MuscleGroup.CHEST, null, 9999L)
            fail("Esperado erro aparelho inexistente")
        } catch (_: IllegalArgumentException) {
        }
        try {
            catalogRepo.updateExercise(9999L, "Teste", MuscleGroup.CHEST, null, eq1)
            fail("Esperado erro exercício inexistente")
        } catch (_: IllegalArgumentException) {
        }
        catalogRepo.updateExercise(exId, "Tríceps Corda", MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS, eq2, "ajustado")
        assertEquals("Tríceps Corda", db.exerciseDao().getById(exId)?.name)

        // eq1 agora está livre e pode ser excluído; eq2 está em uso e deve falhar
        catalogRepo.deleteEquipment(eq1)
        catalogRepo.deleteEquipment(9999L) // no-op
        assertEquals(1, catalogRepo.observeEquipment().first().size)

        try {
            catalogRepo.deleteEquipment(eq2)
            fail("Deveria impedir exclusão de aparelho em uso")
        } catch (_: ResourceInUseException) {
        }

        val planId = planRepo.createPlan("Push Pull Legs")
        val templateId = planRepo.createWorkoutTemplate(planId, "Push")
        val weId = planRepo.addExerciseToWorkout(templateId, exId, targetSets = 3, targetReps = 12, restSeconds = 0)

        try {
            catalogRepo.deleteExercise(exId)
            fail("Deveria impedir exclusão de exercício em uso na ficha")
        } catch (_: ResourceInUseException) {
        }

        // Após remover da ficha, exercício e aparelho podem ser excluídos
        planRepo.removeWorkoutExercise(weId)
        catalogRepo.deleteExercise(exId)
        catalogRepo.deleteExercise(9999L) // no-op
        catalogRepo.deleteEquipment(eq2)
        assertTrue(catalogRepo.observeEquipment().first().isEmpty())
    }

    @Test
    fun planRepository_reorderScheduleBoundsAndActivePlanEdgeCases() = runTest {
        val eqId = catalogRepo.createEquipment("Banco", EquipmentCategory.BENCH_RACK)
        val ex1 = catalogRepo.createExercise("Supino", MuscleGroup.CHEST, null, eqId)
        val ex2 = catalogRepo.createExercise("Crucifixo", MuscleGroup.CHEST, null, eqId)

        try {
            planRepo.createPlan("  ")
            fail("Esperado erro nome vazio")
        } catch (_: IllegalArgumentException) {
        }

        val p1 = planRepo.createPlan("Plano A", createdAtEpochMs = 100L)
        val p2 = planRepo.createPlan("Plano B", createdAtEpochMs = 200L)
        assertEquals(p1, db.planDao().getActivePlan()?.id)
        assertEquals(7, planRepo.observeScheduleForPlan(p1).first().size)

        // setActivePlan para ID inexistente não deve desativar p1
        planRepo.setActivePlan(9999L)
        assertEquals(p1, planRepo.observeActivePlan().first()?.id)

        planRepo.renamePlan(p1, "Plano A Editado")
        assertEquals("Plano A Editado", planRepo.getPlanById(p1)?.name)

        planRepo.setActivePlan(p2)
        assertEquals(p2, db.planDao().getActivePlan()?.id)

        val t1 = planRepo.createWorkoutTemplate(p2, "Treino A")
        val t2 = planRepo.createWorkoutTemplate(p2, "Treino B")
        planRepo.renameWorkoutTemplate(t1, "Treino A1")
        assertEquals("Treino A1", planRepo.getTemplateById(t1)?.name)

        // Reordenação e limites (primeiro pra cima, último pra baixo, id desconhecido)
        planRepo.moveWorkoutTemplate(p2, t1, moveUp = true)
        planRepo.moveWorkoutTemplate(p2, t2, moveUp = false)
        planRepo.moveWorkoutTemplate(p2, 9999L, moveUp = true)
        planRepo.moveWorkoutTemplate(p2, t2, moveUp = true)
        assertEquals(listOf(t2, t1), planRepo.observeTemplatesForPlan(p2).first().map { it.id })

        // Validação numérica de prescrição
        try {
            planRepo.addExerciseToWorkout(t1, ex1, 0, 10, 60)
            fail("sets=0 deve falhar")
        } catch (_: IllegalArgumentException) {
        }
        try {
            planRepo.addExerciseToWorkout(t1, ex1, 3, 0, 60)
            fail("reps=0 deve falhar")
        } catch (_: IllegalArgumentException) {
        }
        try {
            planRepo.addExerciseToWorkout(t1, ex1, 3, 10, -1)
            fail("rest=-1 deve falhar")
        } catch (_: IllegalArgumentException) {
        }

        val we1 = planRepo.addExerciseToWorkout(t1, ex1, 3, 10, 60)
        val we2 = planRepo.addExerciseToWorkout(t1, ex2, 4, 12, 0)
        planRepo.updateWorkoutExercise(we1, 5, 8, 120)
        planRepo.updateWorkoutExercise(9999L, 3, 10, 60) // no-op
        assertEquals(5, planRepo.observeWorkoutExercises(t1).first().first { it.id == we1 }.targetSets)

        planRepo.moveWorkoutExercise(t1, we1, moveUp = true) // boundary no-op
        planRepo.moveWorkoutExercise(t1, we2, moveUp = false) // boundary no-op
        planRepo.moveWorkoutExercise(t1, 9999L, moveUp = true) // unknown no-op
        planRepo.moveWorkoutExercise(t1, we2, moveUp = true)
        assertEquals(listOf(we2, we1), planRepo.observeWorkoutExercises(t1).first().map { it.id })

        // Cronograma fora do intervalo 1..7 deve falhar
        try {
            planRepo.setScheduleDay(p2, 0, t1)
            fail("dayOfWeek=0 deve falhar")
        } catch (_: IllegalArgumentException) {
        }
        try {
            planRepo.setScheduleDay(p2, 8, t1)
            fail("dayOfWeek=8 deve falhar")
        } catch (_: IllegalArgumentException) {
        }
        planRepo.setScheduleDay(p2, 1, t1)
        assertEquals(t1, db.scheduleDao().getScheduleEntry(p2, 1)?.workoutTemplateId)

        // Excluir template transforma o dia agendado em Descanso (null via ON DELETE SET NULL)
        planRepo.deleteWorkoutTemplate(t1)
        assertNull(db.scheduleDao().getScheduleEntry(p2, 1)?.workoutTemplateId)

        planRepo.deletePlan(p1)
        assertNull(planRepo.getPlanById(p1))
    }

    @Test
    fun sessionRepository_suggestionsFallbackHistoryAndDeletionEdgeCases() = runTest {
        val eqId = catalogRepo.createEquipment("Supino Teste", EquipmentCategory.BENCH_RACK)
        val exId = catalogRepo.createExercise("Supino Reto", MuscleGroup.CHEST, MuscleGroup.TRICEPS, eqId)
        val planId = planRepo.createPlan("ABC")
        val emptyTemplateId = planRepo.createWorkoutTemplate(planId, "Treino Vazio")
        val templateId = planRepo.createWorkoutTemplate(planId, "Treino A")
        planRepo.addExerciseToWorkout(templateId, exId, targetSets = 3, targetReps = 10, restSeconds = 90)

        // Template inexistente lança erro
        try {
            sessionRepo.startWorkoutSession(9999L)
            fail("Template inexistente deveria lançar erro")
        } catch (_: IllegalArgumentException) {
        }

        // Template vazio cria sessão sem séries
        val emptySessionId = sessionRepo.startWorkoutSession(emptyTemplateId, startedAtEpochMs = 500L)
        assertTrue(sessionRepo.observeSetsForSession(emptySessionId).first().isEmpty())
        sessionRepo.discardSession(emptySessionId)

        // Sessão 1 concluída com apenas 1 série concluída (série 2 não concluída)
        val s1 = sessionRepo.startWorkoutSession(templateId, startedAtEpochMs = 1000L)
        val s1Sets = db.sessionDao().getSetsForSession(s1)
        assertEquals(3, s1Sets.size)
        assertEquals(0.0, s1Sets[0].weightKg, 0.001)
        sessionRepo.updateSet(s1Sets[0].copy(weightKg = 80.0, reps = 10, completed = true, completedAtEpochMs = 1100L))
        // Série 2 fica completed = false (não deve entrar na sugestão)
        sessionRepo.finishSession(s1, finishedAtEpochMs = 1300L)
        sessionRepo.finishSession(9999L, finishedAtEpochMs = 1300L) // no-op seguro

        // Sessão 2 em aberto não deve contaminar sugestões nem histórico concluído
        val s2 = db.sessionDao().insertSession(
            WorkoutSessionEntity(
                planId = planId,
                workoutTemplateId = templateId,
                workoutNameSnapshot = "Treino A",
                startedAtEpochMs = 2000L,
                finishedAtEpochMs = null
            )
        )
        db.sessionDao().insertSet(
            SessionSetEntity(
                sessionId = s2,
                exerciseId = exId,
                exerciseNameSnapshot = "Supino Reto",
                equipmentNameSnapshot = "Supino Teste",
                exerciseOrderIndex = 0,
                setNumber = 1,
                weightKg = 999.0,
                reps = 1,
                completed = true,
                completedAtEpochMs = 2100L
            )
        )
        assertNotNull(sessionRepo.observeInProgressSession().first())

        // Sessão 3 recebe sugestão da série 1 para S1, e fallbackLastSet (80.0) para S2 e S3!
        val s3 = sessionRepo.startWorkoutSession(templateId, startedAtEpochMs = 3000L)
        val s3Sets = sessionRepo.observeSetsForSession(s3).first()
        assertEquals(80.0, s3Sets[0].weightKg, 0.001)
        assertEquals(80.0, s3Sets[1].weightKg, 0.001)
        assertEquals(80.0, s3Sets[2].weightKg, 0.001)

        // addExtraSet + removeSet + erro para exerciseOrderIndex inexistente
        val extraId = sessionRepo.addExtraSet(s3, exerciseOrderIndex = 0)
        assertEquals(4, sessionRepo.observeSetsForSession(s3).first().last().setNumber)
        sessionRepo.removeSet(extraId)
        assertEquals(3, sessionRepo.observeSetsForSession(s3).first().size)

        try {
            sessionRepo.addExtraSet(s3, exerciseOrderIndex = 99)
            fail("exerciseOrderIndex inexistente deveria lançar erro")
        } catch (_: IllegalArgumentException) {
        }

        // Excluir s2 e s3; histórico concluído e por exercício preservam snapshots mesmo apagando o plano
        sessionRepo.discardSession(s2)
        sessionRepo.discardSession(s3)
        planRepo.deletePlan(planId)

        val completedSummaries = sessionRepo.observeCompletedSessionsWithSets().first()
        assertEquals(1, completedSummaries.size)
        assertEquals("Treino A", completedSummaries.first().session.workoutNameSnapshot)
        assertEquals(1, sessionRepo.observeCompletedSessions().first().size)

        val exHistory = sessionRepo.observeExerciseHistory(exId).first()
        assertEquals(3, exHistory.size)
        assertEquals("Supino Reto", exHistory.first().exerciseNameSnapshot)
    }
}
