package com.gymslut.app.core.time

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provedor de data atual injetável para permitir testes determinísticos de cronograma.
 */
fun interface CurrentDateProvider {
    fun today(): LocalDate
}

@Singleton
class SystemCurrentDateProvider @Inject constructor() : CurrentDateProvider {
    override fun today(): LocalDate = LocalDate.now()
}

/**
 * Rótulos PT-BR para dias da semana ISO-8601 (1=Segunda .. 7=Domingo).
 */
object WeekdayLabels {
    private val fullNames = mapOf(
        1 to "Segunda-feira",
        2 to "Terça-feira",
        3 to "Quarta-feira",
        4 to "Quinta-feira",
        5 to "Sexta-feira",
        6 to "Sábado",
        7 to "Domingo"
    )

    private val shortNames = mapOf(
        1 to "Seg",
        2 to "Ter",
        3 to "Qua",
        4 to "Qui",
        5 to "Sex",
        6 to "Sáb",
        7 to "Dom"
    )

    fun fullName(dayOfWeek: Int): String = fullNames[dayOfWeek] ?: "Dia $dayOfWeek"

    fun shortName(dayOfWeek: Int): String = shortNames[dayOfWeek] ?: "$dayOfWeek"
}
