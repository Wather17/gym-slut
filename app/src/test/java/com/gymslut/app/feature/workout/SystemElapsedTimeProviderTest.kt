package com.gymslut.app.feature.workout

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemElapsedTimeProviderTest {

    @Test
    fun elapsedRealtimeMs_returnsNonNegativeValue() {
        val provider = SystemElapsedTimeProvider()
        assertTrue(provider.elapsedRealtimeMs() >= 0L)
    }
}
