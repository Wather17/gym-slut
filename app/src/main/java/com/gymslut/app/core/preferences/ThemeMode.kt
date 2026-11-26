package com.gymslut.app.core.preferences

/**
 * Preferência de aparência do aplicativo.
 */
enum class ThemeMode(val labelPtBr: String) {
    DARK("Escuro"),
    LIGHT("Claro"),
    SYSTEM("Sistema");

    companion object {
        val DEFAULT: ThemeMode = DARK

        fun fromStorageValue(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DEFAULT
    }
}
