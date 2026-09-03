package cz.b2brental.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Barevné schéma aplikace B2B Rental — světlý režim Material3. */
private val LightColorScheme = lightColorScheme()

/**
 * Hlavní téma aplikace.
 * Používá světlé schéma Material3; temný režim není podporován.
 * @param content obsah rozhraní v rámci tématu
 */
@Composable
public fun B2bTheme(content: @Composable () -> Unit): Unit {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
