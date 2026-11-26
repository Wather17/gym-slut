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

        // Executar novamente não deve duplicar
        CatalogSeedData.seedIfNeeded(db.equipmentDao(), db.exerciseDao())
        assertEquals(firstEquipmentCount, db.equipmentDao().getAll().size)
        assertEquals(firstExerciseCount, db.exerciseDao().getAll().size)
    }

    @Test
    fun catalogRepository_preventsDuplicateEquipmentAndDeletingInUseItems() = runTest {
        val equipmentId = catalogRepo.createEquipment("Polia Articulada", EquipmentCategory.CABLE)
        try {
            catalogRepo.createEquipment("  polia articulada ", EquipmentCategory.CABLE)
            fail("Deveria lançar DuplicateResourceException")
        } catch (_: DuplicateResourceException) {
            // esperado
        }

        val exerciseId = catalogRepo.createExercise(
            name = "Tríceps Unilateral",
            primaryMuscle = MuscleGroup.TRICEPS,
            secondaryMuscle = null,
            equipmentId = equipmentId
        )

        try {
            catalogRepo.deleteEquipment(equipmentId)
            fail("Deveria impedir exclusão de aparelho em uso")
        } catch (_: ResourceInUseException) {
            // esperado
        }

        val planId = planRepo.createPlan("Push Pull Legs")
        val templateId = planRepo.createWorkoutTemplate(planId, "Push")
        planRepo.addExerciseToWorkout(templateId, exerciseId, targetSets = 3, targetReps = 12, restSeconds = 60)

        try {
            catalogRepo.deleteExercise(exerciseId)
            fail("Deveria impedir exclusão de exercício em uso na ficha")
        } catch (_: ResourceInUseException) {
            // esperado
        }
    }

    @Test
    fun planRepository_keepsAtMostOneActivePlanAndCascadesTemplateDeletion() = runTest {
        val p1 = planRepo.createPlan("Plano A", createdAtEpochMs = 100L)
        val p2 = planRepo.createPlan("Plano B", createdAtEpochMs = 200L)

        // Primeiro plano criado é ativado automaticamente
        assertEquals(p1, db.planDao().getActivePlan()?.id)

        planRepo.setActivePlan(p2)
        val allPlans = planRepo.observeAllPlans().first()
        assertEquals(1, allPlans.count { it.isActive })
        assertEquals(p2, db.planDao().getActivePlan()?.id)

        val t1 = planRepo.createWorkoutTemplate(p2, "Treino A")
        planRepo.setScheduleDay(p2, 1, t1)
        assertEquals(t1, db.scheduleDao().getScheduleEntry(p2, 1)?.workoutTemplateId)

        // Excluir template transforma o dia agendado em Descanso (null via ON DELETE SET NULL)
        planRepo.deleteWorkoutTemplate(t1)
        assertNull(db.scheduleDao().getScheduleEntry(p2, 1)?.workoutTemplateId)
    }

    @Test
    fun sessionRepository_suggestsLatestCompletedSessionAndIgnoresInProgress() = runTest {
        val eqId = catalogRepo.createEquipment("Supino Teste", EquipmentCategory.BENCH_RACK)
        val exId = catalogRepo.createExercise("Supino Reto", MuscleGroup.CHEST, MuscleGroup.TRICEPS, eqId)
        val planId = planRepo.createPlan("ABC")
        val templateId = planRepo.createWorkoutTemplate(planId, "Treino A")
        planRepo.addExerciseToWorkout(templateId, exId, targetSets = 2, targetReps = 10, restSeconds = 90)

        // Sessão 1 concluída
        val s1 = sessionRepo.startWorkoutSession(templateId, startedAtEpochMs = 1000L)
        val s1Sets = db.sessionDao().getSetsForSession(s1)
        assertEquals(0.0, s1Sets[0].weightKg, 0.001)
        assertEquals(10, s1Sets[0].reps)
        sessionRepo.updateSet(s1Sets[0].copy(weightKg = 80.0, reps = 10, completed = true, completedAtEpochMs = 1100L))
        sessionRepo.updateSet(s1Sets[1].copy(weightKg = 82.5, reps = 8, completed = true, completedAtEpochMs = 1200L))
        sessionRepo.finishSession(s1, finishedAtEpochMs = 1300L)

        // Sessão 2 em aberto não deve contaminar sugestões
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

        val latest = db.sessionDao().getLatestCompletedSetsForExercise(exId)
        assertEquals(2, latest.size)
        assertEquals(80.0, latest[0].weightKg, 0.001)
        assertEquals(82.5, latest[1].weightKg, 0.001)

        // Sessão 3 recebe sugestões da Sessão 1
        val s3 = sessionRepo.startWorkoutSession(templateId, startedAtEpochMs = 3000L)
        val s3Sets = db.sessionDao().getSetsForSession(s3)
        assertNotNull(s3Sets)
        assertEquals(80.0, s3Sets[0].weightKg, 0.001)
        assertEquals(10, s3Sets[0].reps)
        assertEquals(82.5, s3Sets[1].weightKg, 0.001)
        assertEquals(8, s3Sets[1].reps)
    }
}
