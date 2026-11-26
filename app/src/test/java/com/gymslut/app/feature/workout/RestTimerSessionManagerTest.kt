package com.gymslut.app.feature.workout

import com.gymslut.app.core.notification.RestAlertNotifier
import com.gymslut.app.core.notification.RestTimerServiceController
import com.gymslut.app.core.preferences.PreferencesRepository
import com.gymslut.app.core.preferences.ThemeMode
import com.gymslut.app.core.preferences.WorkoutPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerSessionManagerTest {

    @Test
    fun startControlsExpiryAndCancel_manageServiceLifecycleAndAlerts() = runTest {
        var nowMs = 1_000L
        val prefs = FakePrefs()
        val notifier = FakeNotifier()
        val serviceController = FakeServiceController()

        val manager = RestTimerSessionManager(
            elapsedTimeProvider = ElapsedTimeProvider { nowMs },
            preferencesRepository = prefs,
            restAlertNotifier = notifier,
            serviceController = serviceController,
            scope = backgroundScope
        )

        // Duração <= 0 não inicia timer nem serviço
        manager.start("Supino", 0)
        assertFalse(manager.state.value.isActive)
        assertEquals(0, serviceController.startCalls)

        // Iniciar ativa timer e serviço
        manager.start("Supino Reto", 30)
        runCurrent()
        assertTrue(manager.state.value.isActive)
        assertEquals(1, serviceController.startCalls)

        // Pausar, adicionar +15s, subtrair até zerar -> para serviço sem alerta
        manager.togglePause()
        assertTrue(manager.state.value.isPaused)
        manager.addSeconds(15)
        assertEquals(45, manager.state.value.remainingSeconds)
        manager.addSeconds(-60)
        assertFalse(manager.state.value.isActive)
        assertEquals(1, serviceController.stopCalls)
        assertTrue(notifier.alerts.isEmpty())

        // Iniciar, retomar após pausa e pular -> para serviço sem alerta
        manager.start("Remada Máquina", 20)
        manager.togglePause()
        manager.togglePause()
        assertFalse(manager.state.value.isPaused)
        manager.skip()
        assertFalse(manager.state.value.isActive)
        assertEquals(2, serviceController.stopCalls)

        // Expiração natural respeita restTimerVibrationEnabled = false e para serviço
        prefs.setRestTimerVibrationEnabled(false)
        manager.start("Leg Press 45°", 3)
        runCurrent()
        nowMs += 3_000L
        advanceTimeBy(3_500L)
        runCurrent()
        assertFalse(manager.state.value.isActive)
        assertEquals(3, serviceController.stopCalls)
        assertEquals(listOf("Leg Press 45°" to false), notifier.alerts)

        // Cancelar encerra timer, para serviço e cancela notificação
        manager.start("Hack Squat", 45)
        manager.cancel()
        assertFalse(manager.state.value.isActive)
        assertEquals(4, serviceController.stopCalls)
        assertEquals(1, notifier.cancelCalls)
    }

    private class FakeServiceController : RestTimerServiceController {
        var startCalls = 0
        var stopCalls = 0
        override fun start() {
            startCalls++
        }

        override fun stop() {
            stopCalls++
        }
    }

    private class FakeNotifier : RestAlertNotifier {
        val alerts = mutableListOf<Pair<String, Boolean>>()
        var cancelCalls = 0
        override fun notifyRestFinished(exerciseName: String, vibrate: Boolean) {
            alerts += exerciseName to vibrate
        }

        override fun cancelNotification() {
            cancelCalls++
        }
    }

    private class FakePrefs : PreferencesRepository {
        private val flow = MutableStateFlow(WorkoutPreferences())
        override val themeMode: Flow<ThemeMode> = flowOf(ThemeMode.DARK)
        override val workoutPreferences: Flow<WorkoutPreferences> = flow
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setKeepScreenOn(enabled: Boolean) = Unit
        override suspend fun setHapticFeedbackEnabled(enabled: Boolean) = Unit
        override suspend fun setAutoStartRestTimer(enabled: Boolean) = Unit
        override suspend fun setRestTimerVibrationEnabled(enabled: Boolean) {
            flow.value = flow.value.copy(restTimerVibrationEnabled = enabled)
        }
    }
}
