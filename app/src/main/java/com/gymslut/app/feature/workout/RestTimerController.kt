package com.gymslut.app.feature.workout

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * Provedor de relógio monotônico injetável para contagem precisa em segundo plano e testes.
 */
fun interface ElapsedTimeProvider {
    fun elapsedRealtimeMs(): Long
}

@Singleton
class SystemElapsedTimeProvider @Inject constructor() : ElapsedTimeProvider {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}

data class RestTimerState(
    val isActive: Boolean = false,
    val exerciseName: String = "",
    val remainingSeconds: Int = 0,
    val isPaused: Boolean = false
)

/**
 * Controlador do timer de descanso baseado em timestamp alvo de relógio monotônico.
 */
class RestTimerController(
    private val scope: CoroutineScope,
    private val elapsedTimeProvider: ElapsedTimeProvider
) {
    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private val _finishedEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val finishedEvents: SharedFlow<String> = _finishedEvents.asSharedFlow()

    private var targetElapsedMs: Long = 0L
    private var pausedRemainingMs: Long = 0L
    private var tickerJob: Job? = null

    fun start(exerciseName: String, durationSeconds: Int) {
        if (durationSeconds <= 0) return
        tickerJob?.cancel()
        val durationMs = durationSeconds * 1_000L
        targetElapsedMs = elapsedTimeProvider.elapsedRealtimeMs() + durationMs
        pausedRemainingMs = durationMs
        _state.value = RestTimerState(
            isActive = true,
            exerciseName = exerciseName,
            remainingSeconds = durationSeconds,
            isPaused = false
        )
        launchTicker()
    }

    fun addSeconds(deltaSeconds: Int) {
        val current = _state.value
        if (!current.isActive) return
        val deltaMs = deltaSeconds * 1_000L
        if (current.isPaused) {
            val updatedMs = (pausedRemainingMs + deltaMs).coerceAtLeast(0L)
            if (updatedMs == 0L) {
                finishWithoutAlert()
            } else {
                pausedRemainingMs = updatedMs
                _state.value = current.copy(remainingSeconds = millisToCeilSeconds(updatedMs))
            }
        } else {
            targetElapsedMs += deltaMs
            syncRemaining()
        }
    }

    fun pause() {
        val current = _state.value
        if (!current.isActive || current.isPaused) return
        tickerJob?.cancel()
        pausedRemainingMs = (targetElapsedMs - elapsedTimeProvider.elapsedRealtimeMs()).coerceAtLeast(0L)
        if (pausedRemainingMs == 0L) {
            completeTimer()
            return
        }
        _state.value = current.copy(
            isPaused = true,
            remainingSeconds = millisToCeilSeconds(pausedRemainingMs)
        )
    }

    fun resume() {
        val current = _state.value
        if (!current.isActive || !current.isPaused) return
        targetElapsedMs = elapsedTimeProvider.elapsedRealtimeMs() + pausedRemainingMs
        _state.value = current.copy(isPaused = false)
        launchTicker()
    }

    fun skip() {
        finishWithoutAlert()
    }

    fun cancel() {
        finishWithoutAlert()
    }

    /**
     * Atualiza o tempo restante com base no relógio atual (útil também em testes determinísticos).
     */
    fun syncRemaining() {
        val current = _state.value
        if (!current.isActive || current.isPaused) return
        val remainingMs = targetElapsedMs - elapsedTimeProvider.elapsedRealtimeMs()
        if (remainingMs <= 0L) {
            completeTimer()
        } else {
            _state.value = current.copy(remainingSeconds = millisToCeilSeconds(remainingMs))
        }
    }

    private fun launchTicker() {
        tickerJob = scope.launch {
            while (isActive && _state.value.isActive && !_state.value.isPaused) {
                syncRemaining()
                if (!_state.value.isActive) break
                delay(250L)
            }
        }
    }

    private fun completeTimer() {
        val exerciseName = _state.value.exerciseName
        tickerJob?.cancel()
        _state.value = RestTimerState()
        _finishedEvents.tryEmit(exerciseName)
    }

    private fun finishWithoutAlert() {
        tickerJob?.cancel()
        _state.value = RestTimerState()
    }

    private fun millisToCeilSeconds(ms: Long): Int =
        ceil(ms / 1_000.0).toInt().coerceAtLeast(0)
}
