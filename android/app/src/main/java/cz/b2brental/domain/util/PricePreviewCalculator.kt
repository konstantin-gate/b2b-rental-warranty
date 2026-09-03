package cz.b2brental.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Předběžný výpočet nájemní smlouvy na straně klienta.
 * Přesná kopie logiky `backend/.../services/PricingService.kt:23-33`.
 * Měsíční sazba = součet sazeb; depozit = 30 % měsíční sazby (HALF_UP, scale 2);
 * celková částka = měsíční sazba × počet měsíců + depozit.
 */
@Suppress("HardCodedStringLiteral")
public object PricePreviewCalculator {

    /**
     * Vypočítá předběžný přehled cen.
     * @param monthlyRates seznam měsíčních sazeb jako řetězců (např. ["4500.00", "3600.00"])
     * @param months počet měsíců (1–36)
     * @return data class s předběžnými částkami
     * @throws IllegalArgumentException pokud months není v rozsahu 1–36 nebo seznam sazeb je prázdný
     */
    @Suppress("KDocMissingDocumentation")
    public fun preview(monthlyRates: List<String>, months: Int): PricePreview {
        require(months in 1..36) { "Počet měsíců musí být v rozsahu 1–36" }
        require(monthlyRates.isNotEmpty()) { "Seznam měsíčních sazeb nesmí být prázdný" }

        val monthly = monthlyRates
            .map { BigDecimal(it) }
            .fold(BigDecimal.ZERO) { acc, bd -> acc + bd }
            .setScale(2, RoundingMode.HALF_UP)

        val deposit = monthly
            .multiply(BigDecimal("0.30"))
            .setScale(2, RoundingMode.HALF_UP)

        val total = monthly
            .multiply(BigDecimal(months))
            .add(deposit)
            .setScale(2, RoundingMode.HALF_UP)

        return PricePreview(
            monthlyAmount = monthly.toPlainString(),
            deposit = deposit.toPlainString(),
            totalAmount = total.toPlainString()
        )
    }
}

/**
 * Předběžný přehled cen nájemní smlouvy.
 * @property monthlyAmount měsíční částka (součet sazeb všech položek)
 * @property deposit depozit (30 % měsíční částky, HALF_UP, scale 2)
 * @property totalAmount celková částka (měsíční × počet měsíců + depozit)
 */
public data class PricePreview(
    val monthlyAmount: String,
    val deposit: String,
    val totalAmount: String,
)
