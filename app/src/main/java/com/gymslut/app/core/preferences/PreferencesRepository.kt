package com.gymslut.app.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

data class WorkoutPreferences(
    val keepScreenOn: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val autoStartRestTimer: Boolean = true,
    val restTimerVibrationEnabled: Boolean = true
)

/**
 * Repositório de preferências do usuário persistidas via DataStore Preferences.
 */
interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val workoutPreferences: Flow<WorkoutPreferences>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setHapticFeedbackEnabled(enabled: Boolean)
    suspend fun setAutoStartRestTimer(enabled: Boolean)
    suspend fun setRestTimerVibrationEnabled(enabled: Boolean)
}

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    override val themeMode: Flow<ThemeMode> =
        context.gymSlutDataStore.data.map { prefs ->
            ThemeMode.fromStorageValue(prefs[THEME_MODE_KEY])
        }

    override val workoutPreferences: Flow<WorkoutPreferences> =
        context.gymSlutDataStore.data.map { prefs ->
            WorkoutPreferences(
                keepScreenOn = prefs[KEEP_SCREEN_ON_KEY] ?: true,
                hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK_KEY] ?: true,
                autoStartRestTimer = prefs[AUTO_START_REST_TIMER_KEY] ?: true,
                restTimerVibrationEnabled = prefs[REST_TIMER_VIBRATION_KEY] ?: true
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.gymSlutDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        context.gymSlutDataStore.edit { prefs ->
            prefs[KEEP_SCREEN_ON_KEY] = enabled
        }
    }

    override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.gymSlutDataStore.edit { prefs ->
            prefs[HAPTIC_FEEDBACK_KEY] = enabled
        }
    }

    override suspend fun setAutoStartRestTimer(enabled: Boolean) {
        context.gymSlutDataStore.edit { prefs ->
            prefs[AUTO_START_REST_TIMER_KEY] = enabled
        }
    }

    override suspend fun setRestTimerVibrationEnabled(enabled: Boolean) {
        context.gymSlutDataStore.edit { prefs ->
            prefs[REST_TIMER_VIBRATION_KEY] = enabled
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback_enabled")
        val AUTO_START_REST_TIMER_KEY = booleanPreferencesKey("auto_start_rest_timer")
        val REST_TIMER_VIBRATION_KEY = booleanPreferencesKey("rest_timer_vibration_enabled")
    }
}
