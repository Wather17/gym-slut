package com.gymslut.app.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gymSlutDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gym_slut_preferences"
)

/**
 * Repositório de preferências do usuário persistidas via DataStore Preferences.
 */
interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    override val themeMode: Flow<ThemeMode> =
        context.gymSlutDataStore.data.map { prefs ->
            ThemeMode.fromStorageValue(prefs[THEME_MODE_KEY])
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.gymSlutDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
