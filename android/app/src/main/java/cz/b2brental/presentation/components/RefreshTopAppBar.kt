@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cz.b2brental.R

/**
 * Horní lišta (TopAppBar) s volitelným tlačítkem «Obnovit» a «Odhlásit se».
 *
 * Sjednocuje duplicitní vzor zobrazený na obrazovkách katalogu, smluv, detailů
 * a «Mé vybavení»: titulek z `strings.xml` a akce s ikonou Refresh.
 *
 * @param titleRes ID resource řetězce s titulkem obrazovky
 * @param onNavigateBack callback pro návrat na předchozí obrazovku (null = šipka zpět skryta)
 * @param onRefresh callback pro akci «Obnovit»
 * @param refreshEnabled zda se má tlačítko «Obnovit» vůbec zobrazit (např. před načtením dat)
 * @param onLogout callback pro akci «Odhlásit se» (null = tlačítko skryto)
 * @param logoutEnabled zda se má tlačítko «Odhlásit se» zobrazit
 * @param onNotificationsClick callback pro otevření obrazovky notifikací (null = ikona skryta)
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RefreshTopAppBar(
    titleRes: Int,
    onNavigateBack: (() -> Unit)? = null,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
    onLogout: (() -> Unit)? = null,
    logoutEnabled: Boolean = false,
    onNotificationsClick: (() -> Unit)? = null,
) {
    TopAppBar(
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                }
            }
        },
        title = { Text(stringResource(titleRes)) },
        actions = {
            if (onNotificationsClick != null) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = stringResource(R.string.notifications_title),
                    )
                }
            }
            if (refreshEnabled) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.common_retry),
                    )
                }
            }
            if (logoutEnabled && onLogout != null) {
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.menu_logout),
                    )
                }
            }
        },
    )
}
