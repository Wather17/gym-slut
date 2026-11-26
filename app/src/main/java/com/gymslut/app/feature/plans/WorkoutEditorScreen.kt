package com.gymslut.app.feature.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.model.ExerciseWithEquipment
import com.gymslut.app.data.model.WorkoutExerciseDetail

@Composable
fun WorkoutEditorScreen(
    viewModel: WorkoutEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedCatalogExercise by remember { mutableStateOf<ExerciseWithEquipment?>(null) }
    var editingWorkoutExercise by remember { mutableStateOf<WorkoutExerciseDetail?>(null) }

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
                    showPickerDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar exercício ao treino")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            state.template?.let { template ->
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (state.workoutExercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum exercício nesta ficha. Toque em + para adicionar do catálogo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.workoutExercises, key = { _, item -> item.id }) { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.exerciseName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${item.equipmentName} • ${item.targetSets} x ${item.targetReps} • Descanso ${item.restSeconds}s",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { viewModel.moveExercise(item.id, moveUp = true) }
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = "Subir exercício")
                                }
                                IconButton(
                                    enabled = index < state.workoutExercises.lastIndex,
                                    onClick = { viewModel.moveExercise(item.id, moveUp = false) }
                                ) {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Descer exercício")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.clearFormError()
                                        editingWorkoutExercise = item
                                    }
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Editar prescrição")
                                }
                                IconButton(onClick = { viewModel.removeExercise(item.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remover exercício")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPickerDialog) {
        ExercisePickerDialog(
            exercises = state.catalogExercises,
            searchQuery = state.pickerSearchQuery,
            selectedMuscle = state.pickerMuscleFilter,
            onSearchChange = viewModel::updatePickerSearch,
            onSelectMuscle = viewModel::selectPickerMuscle,
            onPickExercise = { chosen ->
                viewModel.clearFormError()
                selectedCatalogExercise = chosen
                showPickerDialog = false
            },
            onDismiss = { showPickerDialog = false }
        )
    }

    selectedCatalogExercise?.let { chosen ->
        PrescriptionDialog(
            title = chosen.exercise.name,
            initialSets = "3",
            initialReps = "10",
            initialRest = "60",
            errorText = state.formError,
            onDismiss = {
                viewModel.clearFormError()
                selectedCatalogExercise = null
            },
            onConfirm = { sets, reps, rest ->
                viewModel.addExercise(chosen.exercise.id, sets, reps, rest) {
                    selectedCatalogExercise = null
                }
            }
        )
    }

    editingWorkoutExercise?.let { editing ->
        PrescriptionDialog(
            title = editing.exerciseName,
            initialSets = editing.targetSets.toString(),
            initialReps = editing.targetReps.toString(),
            initialRest = editing.restSeconds.toString(),
            errorText = state.formError,
            onDismiss = {
                viewModel.clearFormError()
                editingWorkoutExercise = null
            },
            onConfirm = { sets, reps, rest ->
                viewModel.updatePrescription(editing.id, sets, reps, rest) {
                    editingWorkoutExercise = null
                }
            }
        )
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<ExerciseWithEquipment>,
    searchQuery: String,
    selectedMuscle: MuscleGroup?,
    onSearchChange: (String) -> Unit,
    onSelectMuscle: (MuscleGroup?) -> Unit,
    onPickExercise: (ExerciseWithEquipment) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolher exercício") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Buscar exercício...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedMuscle == null,
                        onClick = { onSelectMuscle(null) },
                        label = { Text("Todos") }
                    )
                    MuscleGroup.entries.forEach { muscle ->
                        FilterChip(
                            selected = selectedMuscle == muscle,
                            onClick = { onSelectMuscle(if (selectedMuscle == muscle) null else muscle) },
                            label = { Text(muscle.labelPtBr) }
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(exercises, key = { it.exercise.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPickExercise(item) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${item.exercise.primaryMuscle.labelPtBr} • ${item.equipmentName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun PrescriptionDialog(
    title: String,
    initialSets: String,
    initialReps: String,
    initialRest: String,
    errorText: String?,
    onDismiss: () -> Unit,
    onConfirm: (sets: String, reps: String, restSeconds: String) -> Unit
) {
    var sets by remember(initialSets) { mutableStateOf(initialSets) }
    var reps by remember(initialReps) { mutableStateOf(initialReps) }
    var rest by remember(initialRest) { mutableStateOf(initialRest) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Séries") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Repetições") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rest,
                    onValueChange = { rest = it },
                    label = { Text("Descanso (segundos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            TextButton(onClick = { onConfirm(sets, reps, rest) }) {
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
