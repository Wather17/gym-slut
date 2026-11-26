package com.gymslut.app.core.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun defaultThemeMode_isDark() {
        assertEquals(ThemeMode.DARK, ThemeMode.DEFAULT)
    }

    @Test
    fun fromStorageValue_mapsValidValuesAndFallsBackToDark() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageValue("DARK"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageValue("LIGHT"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue("SYSTEM"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageValue(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageValue("UNKNOWN"))
    }
}
