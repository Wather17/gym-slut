package com.gymslut.app.feature.settings

import com.gymslut.app.core.preferences.PreferencesRepository
import com.gymslut.app.core.preferences.ThemeMode
import com.gymslut.app.data.model.BackupPayload
import com.gymslut.app.data.repository.BackupRepository
import com.gymslut.app.data.repository.BackupValidationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefsRepo: FakePrefsRepo
    private lateinit var backupRepo: FakeBackupRepo
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        prefsRepo = FakePrefsRepo()
        backupRepo = FakeBackupRepo()
        viewModel = SettingsViewModel(prefsRepo, backupRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectThemeMode_updatesState() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.selectThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        job.cancel()
    }

    @Test
    fun prepareAndConfirmRestore_handlesValidAndInvalidBackups() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.prepareRestore("INVALID")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.pendingRestoreJson)
        assertTrue(viewModel.uiState.value.isError)

        viewModel.prepareRestore("VALID_JSON")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.pendingRestoreJson)

        viewModel.confirmRestore()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.pendingRestoreJson)
        assertFalse(viewModel.uiState.value.isError)
        assertEquals("VALID_JSON", backupRepo.lastRestoredJson)
        job.cancel()
    }

    private class FakePrefsRepo : PreferencesRepository {
        private val flow = MutableStateFlow(ThemeMode.DARK)
        override val themeMode: Flow<ThemeMode> = flow
        override suspend fun setThemeMode(mode: ThemeMode) {
            flow.value = mode
        }
    }

    private class FakeBackupRepo : BackupRepository {
        var lastRestoredJson: String? = null

        override suspend fun exportToJson(exportedAtEpochMs: Long): String = "{\"schemaVersion\":1}"

        override fun validateJson(jsonString: String): BackupPayload {
            if (jsonString != "VALID_JSON") {
                throw BackupValidationException("Backup inválido")
            }
            return BackupPayload(exportedAtEpochMs = 1L)
        }

        override suspend fun restoreFromJson(jsonString: String) {
            validateJson(jsonString)
            lastRestoredJson = jsonString
        }
    }
}
