package com.gymslut.app.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymslut.app.core.time.WeekdayLabels
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity

@Composable
fun TodayScreen(
    onNavigateToPlans: () -> Unit,
    onOpenActiveSession: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showOtherWorkoutsDialog by remember { mutableStateOf(false) }
    var pendingTemplateToStart by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }

    fun handleStartTemplate(template: WorkoutTemplateEntity) {
        val openSession = state.inProgressSession
        if (openSession != null) {
            pendingTemplateToStart = template
        } else {
            viewModel.startWorkout(template.id, onOpenActiveSession)
        }
    }

    val activePlan = state.activePlan
    if (activePlan == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nenhum plano de treino ativo",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Crie ou ative um plano de treino para organizar seu cronograma semanal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onNavigateToPlans) {
                    Text("Ir para Planos")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = activePlan.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = WeekdayLabels.fullName(state.todayDayOfWeek),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.weekDays.forEach { day ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (day.isToday) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (day.isToday) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = day.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (day.workoutName != null) "•" else "-",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (day.workoutName != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }

        state.inProgressSession?.let { openSession ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Treino em andamento: ${openSession.workoutNameSnapshot}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onOpenActiveSession(openSession.id) }) {
                                Text("Retomar treino")
                            }
                            TextButton(
                                onClick = { viewModel.discardInProgressSession(openSession.id) }
                            ) {
                                Text("Descartar")
                            }
                        }
                    }
                }
            }
        }

        item {
            val scheduled = state.scheduledTemplate
            if (scheduled != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Treino de hoje",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = scheduled.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (state.scheduledExercises.isEmpty()) {
                            Text(
                                text = "Esta ficha ainda não possui exercícios cadastrados.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.scheduledExercises.forEach { ex ->
                                Text(
                                    text = "• ${ex.exerciseName} (${ex.targetSets}x${ex.targetReps})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { handleStartTemplate(scheduled) },
                            enabled = state.scheduledExercises.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Iniciar treino")
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Dia de descanso",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Hoje está marcado como descanso no cronograma. Se quiser compensar uma falta, você pode iniciar qualquer outro treino do plano.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.allTemplates.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = { showOtherWorkoutsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Escolher outro treino")
                }
            }
        } else {
            item {
                OutlinedButton(
                    onClick = onNavigateToPlans,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configurar fichas em Planos")
                }
            }
        }
    }

    if (showOtherWorkoutsDialog) {
        AlertDialog(
            onDismissRequest = { showOtherWorkoutsDialog = false },
            title = { Text("Escolher outro treino") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.allTemplates.forEach { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOtherWorkoutsDialog = false
                                    handleStartTemplate(template)
                                }
                        ) {
                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOtherWorkoutsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    pendingTemplateToStart?.let { newTemplate ->
        val openSession = state.inProgressSession
        AlertDialog(
            onDismissRequest = { pendingTemplateToStart = null },
            title = { Text("Treino em andamento") },
            text = {
                Text(
                    "Você já possui uma sessão aberta (${openSession?.workoutNameSnapshot ?: ""}). Deseja descartá-la e iniciar \"${newTemplate.name}\"?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingTemplateToStart = null
                        if (openSession != null) {
                            viewModel.discardAndStartWorkout(openSession.id, newTemplate.id, onOpenActiveSession)
                        }
                    }
                ) {
                    Text("Descartar e iniciar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTemplateToStart = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
