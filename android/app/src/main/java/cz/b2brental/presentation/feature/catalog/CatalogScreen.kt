package cz.b2brental.presentation.feature.catalog

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
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
import cz.b2brental.presentation.components.StyledEquipmentCard
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.UserRole

/**
 * Obrazovka katalogu vybavení — zobrazí seznam položek s filtry a výběrem pro smlouvu.
 * @param profile profil přihlášeného uživatele
 * @param viewModel ViewModel katalogu
 * @param onEquipmentClick callback při kliknutí na položku (přechod na detail)
 * @param onCreateContract callback při kliknutí na FAB (přechod na vytvoření smlouvy)
 */
@Suppress("KDocMissingDocumentation", "HardcodedStringLiteral")
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

                    // Lokální filtrace bez opětovného načtení — přepnutí filtru nepřekresluje celou obrazovku.
                    val visibleItems: List<CatalogItemResponseDto> = remember(
                        uiState.items,
                        uiState.activeCategoryId,
                    ) {
                        if (uiState.activeCategoryId == null) {
                            uiState.items
                        } else {
                            uiState.items.filter { item -> item.categoryId == uiState.activeCategoryId }
                        }
                    }

                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "filter_all") {
                            FilterChip(
                                selected = uiState.activeCategoryId == null,
                                onClick = { viewModel.filterByCategory(null) },
                                label = { Text(stringResource(R.string.catalog_filter_all)) },
                            )
                        }
                        items(categories, key = { pair -> pair.first }) { category ->
                            val (id, name) = category
                            FilterChip(
                                selected = uiState.activeCategoryId == id,
                                onClick = { viewModel.filterByCategory(id) },
                                label = { Text(name) },
                            )
                        }
                    }

                    // Sekce podle kategorií — položky uvnitř seřazeny: dostupné první, pak podle modelu.
                    val sections: List<Pair<String, List<CatalogItemResponseDto>>> = remember(visibleItems) {
                        visibleItems
                            .groupBy { item -> item.categoryId }
                            .toSortedMap()
                            .map { entry ->
                                val categoryName = entry.value.first().categoryName
                                categoryName to entry.value.sortedWith(
                                    compareBy(
                                        { item -> item.status != EquipmentStatus.AVAILABLE },
                                        { item -> item.model },
                                    ),
                                )
                            }
                    }

                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sections.forEach { section ->
                            val (categoryName, sectionItems) = section
                            item(key = "header_$categoryName") {
                                CatalogSectionHeader(title = categoryName)
                            }
                            items(sectionItems, key = { item -> item.id }) { item ->
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
            }
        },
    )
}

/**
 * Záhlaví sekce katalogu (název kategorie).
 * @param title název kategorie
 */
@Composable
private fun CatalogSectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
    }
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
    StyledEquipmentCard(
        categoryId = item.categoryId.toInt(),
        status = item.status,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
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
        EquipmentStatus.AVAILABLE ->
            stringResource(R.string.catalog_status_available) to MaterialTheme.colorScheme.primary

        EquipmentStatus.RENTED ->
            stringResource(R.string.catalog_status_rented) to MaterialTheme.colorScheme.secondary

        EquipmentStatus.MAINTENANCE ->
            stringResource(R.string.catalog_status_maintenance) to MaterialTheme.colorScheme.tertiary
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
