package com.gymslut.app.data.model

import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutExerciseEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMs: Long,
    val equipment: List<EquipmentBackupDto> = emptyList(),
    val exercises: List<ExerciseBackupDto> = emptyList(),
    val trainingPlans: List<TrainingPlanBackupDto> = emptyList(),
    val workoutTemplates: List<WorkoutTemplateBackupDto> = emptyList(),
    val workoutExercises: List<WorkoutExerciseBackupDto> = emptyList(),
    val weeklySchedule: List<WeeklyScheduleBackupDto> = emptyList(),
    val workoutSessions: List<WorkoutSessionBackupDto> = emptyList(),
    val sessionSets: List<SessionSetBackupDto> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class EquipmentBackupDto(
    val id: Long,
    val name: String,
    val category: EquipmentCategory,
    val notes: String = "",
    val isCustom: Boolean = false
)

@Serializable
data class ExerciseBackupDto(
    val id: Long,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscle: MuscleGroup? = null,
    val equipmentId: Long,
    val notes: String = "",
    val isCustom: Boolean = false
)

@Serializable
data class TrainingPlanBackupDto(
    val id: Long,
    val name: String,
    val isActive: Boolean = false,
    val createdAtEpochMs: Long
)

@Serializable
data class WorkoutTemplateBackupDto(
    val id: Long,
    val planId: Long,
    val name: String,
    val orderIndex: Int
)

@Serializable
data class WorkoutExerciseBackupDto(
    val id: Long,
    val workoutTemplateId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int
)

@Serializable
data class WeeklyScheduleBackupDto(
    val planId: Long,
    val dayOfWeek: Int,
    val workoutTemplateId: Long? = null
)

@Serializable
data class WorkoutSessionBackupDto(
    val id: Long,
    val planId: Long? = null,
    val workoutTemplateId: Long? = null,
    val workoutNameSnapshot: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null
)

@Serializable
data class SessionSetBackupDto(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long? = null,
    val exerciseNameSnapshot: String,
    val equipmentNameSnapshot: String,
    val exerciseOrderIndex: Int,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val completed: Boolean = false,
    val completedAtEpochMs: Long? = null
)

fun EquipmentEntity.toBackupDto() = EquipmentBackupDto(id, name, category, notes, isCustom)
fun EquipmentBackupDto.toEntity() = EquipmentEntity(id, name, category, notes, isCustom)

fun ExerciseEntity.toBackupDto() =
    ExerciseBackupDto(id, name, primaryMuscle, secondaryMuscle, equipmentId, notes, isCustom)

fun ExerciseBackupDto.toEntity() =
    ExerciseEntity(id, name, primaryMuscle, secondaryMuscle, equipmentId, notes, isCustom)

fun TrainingPlanEntity.toBackupDto() = TrainingPlanBackupDto(id, name, isActive, createdAtEpochMs)
fun TrainingPlanBackupDto.toEntity() = TrainingPlanEntity(id, name, isActive, createdAtEpochMs)

fun WorkoutTemplateEntity.toBackupDto() = WorkoutTemplateBackupDto(id, planId, name, orderIndex)
fun WorkoutTemplateBackupDto.toEntity() = WorkoutTemplateEntity(id, planId, name, orderIndex)

fun WorkoutExerciseEntity.toBackupDto() =
    WorkoutExerciseBackupDto(id, workoutTemplateId, exerciseId, orderIndex, targetSets, targetReps, restSeconds)

fun WorkoutExerciseBackupDto.toEntity() =
    WorkoutExerciseEntity(id, workoutTemplateId, exerciseId, orderIndex, targetSets, targetReps, restSeconds)

fun WeeklyScheduleEntity.toBackupDto() = WeeklyScheduleBackupDto(planId, dayOfWeek, workoutTemplateId)
fun WeeklyScheduleBackupDto.toEntity() = WeeklyScheduleEntity(planId, dayOfWeek, workoutTemplateId)

fun WorkoutSessionEntity.toBackupDto() =
    WorkoutSessionBackupDto(id, planId, workoutTemplateId, workoutNameSnapshot, startedAtEpochMs, finishedAtEpochMs)

fun WorkoutSessionBackupDto.toEntity() =
    WorkoutSessionEntity(id, planId, workoutTemplateId, workoutNameSnapshot, startedAtEpochMs, finishedAtEpochMs)

fun SessionSetEntity.toBackupDto() = SessionSetBackupDto(
    id, sessionId, exerciseId, exerciseNameSnapshot, equipmentNameSnapshot,
    exerciseOrderIndex, setNumber, weightKg, reps, completed, completedAtEpochMs
)

fun SessionSetBackupDto.toEntity() = SessionSetEntity(
    id, sessionId, exerciseId, exerciseNameSnapshot, equipmentNameSnapshot,
    exerciseOrderIndex, setNumber, weightKg, reps, completed, completedAtEpochMs
)
