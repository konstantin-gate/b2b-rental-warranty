package cz.b2brental.presentation.feature.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cz.b2brental.R
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.MoneyText
import cz.b2brental.presentation.components.RefreshTopAppBar

/**
 * Obrazovka detailu vybavení — zobrazí model, sériové číslo, cenu, sazbu a stav.
 * @param equipmentId ID vybavení z navigace
 * @param viewModel ViewModel detailu vybavení
 * @param onAddToSelection callback pro přidání do výběru (pouze client)
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun EquipmentDetailScreen(
    equipmentId: Long,
    viewModel: EquipmentDetailViewModel,
    onAddToSelection: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(equipmentId) {
        viewModel.loadEquipment(equipmentId)
    }

    Scaffold(
        topBar = {
            RefreshTopAppBar(
                titleRes = R.string.equipment_detail_title,
                onRefresh = { viewModel.retry() },
                refreshEnabled = uiState.item != null,
                onNotificationsClick = onNotificationsClick,
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                ErrorBanner(error = uiState.error)

                uiState.item?.let { item ->
                    if (item.photoUrl != null) {
                        AsyncImage(
                            model = item.photoUrl,
                            contentDescription = item.model,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = item.model,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(
                        label = stringResource(R.string.equipment_detail_serial),
                        value = item.serialNumber,
                    )
                    DetailRow(
                        label = stringResource(R.string.equipment_detail_category),
                        value = item.categoryName,
                    )
                    DetailRow(
                        label = stringResource(R.string.equipment_detail_status),
                        value = item.status.name,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.equipment_detail_price),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    MoneyText(raw = item.price)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.equipment_detail_monthly_rate),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    MoneyText(raw = item.monthlyRate)

                    if (item.description != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.equipment_detail_description),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Jeden řádek detailu — label + hodnota.
 * @param label název pole
 * @param value hodnota pole
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
