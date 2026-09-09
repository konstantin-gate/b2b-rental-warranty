package cz.b2brental.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.presentation.theme.CatalogCategoryColors
import cz.b2brental.presentation.theme.catalogCategoryColors
import cz.b2brental.presentation.theme.catalogStatusAccent
import cz.b2brental.presentation.theme.catalogStatusStripeWidth

/**
 * Karta pozice katalogu ve schváleném barevném stylu: svislý pruh kategorie (8 dp),
 * podmíněný druhý pruh stavu (pronajato 4 dp, údržba 8 dp) a tónované pozadí karty.
 * @param categoryId id kategorie z databáze (neznámé id dostane šedý fallback)
 * @param status stav vybavení (AVAILABLE = bez druhého pruhu)
 * @param onClick callback kliknutí na kartu (null = karta neklikatelná)
 * @param modifier modifikátor aplikovaný na Card (padding vnějšího rozmístění)
 * @param content obsah karty vpravo od pruhů
 */
@Suppress("KDocMissingDocumentation")
@Composable
public fun StyledEquipmentCard(
    categoryId: Int,
    status: EquipmentStatus,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val categoryColors: CatalogCategoryColors = catalogCategoryColors(categoryId)
    val statusAccent: CatalogCategoryColors? =
        if (status == EquipmentStatus.AVAILABLE) null else catalogStatusAccent(status)
    val cardContainerColor: Color = statusAccent?.container ?: categoryColors.container
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Pruh kategorie na levém okraji karty.
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(categoryColors.stripe),
            )
            // Druhý pruh stavu — jen pro nedostupné položky; šířka podle stavu (pronajato 4 dp, údržba 8 dp).
            if (statusAccent != null) {
                Box(
                    modifier = Modifier
                        .width(catalogStatusStripeWidth(status))
                        .fillMaxHeight()
                        .background(statusAccent.stripe),
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}
