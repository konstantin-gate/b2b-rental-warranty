package cz.b2brental.presentation.feature.contract

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.data.remote.dto.ContractItemDto
import cz.b2brental.domain.model.ContractStatus
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.domain.model.UserRole
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.MoneyText
import cz.b2brental.presentation.components.RefreshTopAppBar
import cz.b2brental.presentation.components.StyledEquipmentCard
import cz.b2brental.domain.model.UserProfile

/**
 * Obrazovka detailu nájemní smlouvy — zobrazí položky, částky a akce (schválit/zamítnout/PDF).
 * @param contractId ID smlouvy z navigace
 * @param profile profil přihlášeného uživatele
 * @param viewModel ViewModel detailu smlouvy
 * @param onNavigateBack callback pro návrat na předchozí obrazovku
 * @param onPdfRequested callback pro otevření PDF prohlížeče
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun ContractDetailScreen(
    contractId: Long,
    profile: UserProfile,
    viewModel: ContractDetailViewModel,
    onNavigateBack: () -> Unit,
    onPdfRequested: (Long) -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(contractId) {
        viewModel.loadContract(contractId)
    }

    LaunchedEffect(uiState.pdfDocumentId) {
        uiState.pdfDocumentId?.let { docId ->
            onPdfRequested(docId)
        }
    }

    Scaffold(
        topBar = {
            RefreshTopAppBar(
                titleRes = R.string.contract_detail_title,
                onNavigateBack = onNavigateBack,
                onRefresh = { viewModel.retry() },
                refreshEnabled = uiState.contract != null,
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

                uiState.contract?.let { contract ->
                    Text(
                        text = contract.companyName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.contract_detail_status_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = contract.status.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.contract_detail_period_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("${contract.startDate} — ${contract.endDate}")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.contract_detail_months_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("${contract.months}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.contract_detail_amounts_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    MoneyRow(stringResource(R.string.contract_new_monthly_label), contract.monthlyAmount)
                    MoneyRow(stringResource(R.string.contract_new_deposit_label), contract.deposit)
                    MoneyRow(stringResource(R.string.contract_new_total_label), contract.totalAmount)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.contract_detail_delivery_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(contract.deliveryAddress)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (contract.items.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.contract_detail_items_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        contract.items.forEach { item ->
                            ContractItemRow(item, uiState.equipmentDetails[item.equipmentId])
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val isManagerOrAdmin = profile.role == UserRole.MANAGER || profile.role == UserRole.ADMIN

                    if (isManagerOrAdmin && contract.status == ContractStatus.DRAFT) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { viewModel.approveContract() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.contract_approve))
                            }
                            OutlinedButton(
                                onClick = { viewModel.rejectContract() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.contract_reject))
                            }
                        }
                    }

                    if (contract.status == ContractStatus.ACTIVE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.requestPdf() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.contract_pdf))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Řádek s částkou — název a hodnota přes MoneyText.
 * @param label název položky
 * @param amount částka jako řetězec
 */
@Composable
private fun MoneyRow(label: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        MoneyText(raw = amount)
    }
}

/**
 * Řádek položky smlouvy ve schváleném barevném stylu katalogu.
 * @param item data položky
 * @param detail detail pozice katalogu (null = detail se nepodařilo načíst)
 */
@Composable
private fun ContractItemRow(item: ContractItemDto, detail: CatalogItemResponseDto?) {
    StyledEquipmentCard(
        categoryId = detail?.categoryId?.toInt() ?: 0,
        status = detail?.status ?: EquipmentStatus.AVAILABLE,
        onClick = null,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = item.model, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
