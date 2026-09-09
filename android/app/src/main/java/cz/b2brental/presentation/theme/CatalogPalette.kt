package cz.b2brental.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.b2brental.domain.model.EquipmentStatus

/**
 * Barvy jedné kategorie vybavení: svislý pruh na kartě a světlé pozadí karty.
 * @property stripe barva svislého pruhu kategorie
 * @property container barva pozadí karty
 */
public data class CatalogCategoryColors(
    public val stripe: Color,
    public val container: Color,
)

/**
 * Soubor barev katalogu pro jednu barevnou schéma (světlou nebo tmavou).
 * @property categoryColors barvy podle id kategorie vybavení (klíč = id z databáze, 1–4)
 * @property statusAccents přídavné barvy nedostupných stavů (jen RENTED a MAINTENANCE)
 * @property fallback barvy pro neznámé id kategorie a pro stav AVAILABLE (není nikdy použit u stavu)
 */
public data class CatalogPaletteColors(
    public val categoryColors: Map<Int, CatalogCategoryColors>,
    public val statusAccents: Map<EquipmentStatus, CatalogCategoryColors>,
    public val fallback: CatalogCategoryColors,
)

/** Světlá paleta katalogu — výchozí hodnota. */
internal val LightCatalogPalette: CatalogPaletteColors = CatalogPaletteColors(
    categoryColors = mapOf(
        1 to CatalogCategoryColors(Color(0xFF1E88E5), Color(0xFFE3F2FD)),
        2 to CatalogCategoryColors(Color(0xFF8E24AA), Color(0xFFF3E5F5)),
        3 to CatalogCategoryColors(Color(0xFFE53935), Color(0xFFFFEBEE)),
        4 to CatalogCategoryColors(Color(0xFF546E7A), Color(0xFFECEFF1)),
    ),
    statusAccents = mapOf(
        EquipmentStatus.RENTED to CatalogCategoryColors(Color(0xFF2E7D32), Color(0xFFE8F5E9)),
        EquipmentStatus.MAINTENANCE to CatalogCategoryColors(Color(0xFFF9A825), Color(0xFFFFF8E1)),
    ),
    fallback = CatalogCategoryColors(Color(0xFF9E9E9E), Color(0xFFEEEEEE)),
)

/** Tmavá paleta katalogu — použije se po přidání tmavého barevného schématu do B2bTheme. */
private val DarkCatalogPalette: CatalogPaletteColors = CatalogPaletteColors(
    categoryColors = mapOf(
        1 to CatalogCategoryColors(Color(0xFF64B5F6), Color(0xFF1A2733)),
        2 to CatalogCategoryColors(Color(0xFFCE93D8), Color(0xFF2B1E33)),
        3 to CatalogCategoryColors(Color(0xFFEF9A9A), Color(0xFF331E22)),
        4 to CatalogCategoryColors(Color(0xFF90A4AE), Color(0xFF20272D)),
    ),
    statusAccents = mapOf(
        EquipmentStatus.RENTED to CatalogCategoryColors(Color(0xFF81C784), Color(0xFF182B1B)),
        EquipmentStatus.MAINTENANCE to CatalogCategoryColors(Color(0xFFFFB74D), Color(0xFF2F2617)),
    ),
    fallback = CatalogCategoryColors(Color(0xFFBDBDBD), Color(0xFF262626)),
)

/**
 * CompositionLocal s paletou katalogu. Výchozí hodnota — světlá paleta,
 * explicitně se nastavuje v B2bTheme (bod přepnutí na tmavou paletu v budoucnu).
 */
public val LocalCatalogPalette: ProvidableCompositionLocal<CatalogPaletteColors> =
    staticCompositionLocalOf { LightCatalogPalette }

/**
 * Barvy kategorie vybavení podle id kategorie z databáze.
 * @param categoryId id kategorie vybavení
 * @return barvy pruhu a pozadí; pro neznámé id barvy fallback
 */
@Composable
public fun catalogCategoryColors(categoryId: Int): CatalogCategoryColors {
    val palette: CatalogPaletteColors = LocalCatalogPalette.current
    return palette.categoryColors[categoryId] ?: palette.fallback
}

/**
 * Přídavné barvy nedostupného stavu (druhý pruh a pozadí karty).
 * @param status stav vybavení
 * @return barvy přídavného pruhu a pozadí; pro AVAILABLE barvy fallback (funkce se pro tento stav nevolá)
 */
@Composable
public fun catalogStatusAccent(status: EquipmentStatus): CatalogCategoryColors {
    val palette: CatalogPaletteColors = LocalCatalogPalette.current
    return palette.statusAccents[status] ?: palette.fallback
}

/**
 * Šířka přídavného pruhu stavu: pronajato 4 dp, údržba 8 dp.
 * @param status stav vybavení
 * @return šířka přídavného pruhu; pro AVAILABLE 0 dp (větev kvůli vyčerpávajícímu when, nepoužívá se)
 */
public fun catalogStatusStripeWidth(status: EquipmentStatus): Dp = when (status) {
    EquipmentStatus.RENTED -> 4.dp
    EquipmentStatus.MAINTENANCE -> 8.dp
    EquipmentStatus.AVAILABLE -> 0.dp
}
