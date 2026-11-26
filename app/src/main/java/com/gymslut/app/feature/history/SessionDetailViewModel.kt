package com.gymslut.app.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionExerciseGroup(
    val exerciseOrderIndex: Int,
    val exerciseId: Long?,
    val exerciseName: String,
    val equipmentName: String,
    val sets: List<SessionSetEntity>
)

data class SessionDetailUiState(
    val session: WorkoutSessionEntity? = null,
    val groups: List<SessionExerciseGroup> = emptyList(),
    val totalVolumeKg: Double = 0.0
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    val uiState: StateFlow<SessionDetailUiState> = combine(
        sessionRepository.observeSession(sessionId),
        sessionRepository.observeSetsForSession(sessionId)
    ) { session, sets ->
        val groups = sets
            .groupBy { it.exerciseOrderIndex }
            .toSortedMap()
            .map { (order, groupSets) ->
                val first = groupSets.first()
                SessionExerciseGroup(
                    exerciseOrderIndex = order,
                    exerciseId = first.exerciseId,
                    exerciseName = first.exerciseNameSnapshot,
                    equipmentName = first.equipmentNameSnapshot,
                    sets = groupSets.sortedBy { it.setNumber }
                )
            }
        SessionDetailUiState(
            session = session,
            groups = groups,
            totalVolumeKg = HistoryFormatters.calculateTotalVolumeKg(sets)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionDetailUiState()
    )

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.discardSession(sessionId)
            onDeleted()
        }
    }
}
