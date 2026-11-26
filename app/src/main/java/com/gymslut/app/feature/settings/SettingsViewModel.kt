package com.gymslut.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.core.preferences.PreferencesRepository
import com.gymslut.app.core.preferences.ThemeMode
import com.gymslut.app.core.preferences.WorkoutPreferences
import com.gymslut.app.data.repository.BackupRepository
import com.gymslut.app.data.repository.BackupValidationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val workoutPreferences: WorkoutPreferences = WorkoutPreferences(),
    val pendingRestoreJson: String? = null,
    val feedbackMessage: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val pendingRestoreJson = MutableStateFlow<String?>(null)
    private val feedbackState = MutableStateFlow<Pair<String?, Boolean>>(null to false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.themeMode,
        preferencesRepository.workoutPreferences,
        pendingRestoreJson,
        feedbackState
    ) { theme, workoutPrefs, pendingJson, feedback ->
        SettingsUiState(
            themeMode = theme,
            workoutPreferences = workoutPrefs,
            pendingRestoreJson = pendingJson,
            feedbackMessage = feedback.first,
            isError = feedback.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun selectThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setKeepScreenOn(enabled)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun setAutoStartRestTimer(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoStartRestTimer(enabled)
        }
    }

    fun setRestTimerVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRestTimerVibrationEnabled(enabled)
        }
    }

    fun exportBackup(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupRepository.exportToJson()
                onJsonReady(json)
            } catch (e: Exception) {
                feedbackState.value = ("Falha ao gerar backup: ${e.message ?: "erro desconhecido"}") to true
            }
        }
    }

    fun reportExportCompleted(success: Boolean, errorMessage: String? = null) {
        feedbackState.value = if (success) {
            "Backup exportado com sucesso." to false
        } else {
            (errorMessage ?: "Não foi possível salvar o arquivo de backup.") to true
        }
    }

    fun prepareRestore(jsonString: String) {
        try {
            backupRepository.validateJson(jsonString)
            pendingRestoreJson.value = jsonString
        } catch (e: BackupValidationException) {
            pendingRestoreJson.value = null
            feedbackState.value = (e.message ?: "Arquivo de backup inválido.") to true
        }
    }

    fun confirmRestore() {
        val json = pendingRestoreJson.value ?: return
        pendingRestoreJson.value = null
        viewModelScope.launch {
            try {
                backupRepository.restoreFromJson(json)
                feedbackState.value = "Backup restaurado com sucesso." to false
            } catch (e: BackupValidationException) {
                feedbackState.value = (e.message ?: "Falha na validação do backup.") to true
            } catch (e: Exception) {
                feedbackState.value = "Erro ao restaurar backup: ${e.message ?: "erro desconhecido"}" to true
            }
        }
    }

    fun cancelRestore() {
        pendingRestoreJson.value = null
    }

    fun clearFeedback() {
        feedbackState.value = null to false
    }
}
