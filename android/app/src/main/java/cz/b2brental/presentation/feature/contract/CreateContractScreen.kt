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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.MoneyText
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Obrazovka vytvoření nájemní smlouvy — formulář s výběrem data, měsíců a adresy.
 * @param ids seznam ID vybraných položek z navigace
 * @param viewModel ViewModel pro vytvoření smlouvy
 * @param onContractCreated callback po úspěšném vytvoření (přechod na detail)
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CreateContractScreen(
    ids: List<Long>,
    viewModel: CreateContractViewModel,
    onContractCreated: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(ids) {
        viewModel.init(ids)
    }

    LaunchedEffect(uiState.createdContract) {
        uiState.createdContract?.let { contract ->
            onContractCreated(contract.id)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.contract_new_title)) })
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

                OutlinedTextField(
                    value = uiState.months.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { viewModel.onMonthsChange(it) }
                    },
                    label = { Text(stringResource(R.string.contract_new_months)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                OutlinedTextField(
                    value = uiState.startDate?.format(dateFormatter) ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.contract_new_start_date)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.contract_new_pick_date))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.deliveryAddress,
                    onValueChange = { viewModel.onDeliveryAddressChange(it) },
                    label = { Text(stringResource(R.string.contract_new_delivery_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.contract_new_preview),
                    style = MaterialTheme.typography.titleMedium,
                )
                uiState.preview?.let { preview ->
                    Spacer(modifier = Modifier.height(8.dp))
                    PreviewRow(stringResource(R.string.contract_new_monthly_label), preview.monthlyAmount)
                    PreviewRow(stringResource(R.string.contract_new_deposit_label), preview.deposit)
                    PreviewRow(stringResource(R.string.contract_new_total_label), preview.totalAmount)
                } ?: Text(
                    text = stringResource(R.string.contract_new_preview_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))
                if (uiState.preview != null) {
                    Text(
                        text = stringResource(R.string.contract_new_preview_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.createContract() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.contract_new_submit))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        viewModel.onStartDateChange(date)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Řádek předběžného výpočtu — název a částka.
 * @param label název položky
 * @param amount částka jako řetězec
 */
@Composable
private fun PreviewRow(label: String, amount: String) {
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
