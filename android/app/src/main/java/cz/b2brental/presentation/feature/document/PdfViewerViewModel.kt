package cz.b2brental.presentation.feature.document

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI stav obrazovky prohlížeče PDF.
 * @param isDownloading probíhá stahování
 * @param error chybová zpráva
 * @param pdfFile stažený soubor PDF
 */
public data class PdfViewerUiState(
    val isDownloading: Boolean = false,
    val error: ErrorType? = null,
    val pdfFile: File? = null,
)

/**
 * ViewModel prohlížeče PDF dokumentů — stáhne PDF z backendu a otevře externím prohlížečem.
 * @param apiClient HTTP klient pro stažení PDF
 */
@Suppress("HardCodedStringLiteral")
public class PdfViewerViewModel(
    private val apiClient: B2bApiClient,
) : ViewModel() {

    private companion object {
        private const val CACHE_SUBDIR: String = "documents"
        private const val FILE_PREFIX: String = "document-"
        private const val FILE_EXTENSION: String = ".pdf"
    }

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    /** Aktuální UI stav. */
    public val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    /**
     * Stáhne PDF dokument podle ID a uloží do cache.
     * @param context kontext aplikace (pro přístup k cacheDir)
     * @param documentId ID dokumentu
     */
    public fun downloadPdf(context: Context, documentId: Long): Unit {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
            try {
                val bytes = apiClient.downloadPdf(documentId)
                val dir = File(context.cacheDir, CACHE_SUBDIR)
                dir.mkdirs()
                val file = File(dir, "$FILE_PREFIX$documentId$FILE_EXTENSION")
                file.writeBytes(bytes)
                _uiState.value = _uiState.value.copy(isDownloading = false, pdfFile = file)
            } catch (_: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = ErrorType.PDF_DOWNLOAD_FAILED,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    /**
     * Nastaví chybu, že není nainstalován prohlížeč PDF.
     */
    public fun setNoApp(): Unit {
        _uiState.value = _uiState.value.copy(error = ErrorType.PDF_NO_APP)
    }
}
