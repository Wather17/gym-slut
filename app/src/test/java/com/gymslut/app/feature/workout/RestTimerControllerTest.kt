package com.gymslut.app.feature.workout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerControllerTest {

    @Test
    fun startWithZeroOrNegativeSeconds_doesNotActivateAndNoOpsWhenInactive() = runTest {
        val nowMs = 1_000L
        val controller = RestTimerController(backgroundScope, ElapsedTimeProvider { nowMs })

        controller.start("Supino", 0)
        assertFalse(controller.state.value.isActive)
        controller.start("Supino", -10)
        assertFalse(controller.state.value.isActive)

        // No-ops quando inativo
        controller.addSeconds(15)
        controller.pause()
        controller.resume()
        controller.skip()
        controller.syncRemaining()
        assertFalse(controller.state.value.isActive)
    }

    @Test
    fun pausedAdjustmentsReplaceAndPauseAlreadyExpired_handleEdgeCases() = runTest {
        var nowMs = 1_000L
        val controller = RestTimerController(backgroundScope, ElapsedTimeProvider { nowMs })

        val finished = mutableListOf<String>()
        val collectJob = backgroundScope.launch {
            controller.finishedEvents.collect { finished += it }
        }
        runCurrent()

        controller.start("Supino Reto", 30)
        assertTrue(controller.state.value.isActive)
        assertEquals(30, controller.state.value.remainingSeconds)

        // Substituir timer ativo
        controller.start("Remada Curvada", 20)
        assertEquals("Remada Curvada", controller.state.value.exerciseName)
        assertEquals(20, controller.state.value.remainingSeconds)

        controller.pause()
        controller.pause() // idempotente quando já pausado
        assertTrue(controller.state.value.isPaused)

        // Ajustar segundos enquanto pausado
        controller.addSeconds(15)
        assertEquals(35, controller.state.value.remainingSeconds)
        controller.addSeconds(-15)
        assertEquals(20, controller.state.value.remainingSeconds)

        // Subtrair até <= 0 enquanto pausado encerra sem disparar alerta
        controller.addSeconds(-30)
        assertFalse(controller.state.value.isActive)
        assertTrue(finished.isEmpty())

        // Pausar quando o relógio já ultrapassou o tempo alvo emite finishedEvents
        controller.start("Agachamento", 5)
        nowMs += 6_000L
        controller.pause()
        runCurrent()
        assertFalse(controller.state.value.isActive)
        assertEquals(listOf("Agachamento"), finished)

        // Cancelar limpa estado sem emitir evento
        controller.start("Panturrilha", 15)
        controller.resume() // idempotente quando já rodando
        controller.cancel()
        assertFalse(controller.state.value.isActive)
        assertEquals(1, finished.size)

        collectJob.cancel()
    }

    @Test
    fun countdownReachingZero_emitsFinishedEvent() = runTest {
        var nowMs = 5_000L
        val controller = RestTimerController(backgroundScope, ElapsedTimeProvider { nowMs })

        val finished = mutableListOf<String>()
        val collectJob = backgroundScope.launch {
            controller.finishedEvents.collect { finished += it }
        }
        runCurrent()

        controller.start("Leg Press 45°", 5)
        nowMs += 5_000L
        advanceTimeBy(5_500L)
        runCurrent()

        assertFalse(controller.state.value.isActive)
        assertEquals(listOf("Leg Press 45°"), finished)
        collectJob.cancel()
    }
}
