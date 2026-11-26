package com.gymslut.app.core.notification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controlador injetável para iniciar e encerrar o serviço em primeiro plano do timer de descanso.
 */
interface RestTimerServiceController {
    fun start()
    fun stop()
}

@Singleton
class AndroidRestTimerServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) : RestTimerServiceController {

    override fun start() {
        val intent = Intent(context, RestTimerForegroundService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: IllegalStateException) {
            // Android 12+ pode bloquear FGS se disparado fora de foreground; o timer segue ativo no processo.
        } catch (_: SecurityException) {
            // Fallback defensivo caso a permissão de FGS não esteja disponível no ambiente.
        }
    }

    override fun stop() {
        val intent = Intent(context, RestTimerForegroundService::class.java)
        context.stopService(intent)
    }
}
