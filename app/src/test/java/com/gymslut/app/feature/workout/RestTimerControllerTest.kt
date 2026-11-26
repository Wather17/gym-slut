package com.gymslut.app.feature.workout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
    fun startWithZeroSeconds_doesNotActivateTimer() = runTest {
        var nowMs = 1_000L
        val controller = RestTimerController(backgroundScope, ElapsedTimeProvider { nowMs })

        controller.start("Supino", 0)
        assertFalse(controller.state.value.isActive)
    }

    @Test
    fun addSubtractPauseResumeAndSkip_updatesStateCorrectly() = runTest {
        var nowMs = 1_000L
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val controller = RestTimerController(scope.backgroundScope, ElapsedTimeProvider { nowMs })

        val finished = mutableListOf<String>()
        val collectJob = scope.backgroundScope.launch {
            controller.finishedEvents.collect { finished += it }
        }

        controller.start("Supino Reto", 30)
        assertTrue(controller.state.value.isActive)
        assertEquals(30, controller.state.value.remainingSeconds)

        controller.addSeconds(15)
        assertEquals(45, controller.state.value.remainingSeconds)

        controller.addSeconds(-15)
        assertEquals(30, controller.state.value.remainingSeconds)

        controller.pause()
        assertTrue(controller.state.value.isPaused)
        nowMs += 20_000L
        controller.syncRemaining()
        assertEquals(30, controller.state.value.remainingSeconds)

        controller.resume()
        assertFalse(controller.state.value.isPaused)

        // Pular não deve disparar evento de fim
        controller.skip()
        assertFalse(controller.state.value.isActive)
        assertTrue(finished.isEmpty())

        // -15s até zerar encerra sem negativo
        controller.start("Agachamento", 10)
        controller.addSeconds(-15)
        assertFalse(controller.state.value.isActive)
        assertTrue(finished.isEmpty())

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
