package com.gymslut.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymslut.app.core.designsystem.GymSlutTheme
import com.gymslut.app.core.navigation.GymSlutAppShell
import com.gymslut.app.core.preferences.DataStorePreferencesRepository
import com.gymslut.app.core.preferences.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity única do aplicativo Gym Slut.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: DataStorePreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by preferencesRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.DEFAULT
            )
            GymSlutTheme(themeMode = themeMode) {
                GymSlutAppShell()
            }
        }
    }
}
