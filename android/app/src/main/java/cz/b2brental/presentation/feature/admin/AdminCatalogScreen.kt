@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.MoneyText
import cz.b2brental.presentation.components.OfflineBanner
import cz.b2brental.presentation.components.RefreshTopAppBar
import cz.b2brental.presentation.components.StyledEquipmentCard

/**
 * Obrazovka správy katalogu (admin).
 * @param viewModel ViewModel obrazovky
 * @param onLogout callback odhlášení
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun AdminCatalogScreen(
    viewModel: AdminCatalogViewModel,
    onLogout: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<CatalogItemResponseDto?>(null) }

    Scaffold(
        topBar = {
            RefreshTopAppBar(
                titleRes = R.string.admin_catalog_title,
                onRefresh = { viewModel.loadCatalog() },
                refreshEnabled = true,
                onLogout = onLogout,
                logoutEnabled = true,
                onNotificationsClick = onNotificationsClick,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openCreate() }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.admin_catalog_create),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OfflineBanner(isOffline = uiState.isOffline)
            ErrorBanner(error = uiState.error)
            if (uiState.isLoading) {
                LoadingIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = uiState.items, key = { item -> item.id }) { item ->
                        AdminCatalogItemCard(
                            item = item,
                            onEdit = { viewModel.openEdit(item) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showEditor) {
        CatalogEditorDialog(
            editing = uiState.editingItem,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.closeEditor() },
            onSave = { categoryId, model, serial, price, rate, description, photoUrl ->
                viewModel.save(categoryId, model, serial, price, rate, description, photoUrl)
            },
        )
    }

    val toDelete: CatalogItemResponseDto? = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.admin_catalog_confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/**
 * Karta položky katalogu v administraci.
 * @param item data položky
 * @param onEdit callback úpravy
 * @param onDelete callback smazání
 */
@Composable
private fun AdminCatalogItemCard(
    item: CatalogItemResponseDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    StyledEquipmentCard(
        categoryId = item.categoryId.toInt(),
        status = item.status,
        onClick = null,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.model,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.admin_catalog_edit),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.admin_catalog_delete),
                    )
                }
            }
            Text(text = item.serialNumber)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.admin_catalog_price) + ": ")
                MoneyText(raw = item.price)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.admin_catalog_monthly_rate) + ": ")
                MoneyText(raw = item.monthlyRate)
            }
            Text(text = statusLabel(item.status))
        }
    }
}

/**
 * Lokalizovaný název stavu vybavení.
 * @param status stav vybavení
 * @return český text
 */
@Composable
private fun statusLabel(status: EquipmentStatus): String = when (status) {
    EquipmentStatus.AVAILABLE -> stringResource(R.string.catalog_status_available)
    EquipmentStatus.RENTED -> stringResource(R.string.catalog_status_rented)
    EquipmentStatus.MAINTENANCE -> stringResource(R.string.catalog_status_maintenance)
}

/**
 * Dialog editoru (vytvoření / úprava) katalogové položky.
 * @param editing upravovaná položka (null = režim vytvoření)
 * @param isSaving probíhá ukládání
 * @param onDismiss callback zavření
 * @param onSave callback uložení
 */
@Composable
private fun CatalogEditorDialog(
    editing: CatalogItemResponseDto?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, String, String, String) -> Unit,
) {
    var categoryIdText: String by remember { mutableStateOf(editing?.categoryId?.toString() ?: "") }
    var model: String by remember { mutableStateOf(editing?.model ?: "") }
    var serial: String by remember { mutableStateOf(editing?.serialNumber ?: "") }
    var price: String by remember { mutableStateOf(editing?.price ?: "") }
    var rate: String by remember { mutableStateOf(editing?.monthlyRate ?: "") }
    var description: String by remember { mutableStateOf(editing?.description.orEmpty()) }
    var photoUrl: String by remember { mutableStateOf(editing?.photoUrl.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editing == null) stringResource(R.string.admin_catalog_create)
                else stringResource(R.string.admin_catalog_edit),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Pole editoru jsou definovaná datově, aby se neopakovaly shodné fragmenty kódu
                val fields: List<Triple<String, Int, (String) -> Unit>> =
                    listOf(
                        Triple(categoryIdText, R.string.admin_catalog_category_id) { value -> categoryIdText = value },
                        Triple(model, R.string.admin_catalog_model) { value -> model = value },
                        Triple(serial, R.string.admin_catalog_serial_number) { value -> serial = value },
                        Triple(price, R.string.admin_catalog_price) { value -> price = value },
                        Triple(rate, R.string.admin_catalog_monthly_rate) { value -> rate = value },
                        Triple(description, R.string.admin_catalog_description) { value -> description = value },
                        Triple(photoUrl, R.string.admin_catalog_photo_url) { value -> photoUrl = value },
                    )
                fields.forEach { field -> EditorField(field.first, field.second, field.third) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catId: Long = categoryIdText.toLongOrNull() ?: 0L
                    onSave(catId, model, serial, price, rate, description, photoUrl)
                },
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/**
 * Jednořádkové textové pole editoru katalogové položky.
 * @param value aktuální hodnota pole
 * @param labelRes ID resource popisku pole
 * @param onValueChange callback při změně hodnoty
 */
@Suppress("KDocMissingDocumentation")
@Composable
private fun EditorField(
    value: String,
    labelRes: Int,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        modifier = Modifier.fillMaxWidth(),
    )
}
