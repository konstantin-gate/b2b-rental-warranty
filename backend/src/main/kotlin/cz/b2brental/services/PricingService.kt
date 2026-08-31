@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.domain.DEPOSIT_RATE
import cz.b2brental.domain.czkPercent
import org.javamoney.moneta.Money

/** Výsledek cenové kalkulace; všechny částky jsou typu Money v měně CZK */
public data class PricingResult(
    public val monthlyAmount: Money,
    public val deposit: Money,
    public val totalAmount: Money,
)

/** Čistá cenová služba bez závislosti na databázi */
public object PricingService {
    /** Spočítá měsíční částku, kauci a celkovou sumu nájmu */
    public fun calc(
        monthlyRates: List<Money>,
        months: Int,
    ): PricingResult {
        require(monthlyRates.isNotEmpty()) { "Seznam sazeb nesmí být prázdný" }
        require(months in 1..36) { "Doba nájmu musí být v rozmezí 1 až 36 měsíců" }

        // Součet sazeb; Money.add kontroluje shodu měn
        val monthly: Money = monthlyRates.reduce { acc, m -> acc.add(m) }

        // Vklad: přesně 30 %, HALF_UP, 2 desetinná místa
        val deposit: Money = monthly.czkPercent(DEPOSIT_RATE)

        // Celková částka: měsíční sazba × počet měsíců + vklad
        val total: Money = monthly.multiply(months.toLong()).add(deposit)

        return PricingResult(monthly, deposit, total)
    }
}
