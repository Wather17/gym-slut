package com.gymslut.app.feature.workout

import com.gymslut.app.core.notification.RestAlertNotifier
import com.gymslut.app.core.notification.RestTimerServiceController
import com.gymslut.app.core.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador singleton do timer de descanso, compartilhado entre a UI do treino ativo
 * e o Foreground Service para sobreviver à troca de telas e tela bloqueada.
 */
@Singleton
class RestTimerSessionManager internal constructor(
    elapsedTimeProvider: ElapsedTimeProvider,
    private val preferencesRepository: PreferencesRepository,
    private val restAlertNotifier: RestAlertNotifier,
    private val serviceController: RestTimerServiceController,
    scope: CoroutineScope
) {

    @Inject
    constructor(
        elapsedTimeProvider: ElapsedTimeProvider,
        preferencesRepository: PreferencesRepository,
        restAlertNotifier: RestAlertNotifier,
        serviceController: RestTimerServiceController
    ) : this(
        elapsedTimeProvider = elapsedTimeProvider,
        preferencesRepository = preferencesRepository,
        restAlertNotifier = restAlertNotifier,
        serviceController = serviceController,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    private val controller = RestTimerController(scope, elapsedTimeProvider)

    val state: StateFlow<RestTimerState> = controller.state

    init {
        scope.launch {
            controller.finishedEvents.collect { exerciseName ->
                serviceController.stop()
                val vibrate = preferencesRepository.workoutPreferences.first().restTimerVibrationEnabled
                restAlertNotifier.notifyRestFinished(exerciseName, vibrate = vibrate)
            }
        }
    }

    fun start(exerciseName: String, durationSeconds: Int) {
        if (durationSeconds <= 0) return
        controller.start(exerciseName, durationSeconds)
        serviceController.start()
    }

    fun addSeconds(deltaSeconds: Int) {
        val wasActive = state.value.isActive
        controller.addSeconds(deltaSeconds)
        if (wasActive && !state.value.isActive) {
            serviceController.stop()
        }
    }

    fun togglePause() {
        if (state.value.isPaused) {
            controller.resume()
        } else {
            controller.pause()
        }
    }

    fun skip() {
        controller.skip()
        serviceController.stop()
    }

    fun cancel() {
        controller.cancel()
        serviceController.stop()
        restAlertNotifier.cancelNotification()
    }
}
