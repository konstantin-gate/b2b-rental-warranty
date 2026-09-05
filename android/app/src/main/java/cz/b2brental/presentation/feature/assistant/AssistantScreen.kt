@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import cz.b2brental.presentation.components.ErrorBanner

/**
 * Obrazovka AI asistenta (manažer/admin).
 * @param viewModel ViewModel obrazovky
 * @param onLogout callback odhlášení
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AssistantScreen(
    viewModel: AssistantViewModel,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.assistant_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.menu_logout),
                        )
                    }
                },
            )
        },
        bottomBar = {
            AssistantInputBar(
                value = uiState.input,
                enabled = !uiState.isLoading,
                onValueChange = { value -> viewModel.onInputChange(value) },
                onSend = { viewModel.send() },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ErrorBanner(error = uiState.error)
            MessagesList(messages = uiState.messages, isLoading = uiState.isLoading)
        }
    }
}

/**
 * Spodní lišta s textovým polem a tlačítkem odeslání.
 * @param value aktuální text
 * @param enabled je pole aktivní
 * @param onValueChange callback změny textu
 * @param onSend callback odeslání
 */
@Composable
private fun AssistantInputBar(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(stringResource(R.string.assistant_input)) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.assistant_send),
                )
            }
        }
    }
}

/**
 * Seznam zpráv konverzace.
 * @param messages seznam zpráv
 * @param isLoading probíhá odeslání
 */
@Composable
private fun MessagesList(
    messages: List<AssistantMessage>,
    isLoading: Boolean,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items = messages, key = { index -> index }) { message ->
            MessageBubble(message = message)
        }
        if (isLoading) {
            item {
                Text(
                    text = "…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

/**
 * Komponenta bubliny zprávy.
 * @param message data zprávy
 */
@Composable
private fun MessageBubble(message: AssistantMessage) {
    val isUser: Boolean = message.isUser
    val container: androidx.compose.ui.graphics.Color =
        if (isUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer
    val onContainer: androidx.compose.ui.graphics.Color =
        if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer
    val alignment: Alignment.Horizontal =
        if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            color = container,
            contentColor = onContainer,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
