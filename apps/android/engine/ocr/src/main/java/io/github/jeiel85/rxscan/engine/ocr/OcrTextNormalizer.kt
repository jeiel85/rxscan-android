package io.github.jeiel85.rxscan.engine.ocr

import java.text.Normalizer

/**
 * Deterministic OCR text normalization (03_OCR_PIPELINE.md §6).
 *
 * Allowed: Unicode NFKC, whitespace collapse, and standardizing dosage-unit
 * spelling through a versioned dictionary. Forbidden: inferring a missing digit,
 * unit, or dosage-form suffix, and overwriting the raw recognizer text (the
 * caller keeps raw text in [OcrToken.rawText]).
 */
object OcrTextNormalizer {
    const val DICTIONARY_VERSION: String = "ocr-normalize-v1"

    // Canonical spelling for dosage units that appear right after a number.
    // NFKC already folds compatibility squares (㎎ -> "mg") and full-width forms.
    private val unitCanon: Map<String, String> = mapOf(
        "mg" to "mg",
        "ml" to "mL",
        "mcg" to "mcg",
        "ug" to "mcg",
        "µg" to "mcg",
        "g" to "g",
        "kg" to "kg",
        "iu" to "IU",
    )

    private val numberUnit = Regex("""(\d)\s*(mg|ml|mcg|ug|µg|kg|iu|g)\b""", RegexOption.IGNORE_CASE)
    private val whitespace = Regex("""\s+""")

    fun normalize(raw: String): String {
        val nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        val collapsed = whitespace.replace(nfkc, " ").trim()
        return numberUnit.replace(collapsed) { match ->
            val digit = match.groupValues[1]
            val unit = unitCanon.getValue(match.groupValues[2].lowercase())
            digit + unit
        }
    }
}
