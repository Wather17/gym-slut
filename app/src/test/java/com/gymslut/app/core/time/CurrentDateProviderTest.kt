package com.gymslut.app.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class CurrentDateProviderTest {

    @Test
    fun weekdayLabels_coversAllIsoWeekdaysAndFallbacks() {
        val expectedFull = listOf(
            "Segunda-feira",
            "Terça-feira",
            "Quarta-feira",
            "Quinta-feira",
            "Sexta-feira",
            "Sábado",
            "Domingo"
        )
        val expectedShort = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

        for (day in 1..7) {
            assertEquals(expectedFull[day - 1], WeekdayLabels.fullName(day))
            assertEquals(expectedShort[day - 1], WeekdayLabels.shortName(day))
        }

        assertEquals("Dia 0", WeekdayLabels.fullName(0))
        assertEquals("Dia 8", WeekdayLabels.fullName(8))
        assertEquals("-1", WeekdayLabels.shortName(-1))
        assertEquals("9", WeekdayLabels.shortName(9))
    }

    @Test
    fun systemCurrentDateProvider_returnsCurrentLocalDate() {
        val provider = SystemCurrentDateProvider()
        val today = provider.today()
        assertNotNull(today)
        assertEquals(LocalDate.now(), today)
    }
}
