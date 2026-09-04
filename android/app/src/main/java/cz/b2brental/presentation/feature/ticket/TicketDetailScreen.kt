@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.ticket

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.model.Severity
import cz.b2brental.domain.model.TicketStatus
import cz.b2brental.domain.model.UserRole
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.WarrantyVerdict
import cz.b2brental.presentation.components.EmptyState
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator

/**
 * Obrazovka detailu servisního tiketu.
 * @param ticketId ID tiketu
 * @param profile profil přihlášeného uživatele (pro role-based akce)
 * @param viewModel ViewModel detailu
 * @param onNavigateBack callback návratu
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TicketDetailScreen(
    ticketId: Long,
    profile: UserProfile,
    viewModel: TicketDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(ticketId) { viewModel.loadTicket(ticketId) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ticket_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.common_retry),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ErrorBanner(error = uiState.error)

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.ticket == null -> EmptyState()
                else -> uiState.ticket?.let { ticket ->
                    TicketDetailContent(ticket = ticket, profile = profile)
                }
            }
        }
    }
}

/**
 * Obsah detailu tiketu — karty, fotografie, AI diagnostika, akce.
 * @param ticket data tiketu
 * @param profile profil přihlášeného uživatele
 */
@Composable
private fun TicketDetailContent(ticket: TicketResponseDto, profile: UserProfile) {
    Text(text = stringResource(R.string.ticket_equipment_label, ticket.equipmentModel))
    Text(text = stringResource(R.string.ticket_company_label, ticket.companyName))
    Text(text = ticket.description)
    Text(text = stringResource(R.string.ticket_created_at_label, ticket.createdAt))
    Text(text = stringResource(R.string.ticket_status_label, statusLabel(ticket.status)))

    Spacer(modifier = Modifier.height(8.dp))

    if (ticket.photoBase64 != null) {
        val photoBitmap: Bitmap? = remember(ticket.photoBase64) {
            try {
                val bytes: ByteArray = Base64.decode(ticket.photoBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        }
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.ticket_photo_attached),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Text(text = stringResource(R.string.ticket_photo_not_available))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(text = stringResource(R.string.report_severity) + ": " + severityLabel(ticket.severity))
    Text(text = stringResource(R.string.report_warranty_verdict) + ": " + verdictLabel(ticket.warrantyVerdict))
    if (ticket.warrantyReason != null) {
        Text(text = stringResource(R.string.ticket_warranty_reason_label, ticket.warrantyReason))
    }
    if (ticket.aiRecommendation != null) {
        Text(text = stringResource(R.string.report_ai_recommendation) + ": " + ticket.aiRecommendation)
    }

    Spacer(modifier = Modifier.height(12.dp))

    when (profile.role) {
        UserRole.MANAGER, UserRole.ADMIN -> {
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ticket_assign))
            }
        }
        UserRole.TECHNICIAN -> {
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ticket_start))
            }
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ticket_resolve))
            }
        }
        UserRole.CLIENT -> Unit
    }
    Text(
        text = stringResource(R.string.ticket_actions_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Lokalizovaný název stavu tiketu pro detail.
 * @param status stav tiketu (enum)
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

/**
 * Lokalizovaný název závažnosti (low/medium/critical) nebo Neurčeno.
 * @param severity nullable závažnost
 * @return český text
 */
@Composable
private fun severityLabel(severity: Severity?): String = when (severity) {
    Severity.LOW -> stringResource(R.string.severity_low)
    Severity.MEDIUM -> stringResource(R.string.severity_medium)
    Severity.CRITICAL -> stringResource(R.string.severity_critical)
    null -> stringResource(R.string.severity_none)
}

/**
 * Lokalizovaný verdikt záruky nebo Neurčeno.
 * @param verdict nullable verdikt
 * @return český text
 */
@Composable
private fun verdictLabel(verdict: WarrantyVerdict?): String = when (verdict) {
    WarrantyVerdict.COVERED -> stringResource(R.string.verdict_covered)
    WarrantyVerdict.NOT_COVERED -> stringResource(R.string.verdict_not_covered)
    WarrantyVerdict.REVIEW_REQUIRED -> stringResource(R.string.verdict_review_required)
    null -> stringResource(R.string.verdict_none)
}