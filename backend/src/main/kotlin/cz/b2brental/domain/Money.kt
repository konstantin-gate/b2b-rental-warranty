package cz.b2brental.domain

import java.math.BigDecimal
import java.math.RoundingMode
import javax.money.CurrencyUnit
import javax.money.Monetary
import org.javamoney.moneta.Money

// Jednotná měna projektu
val CZK: CurrencyUnit = Monetary.getCurrency("CZK")

// Sazba vkladu: přesně 30 %
val DEPOSIT_RATE: BigDecimal = BigDecimal("0.30")

// Převod hodnoty z DB (BigDecimal ze sloupce decimal(12,2)) na peněžní typ CZK
fun BigDecimal.toCzkMoney(): Money = Money.of(this, CZK)

// Převod peněžní hodnoty na BigDecimal se škálou 2 pro zápis do DB (decimal(12,2)) a do JSON
fun Money.toDbBigDecimal(): BigDecimal =
    number.numberValue(BigDecimal::class.java).setScale(2, RoundingMode.HALF_UP)

// Procento od částky: násobení a zaokrouhlení HALF_UP na 2 desetinná místa
// (používá se výhradně pro výpočet vkladu v PricingService)
fun Money.czkPercent(percent: BigDecimal): Money =
    Money.of(toDbBigDecimal().multiply(percent).setScale(2, RoundingMode.HALF_UP), CZK)

// Formát částky pro PDF dokumenty: "110700.00 Kč"
fun BigDecimal.formatCzk(): String = setScale(2, RoundingMode.HALF_UP).toPlainString() + " Kč"
