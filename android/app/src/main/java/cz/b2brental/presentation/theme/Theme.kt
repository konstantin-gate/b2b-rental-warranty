package cz.b2brental.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Barevné schéma aplikace B2B Rental — světlý režim Material3. */
private val LightColorScheme = lightColorScheme()

/**
 * Hlavní téma aplikace.
 * Používá světlé schéma Material3; temný režim není podporován.
 * @param content obsah rozhraní v rámci tématu
 */
@Composable
public fun B2bTheme(content: @Composable () -> Unit): Unit {
    CompositionLocalProvider(LocalCatalogPalette provides LightCatalogPalette) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            content = content
        )
    }
}
