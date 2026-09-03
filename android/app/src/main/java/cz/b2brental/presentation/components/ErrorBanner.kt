package cz.b2brental.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.b2brental.presentation.util.ErrorType

/**
 * Banner zobrazující chybovou zprávu uživateli.
 * Zobrazí se pouze pokud [message] není null ani prázdný.
 * @param message text chyby (null = banner skrytý)
 * @param modifier volitelný modifier
 */
@Composable
public fun ErrorBanner(message: String?, modifier: Modifier = Modifier): Unit {
    if (message.isNullOrBlank()) return
    val displayMessage = if (message.length > 250) message.take(250) else message
    Text(
        text = displayMessage,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/**
 * Banner zobrazující chybovou zprávu podle typu chyby.
 * @param error typ chyby (null = banner skrytý)
 * @param modifier volitelný modifier
 */
@Composable
public fun ErrorBanner(error: ErrorType?, modifier: Modifier = Modifier): Unit {
    ErrorBanner(message = errorMessage(error), modifier = modifier)
}
