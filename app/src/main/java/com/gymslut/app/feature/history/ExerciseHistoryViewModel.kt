package com.gymslut.app.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.model.ExerciseHistoryRow
import com.gymslut.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ExerciseSessionHistoryCard(
    val sessionId: Long,
    val workoutName: String,
    val formattedDate: String,
    val sets: List<ExerciseHistoryRow>
)

data class ExerciseHistoryUiState(
    val exerciseName: String = "",
    val equipmentName: String = "",
    val entries: List<ExerciseSessionHistoryCard> = emptyList()
)

@HiltViewModel
class ExerciseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    val uiState: StateFlow<ExerciseHistoryUiState> = sessionRepository
        .observeExerciseHistory(exerciseId)
        .map { rows ->
            val first = rows.firstOrNull()
            val grouped = rows
                .groupBy { it.sessionId }
                .values
                .map { sessionRows ->
                    val head = sessionRows.first()
                    ExerciseSessionHistoryCard(
                        sessionId = head.sessionId,
                        workoutName = head.workoutNameSnapshot,
                        formattedDate = HistoryFormatters.formatEpochMs(head.startedAtEpochMs),
                        sets = sessionRows
                    )
                }
            ExerciseHistoryUiState(
                exerciseName = first?.exerciseNameSnapshot ?: "Exercício",
                equipmentName = first?.equipmentNameSnapshot ?: "",
                entries = grouped
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExerciseHistoryUiState()
        )
}
