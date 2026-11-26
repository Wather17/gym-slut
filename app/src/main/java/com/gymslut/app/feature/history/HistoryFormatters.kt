package com.gymslut.app.feature.history

import com.gymslut.app.data.local.entity.SessionSetEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object HistoryFormatters {

    private val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm", ptBrLocale)

    fun formatEpochMs(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(epochMs).atZone(zoneId).format(dateTimeFormatter)
    }

    fun formatDuration(startedAtEpochMs: Long, finishedAtEpochMs: Long): String {
        val totalMinutes = ((finishedAtEpochMs - startedAtEpochMs).coerceAtLeast(0L) / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            totalMinutes > 0 -> "${totalMinutes} min"
            else -> "< 1 min"
        }
    }

    fun calculateTotalVolumeKg(sets: List<SessionSetEntity>): Double {
        return sets.filter { it.completed }.sumOf { it.weightKg * it.reps }
    }

    fun formatKg(value: Double): String {
        return if (value % 1.0 == 0.0) {
            "${value.toLong()} kg"
        } else {
            String.format(ptBrLocale, "%.1f kg", value)
        }
    }
}
