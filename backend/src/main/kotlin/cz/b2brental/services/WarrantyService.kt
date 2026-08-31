@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.WarrantyVerdict
import java.time.LocalDate

/** Výsledek hodnocení záruky */
public data class WarrantyEvaluation(
    public val verdict: WarrantyVerdict,
    public val reason: String,
)

/** Deterministický engine pro vyhodnocení záruky bez přístupu k DB */
public object WarrantyService {
    /** Vyhodnotí záruční krytí podle pravidel R1–R4; pravidla se aplikují v uvedeném pořadí */
    public fun evaluate(
        contractStartDate: LocalDate?,
        warrantyMonths: Int?,
        description: String,
        excludedCauses: List<String>,
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

        // R3: vyloučená příčina (porovnání bez ohledu na velikost písmen)
        val descLower: String = description.lowercase()
        for (excluded in excludedCauses) {
            val key: String = excluded.lowercase().trim()
            if (key.isNotEmpty() && descLower.contains(key)) {
                return WarrantyEvaluation(WarrantyVerdict.not_covered, "Příčina není kryta zárukou: $excluded")
            }
        }

        // R4: záruka platí
        return WarrantyEvaluation(WarrantyVerdict.covered, "Záruka platí do $warrantyEnd")
    }
}
