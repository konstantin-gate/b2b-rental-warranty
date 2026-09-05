package cz.b2brental.presentation.feature.equipment

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.presentation.components.EmptyState
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.RefreshTopAppBar

/**
 * Obrazovka „Moje vybavení" — zobrazí vybavení z aktivních smluv.
 * @param viewModel ViewModel vybavení
 * @param onReportIssueClick callback přechodu na hlášení poruchy pro dané vybavení
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun MyEquipmentScreen(
    viewModel: MyEquipmentViewModel,
    onReportIssueClick: (Long) -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RefreshTopAppBar(
                titleRes = R.string.my_equipment_title,
                onRefresh = { viewModel.retry() },
                refreshEnabled = true,
                onLogout = onLogout,
                logoutEnabled = true,
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.items.isEmpty()) {
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
                    items(uiState.items, key = { it.equipmentId }) { item ->
                        EquipmentCard(
                            item = item,
                            onReportIssue = { onReportIssueClick(item.equipmentId) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Karta vybavení — zobrazí model, sériové číslo a tlačítko pro nahlášení poruchy.
 * @param item data vybavení
 * @param onReportIssue callback nahlášení poruchy pro dané vybavení
 */
@Composable
private fun EquipmentCard(item: MyEquipmentItem, onReportIssue: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = item.model,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.equipment_serial),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = item.serialNumber ?: stringResource(R.string.equipment_serial_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onReportIssue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.report_issue_title))
            }
        }
    }
}
