@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.Severity
import cz.b2brental.db.WarrantyVerdict

/** Výsledek diagnostiky AI */
public data class DiagnosisResult(
    public val possibleCause: String,
    public val severity: Severity,
    public val recommendation: String,
)

/** Služba pro AI operace (fallback verze pro Vlnu D bez HTTP volání) */
public object AiService {
    /** Diagnostika poruchy (fallback) */
    public fun diagnose(
        description: String,
        photoBase64: String?,
    ): DiagnosisResult =
        DiagnosisResult(
            possibleCause = "Příčina nebyla určena — AI není dostupná",
            severity = Severity.medium,
            recommendation = "Vyžaduje ruční diagnostiku technikem",
        )

    /** Vysvětlení záručního verdiktu (fallback) */
    public fun explainVerdict(
        verdict: WarrantyVerdict,
        reason: String?,
    ): String = "AI není dostupná — verdikt vypočítán pravidly: ${reason ?: "bez dodatečného odůvodnění"}"

    /** Odpověď AI asistenta (fallback) */
    public fun assistantReply(
        message: String,
        context: String,
    ): String = "AI asistent není dostupný"
}
