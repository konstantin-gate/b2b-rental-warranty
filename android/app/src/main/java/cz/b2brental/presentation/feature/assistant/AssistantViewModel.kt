@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.domain.repository.AiRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Zpráva v konverzaci s AI asistentem.
 * @param isUser true = zpráva od uživatele, false = odpověď AI
 * @param text text zprávy
 */
public data class AssistantMessage(
    val isUser: Boolean,
    val text: String,
)

/**
 * UI stav obrazovky AI asistenta.
 * @param messages seznam zpráv konverzace
 * @param input aktuální text vstupního pole
 * @param isLoading probíhá odeslání/čekání na odpověď
 * @param error typ chyby (null = bez chyby)
 */
public data class AssistantUiState(
    val messages: List<AssistantMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
)

/**
 * ViewModel obrazovky AI asistenta (manager/admin).
 * @param aiRepository repozitář AI dotazů
 */
public class AssistantViewModel(
    private val aiRepository: AiRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<AssistantUiState> = MutableStateFlow(AssistantUiState())

    /** Aktuální UI stav. */
    public val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    /**
     * Změní text vstupního pole.
     * @param value nová hodnota vstupního textu
     */
    public fun onInputChange(value: String): Unit {
        _uiState.update { it.copy(input = value) }
    }

    /**
     * Odešle zprávu AI asistentovi. Při prázdném vstupu se nic neprovede.
     * Při chybě zůstává zpráva uživatele v historii.
     */
    @Suppress("KDocMissingDocumentation")
    public fun send(): Unit {
        val current: AssistantUiState = _uiState.value
        val text: String = current.input.trim()
        if (text.isBlank()) return

        _uiState.update {
            it.copy(
                messages = it.messages + AssistantMessage(isUser = true, text = text),
                input = "",
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val response = aiRepository.askAssistant(text)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + AssistantMessage(isUser = false, text = response.reply),
                        isLoading = false,
                    )
                }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.AI_ASSISTANT_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }
}
