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
    fun exportAndRestore_roundTripPreservesAllTablesAndNullableSnapshots() = runTest {
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

        db.backupDao().replaceAllData(
            emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList()
        )
        assertEquals(0, db.equipmentDao().getAll().size)

        backupRepo.restoreFromJson(exportedJson)

        assertEquals(1, db.equipmentDao().getAll().size)
        assertEquals(1, db.exerciseDao().getAll().size)
        assertEquals(1, db.backupDao().getAllPlans().size)
        assertEquals(1, db.backupDao().getAllTemplates().size)
        assertEquals(1, db.backupDao().getAllWorkoutExercises().size)
        assertEquals(7, db.backupDao().getAllWeeklySchedules().size)
        assertEquals(1, db.backupDao().getAllSessions().size)
        assertEquals(3, db.backupDao().getAllSessionSets().size)

        // Backup válido com FKs nulas em histórico (plano/treino/exercício excluídos) e 0 planos ativos
        val snapshotOnlyJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMs": 100,
              "workoutSessions": [
                {"id": 1, "planId": null, "workoutTemplateId": null, "workoutNameSnapshot": "Antigo", "startedAtEpochMs": 10, "finishedAtEpochMs": 20}
              ],
              "sessionSets": [
                {"id": 1, "sessionId": 1, "exerciseId": null, "exerciseNameSnapshot": "Supino", "equipmentNameSnapshot": "Barra", "exerciseOrderIndex": 0, "setNumber": 1, "weightKg": 60.0, "reps": 10, "completed": true, "completedAtEpochMs": 15}
              ]
            }
        """.trimIndent()
        backupRepo.restoreFromJson(snapshotOnlyJson)
        assertEquals(1, db.backupDao().getAllSessions().size)
        assertEquals(1, db.backupDao().getAllSessionSets().size)
    }

    @Test
    fun restoreInvalidPayloads_rejectsAllValidationBranchesWithoutCorruptingData() = runTest {
        val eqId = catalogRepo.createEquipment("Polia Alta", EquipmentCategory.CABLE)
        catalogRepo.createExercise("Puxada", MuscleGroup.BACK, null, eqId)

        val invalidInputs = listOf(
            "",
            "   ",
            "{ not valid json",
            """{"schemaVersion":99,"exportedAtEpochMs":1}""",
            // Aparelho duplicado / sem nome
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[{"id":1,"name":"A","category":"CABLE"},{"id":1,"name":"B","category":"CABLE"}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[{"id":1,"name":"   ","category":"CABLE"}]}""",
            // Exercício duplicado / FK aparelho inexistente / sem nome
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[{"id":1,"name":"A","category":"CABLE"}],"exercises":[{"id":1,"name":"X","primaryMuscle":"CHEST","equipmentId":1},{"id":1,"name":"Y","primaryMuscle":"CHEST","equipmentId":1}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[],"exercises":[{"id":1,"name":"X","primaryMuscle":"CHEST","equipmentId":999}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[{"id":1,"name":"A","category":"CABLE"}],"exercises":[{"id":1,"name":" ","primaryMuscle":"CHEST","equipmentId":1}]}""",
            // Planos duplicados / >1 plano ativo
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[{"id":1,"name":"A","isActive":false,"createdAtEpochMs":1},{"id":1,"name":"B","isActive":false,"createdAtEpochMs":2}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1},{"id":2,"name":"B","isActive":true,"createdAtEpochMs":2}]}""",
            // Templates duplicados / FK plano inexistente / sem nome
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1}],"workoutTemplates":[{"id":1,"planId":1,"name":"T1","orderIndex":0},{"id":1,"planId":1,"name":"T2","orderIndex":1}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[],"workoutTemplates":[{"id":1,"planId":99,"name":"T1","orderIndex":0}]}""",
            // WorkoutExercise duplicado / FK inválida / bounds inválidos
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[{"id":1,"name":"A","category":"CABLE"}],"exercises":[{"id":1,"name":"X","primaryMuscle":"CHEST","equipmentId":1}],"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1}],"workoutTemplates":[{"id":1,"planId":1,"name":"T1","orderIndex":0}],"workoutExercises":[{"id":1,"workoutTemplateId":1,"exerciseId":1,"orderIndex":0,"targetSets":3,"targetReps":10,"restSeconds":60},{"id":1,"workoutTemplateId":1,"exerciseId":1,"orderIndex":1,"targetSets":3,"targetReps":10,"restSeconds":60}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"equipment":[{"id":1,"name":"A","category":"CABLE"}],"exercises":[{"id":1,"name":"X","primaryMuscle":"CHEST","equipmentId":1}],"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1}],"workoutTemplates":[{"id":1,"planId":1,"name":"T1","orderIndex":0}],"workoutExercises":[{"id":1,"workoutTemplateId":1,"exerciseId":1,"orderIndex":0,"targetSets":0,"targetReps":10,"restSeconds":60}]}""",
            // WeeklySchedule duplicado / dayOfWeek inválido / templateId inválido
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1}],"weeklySchedule":[{"planId":1,"dayOfWeek":1,"workoutTemplateId":null},{"planId":1,"dayOfWeek":1,"workoutTemplateId":null}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1}],"weeklySchedule":[{"planId":1,"dayOfWeek":8,"workoutTemplateId":null}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"trainingPlans":[{"id":1,"name":"A","isActive":true,"createdAtEpochMs":1}],"weeklySchedule":[{"planId":1,"dayOfWeek":1,"workoutTemplateId":999}]}""",
            // Sessões duplicadas / FK inválida
            """{"schemaVersion":1,"exportedAtEpochMs":1,"workoutSessions":[{"id":1,"workoutNameSnapshot":"T","startedAtEpochMs":1},{"id":1,"workoutNameSnapshot":"T","startedAtEpochMs":2}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"workoutSessions":[{"id":1,"planId":999,"workoutNameSnapshot":"T","startedAtEpochMs":1}]}""",
            // Séries duplicadas / FK sessão inválida / peso negativo
            """{"schemaVersion":1,"exportedAtEpochMs":1,"workoutSessions":[{"id":1,"workoutNameSnapshot":"T","startedAtEpochMs":1}],"sessionSets":[{"id":1,"sessionId":1,"exerciseNameSnapshot":"X","equipmentNameSnapshot":"Y","exerciseOrderIndex":0,"setNumber":1,"weightKg":10.0,"reps":10},{"id":1,"sessionId":1,"exerciseNameSnapshot":"X","equipmentNameSnapshot":"Y","exerciseOrderIndex":0,"setNumber":2,"weightKg":10.0,"reps":10}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"workoutSessions":[],"sessionSets":[{"id":1,"sessionId":99,"exerciseNameSnapshot":"X","equipmentNameSnapshot":"Y","exerciseOrderIndex":0,"setNumber":1,"weightKg":10.0,"reps":10}]}""",
            """{"schemaVersion":1,"exportedAtEpochMs":1,"workoutSessions":[{"id":1,"workoutNameSnapshot":"T","startedAtEpochMs":1}],"sessionSets":[{"id":1,"sessionId":1,"exerciseNameSnapshot":"X","equipmentNameSnapshot":"Y","exerciseOrderIndex":0,"setNumber":1,"weightKg":-5.0,"reps":10}]}"""
        )

        for (badInput in invalidInputs) {
            try {
                backupRepo.restoreFromJson(badInput)
                fail("Deveria rejeitar backup inválido: $badInput")
            } catch (_: BackupValidationException) {
                // esperado
            }
        }

        assertEquals(1, db.equipmentDao().getAll().size)
        assertEquals(1, db.exerciseDao().getAll().size)
    }
}
