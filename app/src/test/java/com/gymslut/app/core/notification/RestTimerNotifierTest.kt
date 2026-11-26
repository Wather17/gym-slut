package com.gymslut.app.core.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RestTimerNotifierTest {

    @Test
    fun api35WithoutPermission_doesNotThrowAndSkipsNotification() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifier = AndroidRestTimerNotifier(app)

        notifier.notifyRestFinished("Supino Reto")

        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(0, shadowOf(nm).allNotifications.size)
    }

    @Test
    fun api35WithPermission_createsChannelPostsAndCancelsNotification() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifier = AndroidRestTimerNotifier(app)

        notifier.notifyRestFinished("Agachamento Livre")

        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel("rest_timer_channel")
        assertNotNull(channel)
        assertEquals(1, shadowOf(nm).allNotifications.size)
        assertNotNull(shadowOf(nm).allNotifications.first().contentIntent)

        // Chamar segunda vez reutiliza canal existente sem duplicar erro (também com vibrate = false)
        notifier.notifyRestFinished("Leg Press", vibrate = false)
        assertEquals(1, shadowOf(nm).allNotifications.size)

        notifier.cancelNotification()
        assertEquals(0, shadowOf(nm).allNotifications.size)
    }

    @Test
    @Config(sdk = [28])
    fun api28LegacyVibratorAndNotification_worksWithoutRuntimePermission() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val notifier = AndroidRestTimerNotifier(app)

        notifier.notifyRestFinished("Rosca Direta")

        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(1, shadowOf(nm).allNotifications.size)
    }
}
