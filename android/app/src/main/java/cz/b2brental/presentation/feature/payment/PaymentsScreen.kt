@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.data.remote.dto.PaymentResponseDto
import cz.b2brental.domain.model.PaymentStatus
import cz.b2brental.presentation.components.EmptyState
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator
import cz.b2brental.presentation.components.MoneyText
import cz.b2brental.presentation.components.RefreshTopAppBar

/**
 * Obrazovka seznamu plateb nájemného.
 * @param viewModel ViewModel plateb
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun PaymentsScreen(
    viewModel: PaymentsViewModel,
    onLogout: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RefreshTopAppBar(
                titleRes = R.string.payments_title,
                onRefresh = { viewModel.loadPayments() },
                refreshEnabled = true,
                onLogout = onLogout,
                logoutEnabled = true,
                onNotificationsClick = onNotificationsClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ErrorBanner(error = uiState.error)

            when {
                uiState.isLoading && uiState.payments.isEmpty() -> LoadingIndicator()
                uiState.payments.isEmpty() -> EmptyState()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.payments, key = { item -> item.id }) { item ->
                        PaymentCard(
                            payment = item,
                            isPaying = uiState.payingPaymentId == item.id,
                            onPay = { viewModel.pay(item.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Karta jedné platby se stavem a tlačítkem «Zaplatit» pro neuhrazené.
 * @param payment data platby
 * @param isPaying probíhá odeslání platby na server
 * @param onPay callback odeslání platby
 */
@Composable
private fun PaymentCard(payment: PaymentResponseDto, isPaying: Boolean, onPay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.payment_period_label, payment.period),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                MoneyText(raw = payment.amount)
            }
            Text(text = stringResource(R.string.payment_due_date_label, payment.dueDate))
            StatusRow(payment = payment, isPaying = isPaying, onPay = onPay)
        }
    }
}

/**
 * Řádek se stavem platby (badge + datum zaplacení nebo tlačítko Zaplatit).
 * @param payment data platby
 * @param isPaying probíhá odeslání platby
 * @param onPay callback odeslání platby
 */
@Composable
private fun StatusRow(payment: PaymentResponseDto, isPaying: Boolean, onPay: () -> Unit) {
    when (payment.status) {
        PaymentStatus.PAID -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(text = stringResource(R.string.payment_status_paid), status = payment.status)
                if (payment.paidAt != null) {
                    Text(text = stringResource(R.string.payment_paid_at_label, payment.paidAt))
                }
            }
        }
        PaymentStatus.UNPAID -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(text = stringResource(R.string.payment_status_unpaid), status = payment.status)
                PayButton(isPaying = isPaying, onPay = onPay, overdue = false)
            }
        }
        PaymentStatus.OVERDUE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(text = stringResource(R.string.payment_status_overdue), status = payment.status)
                PayButton(isPaying = isPaying, onPay = onPay, overdue = true)
            }
        }
    }
}

/**
 * Barevný bage stavu platby (PAID/UNPAID/OVERDUE) — barvy přes MaterialTheme.
 * @param text český text stavu
 * @param status stav platby pro barevné odlišení (PAID=primary, UNPAID=surface, OVERDUE=error)
 */
@Composable
private fun StatusBadge(text: String, status: PaymentStatus) {
    val (bg: Color, fg: Color) = when (status) {
        PaymentStatus.PAID -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PaymentStatus.UNPAID -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        PaymentStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = bg,
            contentColor = fg,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * Tlačítko «Zaplatit» s indikátorem načítání; pro OVERDUE v chybové barvě.
 * @param isPaying probíhá odeslání platby (true = spinner)
 * @param onPay callback odeslání platby
 * @param overdue true = tlačítko v chybové barvě
 */
@Composable
private fun PayButton(isPaying: Boolean, onPay: () -> Unit, overdue: Boolean) {
    if (overdue) {
        Button(
            onClick = onPay,
            enabled = !isPaying,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            if (isPaying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onError,
                )
            } else {
                Text(stringResource(R.string.payment_pay))
            }
        }
    } else {
        Button(onClick = onPay, enabled = !isPaying) {
            if (isPaying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.payment_pay))
            }
        }
    }
}