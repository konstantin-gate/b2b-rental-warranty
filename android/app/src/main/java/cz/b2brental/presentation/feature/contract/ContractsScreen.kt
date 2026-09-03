package cz.b2brental.presentation.feature.contract

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.data.remote.dto.ContractResponseDto
import cz.b2brental.domain.model.ContractStatus
import cz.b2brental.presentation.components.EmptyState
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.MoneyText
import cz.b2brental.presentation.components.RefreshTopAppBar

/**
 * Obrazovka seznamu nájemních smluv.
 * @param viewModel ViewModel smluv
 * @param onContractClick callback při kliknutí na smlouvu (přechod na detail)
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun ContractsScreen(
    viewModel: ContractsViewModel,
    onContractClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RefreshTopAppBar(
                titleRes = R.string.contracts_title,
                onRefresh = { viewModel.retry() },
                refreshEnabled = true,
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.contracts.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                ErrorBanner(error = uiState.error)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.contracts, key = { it.id }) { contract ->
                        ContractCard(
                            contract = contract,
                            onClick = { onContractClick(contract.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Karta smlouvy — zobrazí název firmy, stav a částky.
 * @param contract data smlouvy
 * @param onClick callback při kliknutí
 */
@Composable
private fun ContractCard(
    contract: ContractResponseDto,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = contract.companyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                StatusBadge(status = contract.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${contract.startDate} — ${contract.endDate}",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.contract_card_monthly_label), style = MaterialTheme.typography.bodySmall)
                MoneyText(raw = contract.monthlyAmount)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.contract_card_deposit_label), style = MaterialTheme.typography.bodySmall)
                MoneyText(raw = contract.deposit)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.contract_card_total_label), style = MaterialTheme.typography.bodySmall)
                MoneyText(raw = contract.totalAmount)
            }
        }
    }
}

/**
 * Barevný badge stavu smlouvy.
 * @param status stav smlouvy
 */
@Composable
private fun StatusBadge(status: ContractStatus) {
    val (text, color) = when (status) {
        ContractStatus.DRAFT -> stringResource(R.string.contract_status_draft) to MaterialTheme.colorScheme.secondary
        ContractStatus.ACTIVE -> stringResource(R.string.contract_status_active) to MaterialTheme.colorScheme.primary
        ContractStatus.APPROVED -> stringResource(R.string.contract_status_approved) to MaterialTheme.colorScheme.tertiary
        ContractStatus.REJECTED -> stringResource(R.string.contract_status_rejected) to MaterialTheme.colorScheme.error
        else -> status.name to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
