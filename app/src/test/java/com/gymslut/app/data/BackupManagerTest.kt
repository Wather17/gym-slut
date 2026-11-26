package com.gymslut.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymslut.app.data.local.GymSlutDatabase
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.repository.BackupValidationException
import com.gymslut.app.data.repository.RoomBackupRepository
import com.gymslut.app.data.repository.RoomCatalogRepository
import com.gymslut.app.data.repository.RoomPlanRepository
import com.gymslut.app.data.repository.RoomSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupManagerTest {

    private lateinit var db: GymSlutDatabase
    private lateinit var catalogRepo: RoomCatalogRepository
    private lateinit var planRepo: RoomPlanRepository
    private lateinit var sessionRepo: RoomSessionRepository
    private lateinit var backupRepo: RoomBackupRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymSlutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalogRepo = RoomCatalogRepository(db.equipmentDao(), db.exerciseDao())
        planRepo = RoomPlanRepository(db.planDao(), db.scheduleDao())
        sessionRepo = RoomSessionRepository(db.sessionDao(), db.planDao())
        backupRepo = RoomBackupRepository(db.backupDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun exportAndRestore_roundTripPreservesAllTables() = runTest {
        val eqId = catalogRepo.createEquipment("Supino Reto", EquipmentCategory.BENCH_RACK)
        val exId = catalogRepo.createExercise("Supino Reto", MuscleGroup.CHEST, MuscleGroup.TRICEPS, eqId)
        val planId = planRepo.createPlan("ABC", createdAtEpochMs = 100L)
        val templateId = planRepo.createWorkoutTemplate(planId, "Treino A")
        planRepo.addExerciseToWorkout(templateId, exId, 3, 10, 90)
        planRepo.setScheduleDay(planId, 1, templateId)

        val sessionId = sessionRepo.startWorkoutSession(templateId, startedAtEpochMs = 1_000L)
        val sets = db.sessionDao().getSetsForSession(sessionId)
        sessionRepo.updateSet(sets.first().copy(weightKg = 80.0, reps = 10, completed = true, completedAtEpochMs = 1_100L))
        sessionRepo.finishSession(sessionId, finishedAtEpochMs = 2_000L)

        val exportedJson = backupRepo.exportToJson(exportedAtEpochMs = 5_000L)
        assertTrue(exportedJson.contains("\"schemaVersion\": 1"))

        // Limpa/altera banco
        db.backupDao().replaceAllData(
            emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList()
        )
        assertEquals(0, db.equipmentDao().getAll().size)

        // Restaura backup
        backupRepo.restoreFromJson(exportedJson)

        assertEquals(1, db.equipmentDao().getAll().size)
        assertEquals(1, db.exerciseDao().getAll().size)
        assertEquals(1, db.backupDao().getAllPlans().size)
        assertEquals(1, db.backupDao().getAllTemplates().size)
        assertEquals(1, db.backupDao().getAllWorkoutExercises().size)
        assertEquals(7, db.backupDao().getAllWeeklySchedules().size)
        assertEquals(1, db.backupDao().getAllSessions().size)
        assertEquals(3, db.backupDao().getAllSessionSets().size)
    }

    @Test
    fun restoreInvalidJsonOrUnsupportedSchemaOrBrokenFk_doesNotCorruptExistingData() = runTest {
        val eqId = catalogRepo.createEquipment("Polia Alta", EquipmentCategory.CABLE)
        catalogRepo.createExercise("Puxada", MuscleGroup.BACK, null, eqId)

        val invalidInputs = listOf(
            "",
            "{ not valid json",
            """{"schemaVersion":99,"exportedAtEpochMs":1}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[],"exercises":[{"id":1,"name":"X","primaryMuscle":"CHEST","equipmentId":999}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1},{"id":2,"name":"B","isActive":true,"createdAtEpochMs":2}]}"""
        )

        for (badInput in invalidInputs) {
            try {
                backupRepo.restoreFromJson(badInput)
                fail("Deveria rejeitar backup inválido: $badInput")
            } catch (_: BackupValidationException) {
                // esperado
            }
        }

        // Dados existentes permanecem intactos
        assertEquals(1, db.equipmentDao().getAll().size)
        assertEquals(1, db.exerciseDao().getAll().size)
    }
}
