package com.gymslut.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymslut.app.data.local.CatalogSeedData
import com.gymslut.app.data.local.GymSlutDatabase
import com.gymslut.app.data.local.dao.BackupDao
import com.gymslut.app.data.local.dao.EquipmentDao
import com.gymslut.app.data.local.dao.ExerciseDao
import com.gymslut.app.data.local.dao.PlanDao
import com.gymslut.app.data.local.dao.ScheduleDao
import com.gymslut.app.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val SEED_PREFS_NAME = "gym_slut_seed_prefs"
    private const val KEY_CATALOG_SEED_VERSION = "catalog_seed_version"

    @Provides
    @Singleton
    fun provideGymSlutDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<GymSlutDatabase>
    ): GymSlutDatabase {
        return Room.databaseBuilder(
            context,
            GymSlutDatabase::class.java,
            "gym_slut.db"
        )
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        val prefs = context.getSharedPreferences(SEED_PREFS_NAME, Context.MODE_PRIVATE)
                        val appliedVersion = prefs.getInt(KEY_CATALOG_SEED_VERSION, 0)
                        if (appliedVersion < CatalogSeedData.SEED_VERSION) {
                            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                                val database = databaseProvider.get()
                                CatalogSeedData.seedIfNeeded(
                                    equipmentDao = database.equipmentDao(),
                                    exerciseDao = database.exerciseDao()
                                )
                                prefs.edit()
                                    .putInt(KEY_CATALOG_SEED_VERSION, CatalogSeedData.SEED_VERSION)
                                    .apply()
                            }
                        }
                    }
                }
            )
            .build()
    }

    @Provides
    fun provideEquipmentDao(db: GymSlutDatabase): EquipmentDao = db.equipmentDao()

    @Provides
    fun provideExerciseDao(db: GymSlutDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun providePlanDao(db: GymSlutDatabase): PlanDao = db.planDao()

    @Provides
    fun provideScheduleDao(db: GymSlutDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideSessionDao(db: GymSlutDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideBackupDao(db: GymSlutDatabase): BackupDao = db.backupDao()
}
