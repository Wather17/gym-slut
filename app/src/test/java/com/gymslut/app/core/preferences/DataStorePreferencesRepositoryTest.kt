package com.gymslut.app.core.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataStorePreferencesRepositoryTest {

    @Test
    fun themeMode_defaultsToDarkAndPersistsUpdates() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = DataStorePreferencesRepository(context)

        assertEquals(ThemeMode.DARK, repo.themeMode.first())

        repo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.first())

        repo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())

        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.themeMode.first())

        assertEquals(WorkoutPreferences(), repo.workoutPreferences.first())

        repo.setKeepScreenOn(false)
        repo.setHapticFeedbackEnabled(false)
        repo.setAutoStartRestTimer(false)
        repo.setRestTimerVibrationEnabled(false)
        assertEquals(
            WorkoutPreferences(
                keepScreenOn = false,
                hapticFeedbackEnabled = false,
                autoStartRestTimer = false,
                restTimerVibrationEnabled = false
            ),
            repo.workoutPreferences.first()
        )

        repo.setKeepScreenOn(true)
        repo.setHapticFeedbackEnabled(true)
        repo.setAutoStartRestTimer(true)
        repo.setRestTimerVibrationEnabled(true)
        assertEquals(WorkoutPreferences(), repo.workoutPreferences.first())
    }
}
