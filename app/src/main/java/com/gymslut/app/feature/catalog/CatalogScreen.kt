package com.gymslut.app.feature.catalog

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.MuscleGroup
import com.gymslut.app.data.model.ExerciseWithEquipment

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEquipmentDialog by remember { mutableStateOf(false) }
    var editingEquipment by remember { mutableStateOf<EquipmentEntity?>(null) }
    var deletingEquipment by remember { mutableStateOf<EquipmentEntity?>(null) }

    var showExerciseDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseWithEquipment?>(null) }
    var deletingExercise by remember { mutableStateOf<ExerciseWithEquipment?>(null) }

    LaunchedEffect(state.feedbackMessage) {
        val message = state.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.clearFormError()
                    if (state.activeTab == CatalogTab.EXERCISES) {
                        editingExercise = null
                        showExerciseDialog = true
                    } else {
                        editingEquipment = null
                        showEquipmentDialog = true
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (state.activeTab == CatalogTab.EXERCISES) {
                        "Adicionar exercício"
                    } else {
                        "Adicionar aparelho"
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                CatalogTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = state.activeTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CatalogTab.entries.size
                        )
                    ) {
                        Text(text = tab.labelPtBr)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        if (state.activeTab == CatalogTab.EXERCISES) {
                            "Buscar exercício ou aparelho..."
                        } else {
                            "Buscar aparelho..."
                        }
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.hasActiveFilters) {
                        IconButton(onClick = viewModel::clearFilters) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Limpar filtros")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.activeTab == CatalogTab.EXERCISES) {
                ExerciseFilterRow(
                    selectedMuscle = state.selectedMuscle,
                    onSelectMuscle = viewModel::selectMuscleFilter,
                    selectedEquipmentId = state.selectedEquipmentId,
                    equipmentList = state.allEquipment,
                    onSelectEquipment = viewModel::selectEquipmentFilter
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExerciseList(
                    items = state.exercises,
                    onEdit = { item ->
                        viewModel.clearFormError()
                        editingExercise = item
                        showExerciseDialog = true
                    },
                    onDelete = { item -> deletingExercise = item }
                )
            } else {
                EquipmentFilterRow(
                    selectedCategory = state.selectedCategory,
                    onSelectCategory = viewModel::selectCategoryFilter
                )
                Spacer(modifier = Modifier.height(8.dp))
                EquipmentList(
                    items = state.filteredEquipment,
                    onEdit = { eq ->
                        viewModel.clearFormError()
                        editingEquipment = eq
                        showEquipmentDialog = true
                    },
                    onDelete = { eq -> deletingEquipment = eq }
                )
            }
        }
    }

    if (showEquipmentDialog) {
        EquipmentFormDialog(
            initial = editingEquipment,
            errorText = state.formError,
            onDismiss = {
                viewModel.clearFormError()
                showEquipmentDialog = false
            },
            onConfirm = { name, category, notes ->
                viewModel.saveEquipment(
                    editingId = editingEquipment?.id,
                    name = name,
                    category = category,
                    notes = notes,
                    onSuccess = { showEquipmentDialog = false }
                )
            }
        )
    }

    if (showExerciseDialog) {
        ExerciseFormDialog(
            initial = editingExercise,
            equipmentList = state.allEquipment,
            errorText = state.formError,
            onDismiss = {
                viewModel.clearFormError()
                showExerciseDialog = false
            },
            onConfirm = { name, primary, secondary, equipmentId, notes ->
                viewModel.saveExercise(
                    editingId = editingExercise?.exercise?.id,
                    name = name,
                    primaryMuscle = primary,
                    secondaryMuscle = secondary,
                    equipmentId = equipmentId,
                    notes = notes,
                    onSuccess = { showExerciseDialog = false }
                )
            }
        )
    }

    deletingEquipment?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingEquipment = null },
            title = { Text("Excluir aparelho") },
            text = { Text("Deseja excluir \"${target.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEquipment(target)
                        deletingEquipment = null
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEquipment = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    deletingExercise?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingExercise = null },
            title = { Text("Excluir exercício") },
            text = { Text("Deseja excluir \"${target.exercise.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExercise(target)
                        deletingExercise = null
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExercise = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ExerciseFilterRow(
    selectedMuscle: MuscleGroup?,
    onSelectMuscle: (MuscleGroup?) -> Unit,
    selectedEquipmentId: Long?,
    equipmentList: List<EquipmentEntity>,
    onSelectEquipment: (Long?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedMuscle == null,
                onClick = { onSelectMuscle(null) },
                label = { Text("Todos músculos") }
            )
            MuscleGroup.entries.forEach { muscle ->
                FilterChip(
                    selected = selectedMuscle == muscle,
                    onClick = { onSelectMuscle(if (selectedMuscle == muscle) null else muscle) },
                    label = { Text(muscle.labelPtBr) }
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedEquipmentId == null,
                onClick = { onSelectEquipment(null) },
                label = { Text("Todos aparelhos") }
            )
            equipmentList.forEach { eq ->
                FilterChip(
                    selected = selectedEquipmentId == eq.id,
                    onClick = { onSelectEquipment(if (selectedEquipmentId == eq.id) null else eq.id) },
                    label = { Text(eq.name) }
                )
            }
        }
    }
}

@Composable
private fun EquipmentFilterRow(
    selectedCategory: EquipmentCategory?,
    onSelectCategory: (EquipmentCategory?) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            label = { Text("Todas categorias") }
        )
        EquipmentCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(if (selectedCategory == category) null else category) },
                label = { Text(category.labelPtBr) }
            )
        }
    }
}

@Composable
private fun ExerciseList(
    items: List<ExerciseWithEquipment>,
    onEdit: (ExerciseWithEquipment) -> Unit,
    onDelete: (ExerciseWithEquipment) -> Unit
) {
    if (items.isEmpty()) {
        EmptyCatalogMessage("Nenhum exercício encontrado com os filtros atuais.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.exercise.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.exercise.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val muscles = buildString {
                            append(item.exercise.primaryMuscle.labelPtBr)
                            item.exercise.secondaryMuscle?.let {
                                append(" • ")
                                append(it.labelPtBr)
                            }
                        }
                        Text(
                            text = "$muscles • ${item.equipmentName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.exercise.notes.isNotBlank()) {
                            Text(
                                text = item.exercise.notes,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onEdit(item) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar exercício")
                    }
                    IconButton(onClick = { onDelete(item) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Excluir exercício")
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentList(
    items: List<EquipmentEntity>,
    onEdit: (EquipmentEntity) -> Unit,
    onDelete: (EquipmentEntity) -> Unit
) {
    if (items.isEmpty()) {
        EmptyCatalogMessage("Nenhum aparelho encontrado com os filtros atuais.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { eq ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = eq.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = eq.category.labelPtBr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (eq.notes.isNotBlank()) {
                            Text(
                                text = eq.notes,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onEdit(eq) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar aparelho")
                    }
                    IconButton(onClick = { onDelete(eq) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Excluir aparelho")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentFormDialog(
    initial: EquipmentEntity?,
    errorText: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: EquipmentCategory, notes: String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: EquipmentCategory.MACHINE) }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo aparelho" else "Editar aparelho") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do aparelho") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category.labelPtBr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        EquipmentCategory.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.labelPtBr) },
                                onClick = {
                                    category = item
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
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
            TextButton(onClick = { onConfirm(name, category, notes) }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseFormDialog(
    initial: ExerciseWithEquipment?,
    equipmentList: List<EquipmentEntity>,
    errorText: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        primary: MuscleGroup,
        secondary: MuscleGroup?,
        equipmentId: Long?,
        notes: String
    ) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.exercise?.name ?: "") }
    var primary by remember(initial) {
        mutableStateOf(initial?.exercise?.primaryMuscle ?: MuscleGroup.CHEST)
    }
    var secondary by remember(initial) {
        mutableStateOf(initial?.exercise?.secondaryMuscle)
    }
    var selectedEquipmentId by remember(initial, equipmentList) {
        mutableStateOf(initial?.exercise?.equipmentId ?: equipmentList.firstOrNull()?.id)
    }
    var notes by remember(initial) { mutableStateOf(initial?.exercise?.notes ?: "") }

    var primaryExpanded by remember { mutableStateOf(false) }
    var secondaryExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo exercício" else "Editar exercício") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do exercício") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = primaryExpanded,
                    onExpandedChange = { primaryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = primary.labelPtBr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Músculo principal") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = primaryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = primaryExpanded,
                        onDismissRequest = { primaryExpanded = false }
                    ) {
                        MuscleGroup.entries.forEach { muscle ->
                            DropdownMenuItem(
                                text = { Text(muscle.labelPtBr) },
                                onClick = {
                                    primary = muscle
                                    if (secondary == muscle) secondary = null
                                    primaryExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = secondaryExpanded,
                    onExpandedChange = { secondaryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = secondary?.labelPtBr ?: "Nenhum",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Músculo secundário (opcional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = secondaryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = secondaryExpanded,
                        onDismissRequest = { secondaryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nenhum") },
                            onClick = {
                                secondary = null
                                secondaryExpanded = false
                            }
                        )
                        MuscleGroup.entries
                            .filter { it != primary }
                            .forEach { muscle ->
                                DropdownMenuItem(
                                    text = { Text(muscle.labelPtBr) },
                                    onClick = {
                                        secondary = muscle
                                        secondaryExpanded = false
                                    }
                                )
                            }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = equipmentExpanded,
                    onExpandedChange = { equipmentExpanded = it }
                ) {
                    val selectedEqName = equipmentList.firstOrNull { it.id == selectedEquipmentId }?.name
                        ?: "Selecione"
                    OutlinedTextField(
                        value = selectedEqName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aparelho") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = equipmentExpanded,
                        onDismissRequest = { equipmentExpanded = false }
                    ) {
                        equipmentList.forEach { eq ->
                            DropdownMenuItem(
                                text = { Text(eq.name) },
                                onClick = {
                                    selectedEquipmentId = eq.id
                                    equipmentExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
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
            TextButton(onClick = { onConfirm(name, primary, secondary, selectedEquipmentId, notes) }) {
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
