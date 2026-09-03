package cz.b2brental.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.b2brental.R

/**
 * Banner informující uživatele o offline režimu.
 * Zobrazí se pouze pokud [isOffline] je true.
 * @param isOffline flag offline režimu
 * @param modifier volitelný modifier rozvržení
 */
@Composable
public fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    if (!isOffline) return
    Text(
        text = stringResource(R.string.common_offline),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
