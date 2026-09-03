package cz.b2brental.presentation.components

import androidx.compose.material.icons.Icons
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
 * Horní lišta (TopAppBar) s volitelným tlačítkem «Obnovit».
 *
 * Sjednocuje duplicitní vzor zobrazený na obrazovkách katalogu, smluv, detailů
 * a «Mé vybavení»: titulek z `strings.xml` a akce s ikonou Refresh.
 *
 * @param titleRes ID resource řetězce s titulkem obrazovky
 * @param onRefresh callback pro akci «Obnovit»
 * @param refreshEnabled zda se má tlačítko «Obnovit» vůbec zobrazit (např. před načtením dat)
 */
@Suppress("KDocMissingDocumentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RefreshTopAppBar(
    titleRes: Int,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
) {
    TopAppBar(
        title = { Text(stringResource(titleRes)) },
        actions = {
            if (refreshEnabled) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.common_retry),
                    )
                }
            }
        },
    )
}
