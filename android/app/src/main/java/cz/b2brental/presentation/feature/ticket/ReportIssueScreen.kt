@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.ticket

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import cz.b2brental.R
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.model.Severity
import cz.b2brental.domain.model.WarrantyVerdict
import cz.b2brental.presentation.components.ErrorBanner
import java.io.File

/**
 * Obrazovka pro nahlášení poruchy vybraného vybavení.
 * @param equipmentId ID vybavení, ke kterému se hlášení vztahuje
 * @param viewModel ViewModel obrazovky
 * @param onNavigateBack callback návratu na předchozí obrazovku
 * @param onNavigateToTickets callback přechodu na seznam hlášení po úspěšném odeslání
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ReportIssueScreen(
    equipmentId: Long,
    viewModel: ReportIssueViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTickets: () -> Unit,
) {
    LaunchedEffect(equipmentId) { viewModel.onInit(equipmentId) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val tempPhotoUri: Uri = remember {
        val imagesDir: File = File(context.cacheDir, "images").apply { mkdirs() }
        val photoFile: File = File(imagesDir, "ticket_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "cz.b2brental.fileprovider", photoFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            viewModel.onPhotoCaptured(context, tempPhotoUri)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            viewModel.onGalleryPhotoSelected(context, pickedUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_issue_title)) },
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

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { value -> viewModel.onDescriptionChanged(value) },
                label = { Text(stringResource(R.string.report_issue_description)) },
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.photoUri == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = { cameraLauncher.launch(tempPhotoUri) }) {
                        Text(stringResource(R.string.report_take_photo))
                    }
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.report_choose_photo))
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AsyncImage(
                        model = uiState.photoUri,
                        contentDescription = stringResource(R.string.ticket_photo_attached),
                        modifier = Modifier.size(96.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ticket_photo_attached))
                        TextButton(onClick = { viewModel.onPhotoRemoved() }) {
                            Text(stringResource(R.string.ticket_photo_delete))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.submit() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.report_send))
                }
            }

            val createdTicket: TicketResponseDto? = uiState.createdTicket
            if (createdTicket != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.onSuccessDialogDismissed(onNavigateToTickets) },
                    title = { Text(stringResource(R.string.ticket_dialog_ai_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.report_severity) + ": " + severityLabel(createdTicket.severity))
                            Text(stringResource(R.string.report_warranty_verdict) + ": " + verdictLabel(createdTicket.warrantyVerdict))
                            if (createdTicket.aiRecommendation != null) {
                                Text(stringResource(R.string.report_ai_recommendation) + ": " + createdTicket.aiRecommendation)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onSuccessDialogDismissed(onNavigateToTickets) }) {
                            Text(stringResource(R.string.ticket_dialog_button_ok))
                        }
                    },
                )
            }
        }
    }
}

/**
 * Lokalizovaný název závažnosti (low/medium/critical) nebo Neurčeno pro null.
 * @param severity závažnost z diagnostiky
 * @return český text pro UI
 */
@Composable
private fun severityLabel(severity: Severity?): String = when (severity) {
    Severity.LOW -> stringResource(R.string.severity_low)
    Severity.MEDIUM -> stringResource(R.string.severity_medium)
    Severity.CRITICAL -> stringResource(R.string.severity_critical)
    null -> stringResource(R.string.severity_none)
}

/**
 * Lokalizovaný verdikt záruky (covered/not_covered/review_required) nebo Neurčeno pro null.
 * @param verdict verdikt z kontroly záruky
 * @return český text pro UI
 */
@Composable
private fun verdictLabel(verdict: WarrantyVerdict?): String = when (verdict) {
    WarrantyVerdict.COVERED -> stringResource(R.string.verdict_covered)
    WarrantyVerdict.NOT_COVERED -> stringResource(R.string.verdict_not_covered)
    WarrantyVerdict.REVIEW_REQUIRED -> stringResource(R.string.verdict_review_required)
    null -> stringResource(R.string.verdict_none)
}