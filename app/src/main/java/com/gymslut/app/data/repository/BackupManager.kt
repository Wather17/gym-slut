package com.gymslut.app.data.repository

import com.gymslut.app.data.local.dao.BackupDao
import com.gymslut.app.data.model.BackupPayload
import com.gymslut.app.data.model.toBackupDto
import com.gymslut.app.data.model.toEntity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

class BackupValidationException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

/**
 * Contrato de exportação, validação e restauração atômica do banco em JSON.
 */
interface BackupRepository {
    suspend fun exportToJson(exportedAtEpochMs: Long = System.currentTimeMillis()): String
    fun validateJson(jsonString: String): BackupPayload
    suspend fun restoreFromJson(jsonString: String)
}

@Singleton
class RoomBackupRepository @Inject constructor(
    private val backupDao: BackupDao
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    override suspend fun exportToJson(exportedAtEpochMs: Long): String {
        val snapshot = backupDao.readFullSnapshot()
        val payload = BackupPayload(
            schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMs = exportedAtEpochMs,
            equipment = snapshot.equipment.map { it.toBackupDto() },
            exercises = snapshot.exercises.map { it.toBackupDto() },
            trainingPlans = snapshot.plans.map { it.toBackupDto() },
            workoutTemplates = snapshot.templates.map { it.toBackupDto() },
            workoutExercises = snapshot.workoutExercises.map { it.toBackupDto() },
            weeklySchedule = snapshot.weeklySchedule.map { it.toBackupDto() },
            workoutSessions = snapshot.sessions.map { it.toBackupDto() },
            sessionSets = snapshot.sessionSets.map { it.toBackupDto() }
        )
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    override fun validateJson(jsonString: String): BackupPayload {
        if (jsonString.isBlank()) {
            throw BackupValidationException("O arquivo de backup está vazio.")
        }

        val payload = try {
            json.decodeFromString(BackupPayload.serializer(), jsonString)
        } catch (e: SerializationException) {
            throw BackupValidationException("Arquivo JSON inválido ou incompatível.", e)
        } catch (e: IllegalArgumentException) {
            throw BackupValidationException("Estrutura de backup inválida.", e)
        }

        if (payload.schemaVersion != BackupPayload.CURRENT_SCHEMA_VERSION) {
            throw BackupValidationException(
                "Versão de backup não suportada (${payload.schemaVersion}). Esperado: ${BackupPayload.CURRENT_SCHEMA_VERSION}."
            )
        }

        val equipmentIds = payload.equipment.map { it.id }.toSet()
        if (equipmentIds.size != payload.equipment.size) {
            throw BackupValidationException("Backup inválido: IDs de aparelhos duplicados.")
        }
        if (payload.equipment.any { it.name.isBlank() }) {
            throw BackupValidationException("Backup inválido: aparelho sem nome.")
        }
        val equipmentNames = payload.equipment.map { it.name.trim().lowercase() }.toSet()
        if (equipmentNames.size != payload.equipment.size) {
            throw BackupValidationException("Backup inválido: nomes de aparelhos duplicados.")
        }

        val exerciseIds = payload.exercises.map { it.id }.toSet()
        if (exerciseIds.size != payload.exercises.size) {
            throw BackupValidationException("Backup inválido: IDs de exercícios duplicados.")
        }
        if (payload.exercises.any { it.equipmentId !in equipmentIds || it.name.isBlank() }) {
            throw BackupValidationException("Backup inválido: exercício com aparelho inexistente ou sem nome.")
        }

        val planIds = payload.trainingPlans.map { it.id }.toSet()
        if (planIds.size != payload.trainingPlans.size) {
            throw BackupValidationException("Backup inválido: IDs de planos duplicados.")
        }
        if (payload.trainingPlans.any { it.name.isBlank() }) {
            throw BackupValidationException("Backup inválido: plano sem nome.")
        }
        if (payload.trainingPlans.count { it.isActive } > 1) {
            throw BackupValidationException("Backup inválido: há mais de um plano ativo.")
        }

        val templateIds = payload.workoutTemplates.map { it.id }.toSet()
        if (templateIds.size != payload.workoutTemplates.size) {
            throw BackupValidationException("Backup inválido: IDs de treinos duplicados.")
        }
        if (payload.workoutTemplates.any { it.planId !in planIds || it.name.isBlank() || it.orderIndex < 0 }) {
            throw BackupValidationException("Backup inválido: treino vinculado a plano inexistente ou inválido.")
        }
        val templatePlanById = payload.workoutTemplates.associate { it.id to it.planId }

        val workoutExerciseIds = payload.workoutExercises.map { it.id }.toSet()
        if (workoutExerciseIds.size != payload.workoutExercises.size) {
            throw BackupValidationException("Backup inválido: IDs de exercícios da ficha duplicados.")
        }
        if (payload.workoutExercises.any {
                it.workoutTemplateId !in templateIds ||
                    it.exerciseId !in exerciseIds ||
                    it.orderIndex < 0 ||
                    it.targetSets < 1 ||
                    it.targetReps < 1 ||
                    it.restSeconds < 0
            }
        ) {
            throw BackupValidationException("Backup inválido: prescrição de treino com referência ou valores inválidos.")
        }

        val scheduleKeys = payload.weeklySchedule.map { it.planId to it.dayOfWeek }.toSet()
        if (scheduleKeys.size != payload.weeklySchedule.size) {
            throw BackupValidationException("Backup inválido: cronograma semanal duplicado.")
        }
        if (payload.weeklySchedule.any {
                it.planId !in planIds ||
                    it.dayOfWeek !in 1..7 ||
                    (it.workoutTemplateId != null && templatePlanById[it.workoutTemplateId] != it.planId)
            }
        ) {
            throw BackupValidationException("Backup inválido: cronograma semanal inconsistente.")
        }

        val sessionIds = payload.workoutSessions.map { it.id }.toSet()
        if (sessionIds.size != payload.workoutSessions.size) {
            throw BackupValidationException("Backup inválido: IDs de sessões duplicados.")
        }
        if (payload.workoutSessions.any {
                it.workoutNameSnapshot.isBlank() ||
                    (it.finishedAtEpochMs != null && it.finishedAtEpochMs < it.startedAtEpochMs) ||
                    (it.planId != null && it.planId !in planIds) ||
                    (it.workoutTemplateId != null && it.workoutTemplateId !in templateIds) ||
                    (it.planId != null && it.workoutTemplateId != null && templatePlanById[it.workoutTemplateId] != it.planId)
            }
        ) {
            throw BackupValidationException("Backup inválido: sessão com referência ou dados inválidos.")
        }

        val setIds = payload.sessionSets.map { it.id }.toSet()
        if (setIds.size != payload.sessionSets.size) {
            throw BackupValidationException("Backup inválido: IDs de séries duplicados.")
        }
        if (payload.sessionSets.any {
                it.sessionId !in sessionIds ||
                    (it.exerciseId != null && it.exerciseId !in exerciseIds) ||
                    it.exerciseNameSnapshot.isBlank() ||
                    it.exerciseOrderIndex < 0 ||
                    it.setNumber < 1 ||
                    it.weightKg < 0.0 ||
                    it.reps < 0
            }
        ) {
            throw BackupValidationException("Backup inválido: série com referência ou valores inválidos.")
        }

        return payload
    }

    override suspend fun restoreFromJson(jsonString: String) {
        val payload = validateJson(jsonString)
        backupDao.replaceAllData(
            equipment = payload.equipment.map { it.toEntity() },
            exercises = payload.exercises.map { it.toEntity() },
            plans = payload.trainingPlans.map { it.toEntity() },
            templates = payload.workoutTemplates.map { it.toEntity() },
            workoutExercises = payload.workoutExercises.map { it.toEntity() },
            weeklySchedule = payload.weeklySchedule.map { it.toEntity() },
            sessions = payload.workoutSessions.map { it.toEntity() },
            sessionSets = payload.sessionSets.map { it.toEntity() }
        )
    }
}
