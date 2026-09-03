package cz.b2brental.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cz.b2brental.domain.util.MoneyFormat

/**
 * Zobrazení finanční částky v českém formátu (např. "110 700,00 Kč").
 * @param raw surová částka jako řetězec (např. "110700.00")
 * @param modifier volitelný modifier
 */
@Composable
public fun MoneyText(raw: String, modifier: Modifier = Modifier) {
    Text(
        text = MoneyFormat.formatCzk(raw),
        modifier = modifier,
    )
}
