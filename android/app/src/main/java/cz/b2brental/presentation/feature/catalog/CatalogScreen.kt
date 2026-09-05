package cz.b2brental.presentation.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.presentation.components.EmptyState
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.OfflineBanner
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.UserRole

/**
 * Obrazovka katalogu vybavení — zobrazí seznam položek s filtry a výběrem pro smlouvu.
 * @param profile profil přihlášeného uživatele
 * @param viewModel ViewModel katalogu
 * @param onEquipmentClick callback při kliknutí na položku (přechod na detail)
 * @param onCreateContract callback při kliknutí na FAB (přechod na vytvoření smlouvy)
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CatalogScreen(
    profile: UserProfile,
    viewModel: CatalogViewModel,
    onEquipmentClick: (Long) -> Unit,
    onCreateContract: (List<Long>) -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                actions = {
                    IconButton(onClick = { viewModel.loadCatalog() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.common_retry),
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.menu_logout),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (profile.role == UserRole.CLIENT && uiState.selectedIds.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        onCreateContract(uiState.selectedIds.toList())
                        viewModel.clearSelection()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.catalog_create_contract),
                    )
                }
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                OfflineBanner(isOffline = uiState.isOffline)
                ErrorBanner(error = uiState.error)

                if (uiState.isLoading) {
                    LoadingIndicator()
                } else if (uiState.items.isEmpty()) {
                    EmptyState()
                } else {
                    val categories = uiState.items
                        .map { item -> item.categoryId to item.categoryName }
                        .distinctBy { pair -> pair.first }

                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.activeCategoryId == null,
                                onClick = { viewModel.filterByCategory(null) },
                                label = { Text(stringResource(R.string.catalog_filter_all)) },
                            )
                        }
                        items(categories) { category ->
                            val (id, name) = category
                            FilterChip(
                                selected = uiState.activeCategoryId == id,
                                onClick = { viewModel.filterByCategory(id) },
                                label = { Text(name) },
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.items, key = { item -> item.id }) { item ->
                            CatalogItemCard(
                                item = item,
                                isSelectable = profile.role == UserRole.CLIENT &&
                                        item.status == EquipmentStatus.AVAILABLE,
                                isSelected = item.id in uiState.selectedIds,
                                onToggleSelection = { viewModel.toggleSelection(item.id) },
                                onClick = { onEquipmentClick(item.id) },
                            )
                        }
                    }
                }
            }
        },
    )
}

/**
 * Karta položky katalogu.
 * @param item data položky
 * @param isSelectable zda je možné položku vybrat (jen client + AVAILABLE)
 * @param isSelected zda je položka aktuálně vybrána
 * @param onToggleSelection callback při přepnutí výběru
 * @param onClick callback při kliknutí na kartu
 */
@Composable
private fun CatalogItemCard(
    item: CatalogItemResponseDto,
    isSelectable: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.model,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.serialNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = cz.b2brental.domain.util.MoneyFormat.formatCzk(item.monthlyRate),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = item.status)
            }
        }
    }
}

/**
 * Barevný badge stavu vybavení.
 * @param status stav vybavení
 */
@Composable
private fun StatusBadge(status: EquipmentStatus) {
    val (text, color) = when (status) {
        EquipmentStatus.AVAILABLE -> stringResource(R.string.catalog_status_available) to MaterialTheme.colorScheme.primary
        EquipmentStatus.RENTED -> stringResource(R.string.catalog_status_rented) to MaterialTheme.colorScheme.secondary
        EquipmentStatus.MAINTENANCE -> stringResource(R.string.catalog_status_maintenance) to MaterialTheme.colorScheme.tertiary
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
