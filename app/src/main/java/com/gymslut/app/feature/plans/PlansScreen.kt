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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.gymslut.app.data.local.entity.TrainingPlanEntity

@Composable
fun PlansScreen(
    onOpenPlan: (Long) -> Unit,
    viewModel: PlansViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPlanDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<TrainingPlanEntity?>(null) }
    var deletingPlan by remember { mutableStateOf<TrainingPlanEntity?>(null) }

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
                    editingPlan = null
                    showPlanDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo plano")
            }
        }
    ) { innerPadding ->
        if (state.plans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum plano cadastrado. Toque em + para criar sua primeira divisão de treinos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.plans, key = { it.id }) { plan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlan(plan.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.activatePlan(plan) }) {
                                Icon(
                                    imageVector = if (plan.isActive) {
                                        Icons.Outlined.CheckCircle
                                    } else {
                                        Icons.Outlined.RadioButtonUnchecked
                                    },
                                    contentDescription = if (plan.isActive) "Plano ativo" else "Ativar plano",
                                    tint = if (plan.isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (plan.isActive) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Plano ativo") }
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.clearFormError()
                                    editingPlan = plan
                                    showPlanDialog = true
                                }
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Renomear plano")
                            }
                            IconButton(onClick = { deletingPlan = plan }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir plano")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlanDialog) {
        PlanNameDialog(
            initialName = editingPlan?.name ?: "",
            title = if (editingPlan == null) "Novo plano de treino" else "Renomear plano",
            errorText = state.formError,
            onDismiss = {
                viewModel.clearFormError()
                showPlanDialog = false
            },
            onConfirm = { name ->
                viewModel.savePlan(editingPlan?.id, name) {
                    showPlanDialog = false
                }
            }
        )
    }

    deletingPlan?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingPlan = null },
            title = { Text("Excluir plano") },
            text = { Text("Deseja excluir \"${target.name}\" e todas as suas fichas?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlan(target)
                        deletingPlan = null
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlan = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
internal fun PlanNameDialog(
    initialName: String,
    title: String,
    errorText: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
