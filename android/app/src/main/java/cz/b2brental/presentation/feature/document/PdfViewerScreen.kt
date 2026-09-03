package cz.b2brental.presentation.feature.document

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import cz.b2brental.R
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator

/**
 * Obrazovka prohlížeče PDF — stáhne dokument a otevře externím prohlížečem.
 * @param documentId ID dokumentu z navigace
 * @param viewModel ViewModel prohlížeče PDF
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun PdfViewerScreen(
    documentId: Long,
    viewModel: PdfViewerViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(documentId) {
        viewModel.downloadPdf(context, documentId)
    }

    LaunchedEffect(uiState.pdfFile) {
        uiState.pdfFile?.let { file ->
            try {
                val uri = FileProvider.getUriForFile(context, "cz.b2brental.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_viewer_chooser_title)))
            } catch (_: ActivityNotFoundException) {
                viewModel.setNoApp()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.pdf_viewer_title)) })
        },
    ) { padding ->
        if (uiState.isDownloading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ErrorBanner(error = uiState.error)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.pdf_viewer_downloaded))
            }
        }
    }
}
