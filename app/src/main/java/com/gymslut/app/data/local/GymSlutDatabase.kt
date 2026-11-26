package com.gymslut.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymslut.app.data.local.dao.BackupDao
import com.gymslut.app.data.local.dao.EquipmentDao
import com.gymslut.app.data.local.dao.ExerciseDao
import com.gymslut.app.data.local.dao.PlanDao
import com.gymslut.app.data.local.dao.ScheduleDao
import com.gymslut.app.data.local.dao.SessionDao
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutExerciseEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity

/**
 * Banco de dados Room offline-first do Gym Slut.
 */
@Database(
    entities = [
        EquipmentEntity::class,
        ExerciseEntity::class,
        TrainingPlanEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutExerciseEntity::class,
        WeeklyScheduleEntity::class,
        WorkoutSessionEntity::class,
        SessionSetEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GymSlutDatabase : RoomDatabase() {
    abstract fun equipmentDao(): EquipmentDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sessionDao(): SessionDao
    abstract fun backupDao(): BackupDao
}
