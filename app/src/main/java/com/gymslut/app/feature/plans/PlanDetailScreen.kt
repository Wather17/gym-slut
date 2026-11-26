package com.gymslut.app.feature.plans

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymslut.app.core.time.WeekdayLabels
import com.gymslut.app.data.local.entity.WorkoutTemplateEntity

@Composable
fun PlanDetailScreen(
    onOpenWorkoutEditor: (Long) -> Unit,
    viewModel: PlanDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTemplateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }
    var deletingTemplate by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.clearFormError()
                    editingTemplate = null
                    showTemplateDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar treino")
            }
        }
    ) { innerPadding ->
        val plan = state.plan
        if (plan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Plano não encontrado.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = plan.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (plan.isActive) "Plano ativo no momento" else "Plano inativo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!plan.isActive) {
                                Button(onClick = viewModel::activatePlan) {
                                    Text("Ativar")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Treinos da divisão",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (state.templates.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum treino cadastrado neste plano. Toque em + para criar (ex.: Treino A).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    itemsIndexed(state.templates, key = { _, t -> t.id }) { index, template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWorkoutEditor(template.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { viewModel.moveWorkoutTemplate(template, moveUp = true) }
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = "Subir treino")
                                }
                                IconButton(
                                    enabled = index < state.templates.lastIndex,
                                    onClick = { viewModel.moveWorkoutTemplate(template, moveUp = false) }
                                ) {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Descer treino")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.clearFormError()
                                        editingTemplate = template
                                        showTemplateDialog = true
                                    }
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Renomear treino")
                                }
                                IconButton(onClick = { deletingTemplate = template }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Excluir treino")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cronograma semanal",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items((1..7).toList(), key = { day -> "schedule_$day" }) { day ->
                    val assignedId = state.scheduleByDay[day]
                    val assignedName = state.templates.firstOrNull { it.id == assignedId }?.name ?: "Descanso"
                    ScheduleDayRow(
                        dayLabel = WeekdayLabels.fullName(day),
                        assignedLabel = assignedName,
                        templates = state.templates,
                        onSelectTemplate = { templateId ->
                            viewModel.setScheduleDay(day, templateId)
                        }
                    )
                }
            }
        }
    }

    if (showTemplateDialog) {
        PlanNameDialog(
            initialName = editingTemplate?.name ?: "",
            title = if (editingTemplate == null) "Novo treino" else "Renomear treino",
            errorText = state.formError,
            onDismiss = {
                viewModel.clearFormError()
                showTemplateDialog = false
            },
            onConfirm = { name ->
                viewModel.saveWorkoutTemplate(editingTemplate?.id, name) {
                    showTemplateDialog = false
                }
            }
        )
    }

    deletingTemplate?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingTemplate = null },
            title = { Text("Excluir treino") },
            text = { Text("Deseja excluir \"${target.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkoutTemplate(target)
                        deletingTemplate = null
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTemplate = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ScheduleDayRow(
    dayLabel: String,
    assignedLabel: String,
    templates: List<WorkoutTemplateEntity>,
    onSelectTemplate: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(text = assignedLabel)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Descanso") },
                        onClick = {
                            onSelectTemplate(null)
                            expanded = false
                        }
                    )
                    templates.forEach { template ->
                        DropdownMenuItem(
                            text = { Text(template.name) },
                            onClick = {
                                onSelectTemplate(template.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
