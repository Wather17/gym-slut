package com.gymslut.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.repository.CatalogRepository
import com.gymslut.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CompletedSessionCard(
    val sessionId: Long,
    val workoutName: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val formattedDate: String,
    val formattedDuration: String,
    val completedSetsCount: Int,
    val totalVolumeKg: Double
)

data class HistoryUiState(
    val sessions: List<CompletedSessionCard> = emptyList(),
    val catalogExercises: List<ExerciseWithEquipment> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    catalogRepository: CatalogRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        sessionRepository.observeCompletedSessionsWithSets(),
        catalogRepository.observeExercisesWithEquipment()
    ) { sessionsWithSets, catalog ->
        val cards = sessionsWithSets.mapNotNull { item ->
            val finishedAt = item.session.finishedAtEpochMs ?: return@mapNotNull null
            val completedSets = item.sets.count { it.completed }
            val volume = HistoryFormatters.calculateTotalVolumeKg(item.sets)
            CompletedSessionCard(
                sessionId = item.session.id,
                workoutName = item.session.workoutNameSnapshot,
                startedAtEpochMs = item.session.startedAtEpochMs,
                finishedAtEpochMs = finishedAt,
                formattedDate = HistoryFormatters.formatEpochMs(item.session.startedAtEpochMs),
                formattedDuration = HistoryFormatters.formatDuration(
                    item.session.startedAtEpochMs,
                    finishedAt
                ),
                completedSetsCount = completedSets,
                totalVolumeKg = volume
            )
        }
        HistoryUiState(
            sessions = cards,
            catalogExercises = catalog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )
}
