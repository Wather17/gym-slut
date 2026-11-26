package com.gymslut.app.di

import com.gymslut.app.core.preferences.DataStorePreferencesRepository
import com.gymslut.app.core.preferences.PreferencesRepository
import com.gymslut.app.data.repository.CatalogRepository
import com.gymslut.app.data.repository.PlanRepository
import com.gymslut.app.data.repository.RoomCatalogRepository
import com.gymslut.app.data.repository.RoomPlanRepository
import com.gymslut.app.data.repository.RoomSessionRepository
import com.gymslut.app.data.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: DataStorePreferencesRepository
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        impl: RoomCatalogRepository
    ): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(
        impl: RoomPlanRepository
    ): PlanRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: RoomSessionRepository
    ): SessionRepository
}
