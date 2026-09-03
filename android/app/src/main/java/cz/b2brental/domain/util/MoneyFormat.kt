package cz.b2brental.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formátování peněžních částek v CZK pro zobrazení v UI.
 * Vstup: řetězec "110700.00" → výstup: "110 700,00 Kč".
 */
@Suppress("HardCodedStringLiteral")
public object MoneyFormat {

    private val CZK_FORMAT: DecimalFormat = DecimalFormat(
        "#,##0.00 Kč",
        DecimalFormatSymbols(Locale("cs", "CZ")).apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }
    )

    /**
     * Formátuje surový řetězec částky na čitelný formát CZK.
     * @param raw surový řetězec (např. "110700.00")
     * @return formátovaný řetězec (např. "110 700,00 Kč")
     */
    public fun formatCzk(raw: String): String {
        val bd = BigDecimal(raw).setScale(2, RoundingMode.HALF_UP)
        return CZK_FORMAT.format(bd)
    }
}
