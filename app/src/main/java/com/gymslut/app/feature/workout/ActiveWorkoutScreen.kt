package com.gymslut.app.feature.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymslut.app.data.local.entity.SessionSetEntity
import java.util.Locale

@Composable
fun ActiveWorkoutScreen(
    onFinished: () -> Unit,
    onDiscarded: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state.session != null) {
        showLeaveDialog = true
    }

    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var askedNotificationPermission by remember { mutableStateOf(false) }

    DisposableEffect(view, state.workoutPreferences.keepScreenOn) {
        view.keepScreenOn = state.workoutPreferences.keepScreenOn
        onDispose {
            view.keepScreenOn = false
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(timerState.isActive) {
        if (timerState.isActive && !askedNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            askedNotificationPermission = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val session = state.session
    if (session == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Carregando treino...")
        }
        return
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        bottomBar = {
            RestTimerBar(
                state = timerState,
                onSubtract15s = { viewModel.addTimerSeconds(-15) },
                onAdd15s = { viewModel.addTimerSeconds(15) },
                onTogglePause = viewModel::toggleTimerPause,
                onSkip = viewModel::skipTimer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = session.workoutNameSnapshot,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${state.completedSetsCount}/${state.totalSetsCount} séries concluídas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(state.exerciseGroups, key = { it.exerciseOrderIndex }) { group ->
                    ActiveExerciseCard(
                        group = group,
                        onWeightChange = viewModel::updateSetWeight,
                        onRepsChange = viewModel::updateSetReps,
                        onToggleCompleted = { set ->
                            if (!set.completed && state.workoutPreferences.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.toggleSetCompleted(set, group.restSeconds)
                        },
                        onAddExtraSet = { viewModel.addExtraSet(group.exerciseOrderIndex) },
                        onRemoveSet = viewModel::removeSet
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDiscardDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Descartar")
                        }
                        Button(
                            onClick = { showFinishDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Finalizar treino")
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        val pendingCount = state.totalSetsCount - state.completedSetsCount
        val message = if (pendingCount > 0) {
            "Ainda há $pendingCount série(s) não marcada(s) como concluída(s). Deseja finalizar o treino mesmo assim?"
        } else {
            "Concluir e salvar este treino no histórico?"
        }
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finalizar treino") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finishWorkout(onFinished)
                    }
                ) {
                    Text("Finalizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continuar treino")
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Descartar treino") },
            text = { Text("Tem certeza que deseja descartar esta sessão? Os dados registrados nela serão perdidos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout(onDiscarded)
                    }
                ) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Sair da tela de treino?") },
            text = {
                Text(
                    "Sua sessão continua salva em andamento na aba Hoje para retomar quando quiser."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Sair por agora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Continuar treino")
                }
            }
        )
    }
}

@Composable
private fun ActiveExerciseCard(
    group: ActiveExerciseGroup,
    onWeightChange: (SessionSetEntity, String) -> Unit,
    onRepsChange: (SessionSetEntity, String) -> Unit,
    onToggleCompleted: (SessionSetEntity) -> Unit,
    onAddExtraSet: () -> Unit,
    onRemoveSet: (Long) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = group.exerciseName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${group.equipmentName} • Descanso ${group.restSeconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            group.sets.forEach { set ->
                key(set.id) {
                    ActiveSetRow(
                        set = set,
                        canRemove = group.sets.size > 1,
                        onWeightChange = { onWeightChange(set, it) },
                        onRepsChange = { onRepsChange(set, it) },
                        onToggleCompleted = { onToggleCompleted(set) },
                        onRemove = { onRemoveSet(set.id) }
                    )
                }
            }

            TextButton(
                onClick = onAddExtraSet,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar série")
            }
        }
    }
}

@Composable
private fun ActiveSetRow(
    set: SessionSetEntity,
    canRemove: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggleCompleted: () -> Unit,
    onRemove: () -> Unit
) {
    var weightText by remember(set.id) { mutableStateOf(formatWeightInput(set.weightKg)) }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "S${set.setNumber}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(26.dp)
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = { value ->
                weightText = value
                onWeightChange(value)
            },
            label = { Text("kg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = { value ->
                repsText = value
                onRepsChange(value)
            },
            label = { Text("Reps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = set.completed,
            onCheckedChange = { onToggleCompleted() }
        )
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remover série")
            }
        }
    }
}

private fun formatWeightInput(weightKg: Double): String {
    return if (weightKg % 1.0 == 0.0) {
        weightKg.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", weightKg)
    }
}
