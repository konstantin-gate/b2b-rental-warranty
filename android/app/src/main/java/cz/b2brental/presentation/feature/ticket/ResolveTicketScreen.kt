@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.ticket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.domain.model.Resolution
import cz.b2brental.presentation.components.ErrorBanner

/**
 * Obrazovka vyřešení tiketu technikem.
 * @param ticketId ID tiketu
 * @param viewModel ViewModel obrazovky
 * @param onNavigateBack callback návratu zpět
 * @param onOpenReport callback otevření vygenerovaného reportu (PDF)
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ResolveTicketScreen(
    ticketId: Long,
    viewModel: ResolveTicketViewModel,
    onNavigateBack: () -> Unit,
    onOpenReport: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.resolve_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ErrorBanner(error = uiState.error)

            ResolutionOption(
                label = stringResource(R.string.ticket_resolution_repaired),
                selected = uiState.resolution == Resolution.REPAIRED,
                onClick = { viewModel.onResolutionChange(Resolution.REPAIRED) },
            )
            ResolutionOption(
                label = stringResource(R.string.ticket_resolution_replaced),
                selected = uiState.resolution == Resolution.REPLACED,
                onClick = { viewModel.onResolutionChange(Resolution.REPLACED) },
            )
            ResolutionOption(
                label = stringResource(R.string.ticket_resolution_not_covered),
                selected = uiState.resolution == Resolution.NOT_COVERED,
                onClick = { viewModel.onResolutionChange(Resolution.NOT_COVERED) },
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { value -> viewModel.onNotesChange(value) },
                label = { Text(stringResource(R.string.ticket_notes)) },
                placeholder = { Text(stringResource(R.string.resolve_notes_hint)) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.resolve_notes_hint)) },
                minLines = 3,
            )

            Button(
                onClick = { viewModel.submit(ticketId) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.ticket_submit_resolve))
            }
        }
    }

    if (uiState.isResolved) {
        ResolvedDialog(
            reportDocumentId = uiState.reportDocumentId,
            onOpenReport = onOpenReport,
            onClose = onNavigateBack,
        )
    }
}

/**
 * Řádek s radiobuttonem a popiskem volby výsledku řešení.
 * @param label text volby
 * @param selected je vybráno
 * @param onClick callback kliknutí
 */
@Composable
private fun ResolutionOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Box(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * Dialog po úspěšném vyřešení tiketu.
 * @param reportDocumentId ID reportu (null = nebyl vygenerován)
 * @param onOpenReport callback otevření reportu
 * @param onClose callback zavření dialogu
 */
@Composable
private fun ResolvedDialog(
    reportDocumentId: Long?,
    onOpenReport: (Long) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.resolve_success_title)) },
        text = { Text(stringResource(R.string.resolve_success_text)) },
        confirmButton = {
            if (reportDocumentId != null) {
                TextButton(onClick = { onOpenReport(reportDocumentId) }) {
                    Text(stringResource(R.string.ticket_open_report))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}
