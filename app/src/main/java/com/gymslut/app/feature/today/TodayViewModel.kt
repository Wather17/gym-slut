package com.gymslut.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymslut.app.core.time.CurrentDateProvider
import com.gymslut.app.core.time.WeekdayLabels
import com.gymslut.app.data.local.entity.TrainingPlanEntity
import com.gymslut.app.data.local.entity.WeeklyScheduleEntity
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity
import com.gymslut.app.data.model.WorkoutExerciseDetail
import com.gymslut.app.data.repository.PlanRepository
import com.gymslut.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
    private val currentDateProvider: CurrentDateProvider
) : ViewModel() {

    private data class PlanBundle(
        val plan: TrainingPlanEntity?,
        val templates: List<WorkoutTemplateEntity>,
        val schedule: List<WeeklyScheduleEntity>
    )

    private val activePlanBundle: Flow<PlanBundle> =
        planRepository.observeActivePlan().flatMapLatest { activePlan ->
            if (activePlan == null) {
                flowOf(PlanBundle(null, emptyList(), emptyList()))
            } else {
                combine(
                    planRepository.observeTemplatesForPlan(activePlan.id),
                    planRepository.observeScheduleForPlan(activePlan.id)
                ) { templates, schedule ->
                    PlanBundle(activePlan, templates, schedule)
                }
            }
        }

    private val scheduledExercisesFlow: Flow<List<WorkoutExerciseDetail>> =
        activePlanBundle.flatMapLatest { bundle ->
            val todayDay = currentDateProvider.today().dayOfWeek.value
            val templateId = bundle.schedule.firstOrNull { it.dayOfWeek == todayDay }?.workoutTemplateId
            if (templateId == null) {
                flowOf(emptyList())
            } else {
                planRepository.observeWorkoutExercises(templateId)
            }
        }

    val uiState: StateFlow<TodayUiState> = combine(
        activePlanBundle,
        scheduledExercisesFlow,
        sessionRepository.observeInProgressSession()
    ) { bundle, exercises, inProgress ->
        val todayDay = currentDateProvider.today().dayOfWeek.value
        val templatesById = bundle.templates.associateBy { it.id }
        val scheduleByDay = bundle.schedule.associateBy { it.dayOfWeek }
        val todayTemplateId = scheduleByDay[todayDay]?.workoutTemplateId
        val scheduledTemplate = todayTemplateId?.let { templatesById[it] }

        val weekDays = (1..7).map { day ->
            val assignedTemplateId = scheduleByDay[day]?.workoutTemplateId
            WeekDaySummary(
                dayOfWeek = day,
                shortLabel = WeekdayLabels.shortName(day),
                isToday = day == todayDay,
                workoutName = assignedTemplateId?.let { templatesById[it]?.name }
            )
        }

        TodayUiState(
            activePlan = bundle.plan,
            todayDayOfWeek = todayDay,
            weekDays = weekDays,
            scheduledTemplate = scheduledTemplate,
            scheduledExercises = exercises,
            allTemplates = bundle.templates,
            inProgressSession = inProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    private var isStartingSession = false

    fun startWorkout(workoutTemplateId: Long, onSessionStarted: (Long) -> Unit) {
        if (isStartingSession) return
        isStartingSession = true
        viewModelScope.launch {
            try {
                val sessionId = sessionRepository.startWorkoutSession(workoutTemplateId)
                onSessionStarted(sessionId)
            } catch (_: IllegalArgumentException) {
                // Template pode ter sido removido antes do clique
            } finally {
                isStartingSession = false
            }
        }
    }

    fun discardAndStartWorkout(
        existingSessionId: Long,
        newWorkoutTemplateId: Long,
        onSessionStarted: (Long) -> Unit
    ) {
        if (isStartingSession) return
        isStartingSession = true
        viewModelScope.launch {
            try {
                sessionRepository.discardSession(existingSessionId)
                val sessionId = sessionRepository.startWorkoutSession(newWorkoutTemplateId)
                onSessionStarted(sessionId)
            } catch (_: IllegalArgumentException) {
                // Template pode ter sido removido antes do clique
            } finally {
                isStartingSession = false
            }
        }
    }

    fun discardInProgressSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.discardSession(sessionId)
        }
    }
}
