package cz.b2brental.presentation.feature.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.b2brental.R
import cz.b2brental.domain.model.NotificationItem
import cz.b2brental.presentation.components.EmptyState
import cz.b2brental.presentation.components.ErrorBanner
import cz.b2brental.presentation.components.LoadingIndicator

/**
 * Obrazovka notifikací uživatele — seznam notifikací z backendu
 * s možností označit jednotlivé i všechny notifikace jako přečtené.
 * @param viewModel ViewModel obrazovky notifikací
 * @param onNavigateBack callback pro návrat na předchozí obrazovku
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.notifications_title)) },
                actions = {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Text(stringResource(R.string.notifications_mark_all_read))
                    }
                },
            )
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                ErrorBanner(message = state.error)
                when {
                    state.isLoading -> LoadingIndicator()
                    state.items.isEmpty() -> EmptyState(message = stringResource(R.string.notifications_empty))
                    else -> {
                        LazyColumn {
                            items(state.items, key = { item -> item.id }) { item ->
                                NotificationCard(
                                    item = item,
                                    onClick = {
                                        if (!item.isRead) {
                                            viewModel.markRead(item)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

/**
 * Karta jedné notifikace. Nepřečtené notifikace jsou zvýrazněny tučným písmem.
 * @param item notifikace k zobrazení
 * @param onClick akce po kliknutí na nepřečtenou notifikaci
 */
@Composable
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.message,
                fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold,
            )
            Text(
                text = item.createdAt,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }
    }
}
