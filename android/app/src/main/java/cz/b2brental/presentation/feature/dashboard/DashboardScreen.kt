@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.UserRole
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.MoneyText

/**
 * Obrazovka přehledu (dashboard) manažera/admina.
 * @param profile profil přihlášeného uživatele (pro podmíněnou viditelnost tlačítka správy katalogu)
 * @param viewModel ViewModel přehledu
 * @param onOpenAssistant callback otevření AI asistenta
 * @param onOpenAdminCatalog callback otevření správy katalogu
 * @param onLogout callback odhlášení
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DashboardScreen(
    profile: UserProfile,
    viewModel: DashboardViewModel,
    onOpenAssistant: () -> Unit,
    onOpenAdminCatalog: () -> Unit,
    onLogout: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.loadMetrics() }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = { viewModel.retry() }) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.common_retry),
                        )
                    }
                    IconButton(onClick = onLogout) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.menu_logout),
                        )
                    }
                },
            )
        },
    ) { padding ->
        DashboardContent(
            padding = padding,
            uiState = uiState,
            profile = profile,
            onOpenAssistant = onOpenAssistant,
            onOpenAdminCatalog = onOpenAdminCatalog,
        )
    }
}

/**
 * Vnitřní obsah obrazovky přehledu.
 * @param padding odsazení z Scaffold
 * @param uiState aktuální UI stav
 * @param profile profil uživatele
 * @param onOpenAssistant callback AI asistenta
 * @param onOpenAdminCatalog callback správy katalogu
 */
@Composable
private fun DashboardContent(
    padding: PaddingValues,
    uiState: DashboardUiState,
    profile: UserProfile,
    onOpenAssistant: () -> Unit,
    onOpenAdminCatalog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ErrorBanner(error = uiState.error)

        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.metrics == null -> Unit
            else -> {
                val metrics = uiState.metrics ?: return
                DashboardCard(
                    title = stringResource(R.string.dashboard_active_contracts),
                    value = metrics.activeContracts.toString(),
                )
                DashboardCard(
                    title = stringResource(R.string.dashboard_open_tickets),
                    value = metrics.openTickets.toString(),
                )
                DashboardCard(
                    title = stringResource(R.string.dashboard_overdue_payments),
                    value = metrics.overduePayments.toString() + "  ·  ",
                    extra = { MoneyText(raw = metrics.overdueAmount) },
                )
                EquipmentStatusCard(available = metrics.equipmentByStatus.available, rented = metrics.equipmentByStatus.rented, maintenance = metrics.equipmentByStatus.maintenance)
                MonthStatsCard(
                    newContracts = metrics.monthStats.newContracts,
                    paymentsPaidTotal = metrics.monthStats.paymentsPaidTotal,
                    resolvedTickets = metrics.monthStats.resolvedTickets,
                )

                OutlinedButton(
                    onClick = onOpenAssistant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.dashboard_assistant))
                }
                if (profile.role == UserRole.ADMIN) {
                    OutlinedButton(
                        onClick = onOpenAdminCatalog,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.admin_catalog_title))
                    }
                }
            }
        }
    }
}

/**
 * Karta s jedním číselným údajem.
 * @param title název metriky
 * @param value textová hodnota
 * @param extra volitelný další prvek (např. částka)
 */
@Composable
private fun DashboardCard(
    title: String,
    value: String,
    extra: (@Composable () -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(text = value, style = MaterialTheme.typography.headlineSmall)
                if (extra != null) extra()
            }
        }
    }
}

/**
 * Karta s rozložením vybavení dle stavu.
 * @param available počet dostupných
 * @param rented počet pronajatých
 * @param maintenance počet v údržbě
 */
@Composable
private fun EquipmentStatusCard(available: Int, rented: Int, maintenance: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.dashboard_equipment), style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(R.string.catalog_status_available) + ": " + available)
            Text(text = stringResource(R.string.catalog_status_rented) + ": " + rented)
            Text(text = stringResource(R.string.catalog_status_maintenance) + ": " + maintenance)
        }
    }
}

/**
 * Karta se statistikami aktuálního měsíce.
 * @param newContracts počet nových smluv
 * @param paymentsPaidTotal celková částka zaplacených plateb
 * @param resolvedTickets počet vyřešených tiketů
 */
@Composable
private fun MonthStatsCard(
    newContracts: Int,
    paymentsPaidTotal: String,
    resolvedTickets: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.dashboard_month_stats), style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(R.string.dashboard_month_new_contracts) + ": " + newContracts)
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(text = stringResource(R.string.dashboard_month_paid) + ": ")
                MoneyText(raw = paymentsPaidTotal)
            }
            Text(text = stringResource(R.string.dashboard_month_resolved) + ": " + resolvedTickets)
        }
    }
}

