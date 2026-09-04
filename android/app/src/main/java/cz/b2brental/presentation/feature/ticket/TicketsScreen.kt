@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.ticket

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.model.TicketStatus
import cz.b2brental.presentation.components.EmptyState
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.RefreshTopAppBar

/**
 * Obrazovka seznamu servisních hlášení s lokálním filtrem podle stavu.
 * @param viewModel ViewModel seznamu
 * @param onTicketClick callback otevření detailu tiketu
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun TicketsScreen(
    viewModel: TicketsViewModel,
    onTicketClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RefreshTopAppBar(
                titleRes = R.string.tickets_title,
                onRefresh = { viewModel.loadTickets() },
                refreshEnabled = true,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ErrorBanner(error = uiState.error)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedStatus == null,
                        onClick = { viewModel.onFilterSelected(null) },
                        label = { Text(stringResource(R.string.ticket_filter_all)) },
                    )
                }
                items(TicketStatus.entries) { status ->
                    FilterChip(
                        selected = uiState.selectedStatus == status,
                        onClick = { viewModel.onFilterSelected(status) },
                        label = { Text(statusLabel(status)) },
                    )
                }
            }

            val visible: List<TicketResponseDto> = uiState.tickets.filter { ticket ->
                uiState.selectedStatus == null || ticket.status == uiState.selectedStatus
            }

            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (visible.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { item -> item.id }) { item ->
                        TicketCard(ticket = item, onClick = { onTicketClick(item.id) })
                    }
                }
            }
        }
    }
}

/**
 * Karta jednoho tiketu v seznamu.
 * @param ticket data tiketu
 * @param onClick callback otevření detailu
 */
@Composable
private fun TicketCard(ticket: TicketResponseDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ticket_no, ticket.id),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.ticket_equipment_label, ticket.equipmentModel))
            Text(
                text = ticket.description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = stringResource(R.string.ticket_created_at_label, ticket.createdAt))
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadge(status = ticket.status)
        }
    }
}

/**
 * Barevný bage stavu tiketu (NEW/ASSIGNED/IN_PROGRESS/RESOLVED/REJECTED).
 * @param status stav tiketu
 */
@Composable
private fun StatusBadge(status: TicketStatus) {
    val (bg: Color, fg: Color) = when (status) {
        TicketStatus.NEW -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TicketStatus.ASSIGNED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TicketStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TicketStatus.RESOLVED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TicketStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Box(
        modifier = Modifier
            .padding(top = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg)) {
            Text(
                text = statusLabel(status),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * Lokalizovaný název stavu tiketu.
 * @param status stav tiketu
 * @return český text
 */
@Composable
private fun statusLabel(status: TicketStatus): String = when (status) {
    TicketStatus.NEW -> stringResource(R.string.ticket_status_new)
    TicketStatus.ASSIGNED -> stringResource(R.string.ticket_status_assigned)
    TicketStatus.IN_PROGRESS -> stringResource(R.string.ticket_status_in_progress)
    TicketStatus.RESOLVED -> stringResource(R.string.ticket_status_resolved)
    TicketStatus.REJECTED -> stringResource(R.string.ticket_status_rejected)
}