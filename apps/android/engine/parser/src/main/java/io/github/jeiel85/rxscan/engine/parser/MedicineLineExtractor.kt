package io.github.jeiel85.rxscan.engine.parser

import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.ReleaseForm
import io.github.jeiel85.rxscan.core.model.Strength

/**
 * Deterministic medicine-line field extraction (03_OCR_PIPELINE.md §7,
 * 04_DRUG_MATCHING_ENGINE.md §2). Pulls strength, dosage form, release form,
 * manufacturer, and a product-name fragment from a normalized medicine row.
 *
 * Missing values are left null/UNKNOWN — never inferred (AGENTS.md). In
 * particular, the absence of "서방"/ER does NOT imply IMMEDIATE release.
 */
object MedicineLineExtractor {
    private val strengthPattern =
        Regex("""(\d+(?:\.\d+)?)\s*(mg|mcg|g|mL|IU|%)""", RegexOption.IGNORE_CASE)
    private val itemCodePattern = Regex("""\b\d{9,}\b""")
    private val releaseExtended = Regex("""서방|\bER\b|\bCR\b|\bXR\b""", RegexOption.IGNORE_CASE)

    private val formSuffixes = linkedMapOf(
        "캡슐" to DosageForm.CAPSULE,
        "연질캡슐" to DosageForm.CAPSULE,
        "정" to DosageForm.TABLET,
        "과립" to DosageForm.GRANULE,
        "산" to DosageForm.POWDER,
        "시럽" to DosageForm.SYRUP,
        "점안액" to DosageForm.DROPS,
        "액" to DosageForm.LIQUID,
        "주사" to DosageForm.INJECTION,
        "주" to DosageForm.INJECTION,
        "연고" to DosageForm.OINTMENT,
        "크림" to DosageForm.OINTMENT,
        "패치" to DosageForm.PATCH,
    )
    private val manufacturerSuffix = Regex("""\S*(제약|약품|파마|팜|메디칼|바이오)\S*""")

    fun extract(rawLine: String, normalizedLine: String): RecognizedDrugFields {
        val strengthMatch = strengthPattern.find(normalizedLine)
        val strength = strengthMatch?.let {
            Strength(it.groupValues[1].toDouble(), canonicalUnit(it.groupValues[2]))
        }
        val dosageForm = formSuffixes.entries.firstOrNull { normalizedLine.contains(it.key) }?.value
            ?: DosageForm.UNKNOWN
        val releaseForm = if (releaseExtended.containsMatchIn(normalizedLine)) {
            ReleaseForm.EXTENDED
        } else {
            ReleaseForm.UNKNOWN
        }
        val manufacturer = manufacturerSuffix.find(normalizedLine)?.value
        val itemCode = itemCodePattern.find(normalizedLine)?.value
        val productName = productNameFragment(normalizedLine, strengthMatch?.range?.first)

        return RecognizedDrugFields(
            rawLine = rawLine,
            normalizedLine = normalizedLine,
            itemCode = itemCode,
            productName = productName,
            strength = strength,
            dosageForm = dosageForm,
            releaseForm = releaseForm,
            manufacturer = manufacturer,
        )
    }

    private fun canonicalUnit(unit: String): String = when (unit.lowercase()) {
        "ml" -> "mL"
        "iu" -> "IU"
        else -> unit.lowercase()
    }

    /** Product-name fragment: text before the strength marker, else the leading token. */
    private fun productNameFragment(normalizedLine: String, strengthStart: Int?): String? {
        val head = if (strengthStart != null && strengthStart > 0) {
            normalizedLine.substring(0, strengthStart)
        } else {
            normalizedLine
        }.trim()
        if (head.isEmpty()) return null
        return head.substringBefore(' ').ifEmpty { head }
    }
}
