package com.gymslut.app.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.gymslut.app.MainActivity
import com.gymslut.app.R
import com.gymslut.app.feature.workout.RestTimerSessionManager
import com.gymslut.app.feature.workout.RestTimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Construtor da notificação contínua do timer de descanso (canal silencioso IMPORTANCE_LOW).
 */
object RestTimerOngoingNotificationBuilder {

    const val ONGOING_CHANNEL_ID = "rest_timer_ongoing_channel"
    const val ONGOING_NOTIFICATION_ID = 7002
    const val ACTION_ADD_15S = "com.gymslut.app.action.REST_TIMER_ADD_15S"
    const val ACTION_SKIP = "com.gymslut.app.action.REST_TIMER_SKIP"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (manager.getNotificationChannel(ONGOING_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ONGOING_CHANNEL_ID,
                context.getString(R.string.rest_ongoing_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun build(context: Context, state: RestTimerState): Notification {
        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            ONGOING_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add15Intent = PendingIntent.getService(
            context,
            ONGOING_NOTIFICATION_ID + 1,
            Intent(context, RestTimerForegroundService::class.java).setAction(ACTION_ADD_15S),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = PendingIntent.getService(
            context,
            ONGOING_NOTIFICATION_ID + 2,
            Intent(context, RestTimerForegroundService::class.java).setAction(ACTION_SKIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = formatCountdown(state.remainingSeconds)
        val statusPrefix = if (state.isPaused) "Descanso pausado" else "Descanso"

        return NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$statusPrefix • $timeText")
            .setContentText(state.exerciseName)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "+15s", add15Intent)
            .addAction(0, "Pular", skipIntent)
            .build()
    }

    fun formatCountdown(totalSeconds: Int): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val mins = clamped / 60
        val secs = clamped % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

/**
 * Serviço em primeiro plano que mantém o timer de descanso ativo com notificação contínua
 * mesmo com a tela bloqueada ou em outra aba do aplicativo.
 */
@AndroidEntryPoint
class RestTimerForegroundService : Service() {

    @Inject
    lateinit var sessionManager: RestTimerSessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            RestTimerOngoingNotificationBuilder.ACTION_ADD_15S -> sessionManager.addSeconds(15)
            RestTimerOngoingNotificationBuilder.ACTION_SKIP -> {
                sessionManager.skip()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val currentState = sessionManager.state.value
        if (!currentState.isActive) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = RestTimerOngoingNotificationBuilder.build(this, currentState)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            RestTimerOngoingNotificationBuilder.ONGOING_NOTIFICATION_ID,
            notification,
            serviceType
        )

        if (observeJob?.isActive != true) {
            observeJob = serviceScope.launch {
                sessionManager.state.collect { state ->
                        if (!state.isActive) {
                            stopSelf()
                        } else {
                            val updated = RestTimerOngoingNotificationBuilder.build(
                                this@RestTimerForegroundService,
                                state
                            )
                            try {
                                NotificationManagerCompat.from(this@RestTimerForegroundService)
                                    .notify(
                                        RestTimerOngoingNotificationBuilder.ONGOING_NOTIFICATION_ID,
                                        updated
                                    )
                            } catch (_: SecurityException) {
                                // Notificações podem estar negadas pelo usuário no Android 13+
                            }
                        }
                    }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
