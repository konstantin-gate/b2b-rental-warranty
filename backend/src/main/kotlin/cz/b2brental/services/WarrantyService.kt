@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.WarrantyVerdict
import java.time.LocalDate

/** Výsledek hodnocení záruky.
 * @property verdict verdikt záručního krytí
 * @property reason vysvětlení verdiktu
 */
public data class WarrantyEvaluation(
    public val verdict: WarrantyVerdict,
    public val reason: String,
)

/** Deterministický engine pro vyhodnocení záruky bez přístupu k DB */
public object WarrantyService {
    /** Vyhodnotí záruční krytí podle pravidel R1, R2 a R4; pravidla se aplikují v uvedeném pořadí.
     * @param contractStartDate datum začátku smlouvy; null znamená žádnou aktivní smlouvu
     * @param warrantyMonths délka záruky v měsících; null znamená 0
     * @param today aktuální datum pro porovnání s koncem záruky
     */
    public fun evaluate(
        contractStartDate: LocalDate?,
        warrantyMonths: Int?,
        today: LocalDate,
    ): WarrantyEvaluation {
        // R1: žádná aktivní smlouva
        if (contractStartDate == null) {
            return WarrantyEvaluation(WarrantyVerdict.review_required, "Není aktivní smlouva — vyžaduje ruční posouzení")
        }

        val warrantyEnd: LocalDate = contractStartDate.plusMonths(warrantyMonths?.toLong() ?: 0L)

        // R2: záruka vypršela
        if (today.isAfter(warrantyEnd)) {
            return WarrantyEvaluation(WarrantyVerdict.not_covered, "Záruka vypršela dne $warrantyEnd")
        }

        // R4: záruka platí
        return WarrantyEvaluation(WarrantyVerdict.covered, "Záruka platí do $warrantyEnd")
    }
}
