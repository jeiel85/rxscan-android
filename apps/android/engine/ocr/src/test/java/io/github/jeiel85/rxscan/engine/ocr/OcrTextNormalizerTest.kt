package io.github.jeiel85.rxscan.engine.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextNormalizerTest {
    @Test
    fun nfkcFoldsFullWidthDigitsAndUnitSquares() {
        // Full-width "500" + ㎎ compatibility square -> "500mg".
        assertEquals("500mg", OcrTextNormalizer.normalize("５００㎎"))
    }

    @Test
    fun standardizesUnitSpellingAfterNumber() {
        assertEquals("타이레놀 500mg", OcrTextNormalizer.normalize("타이레놀 500 MG"))
        assertEquals("5mL", OcrTextNormalizer.normalize("5 ml"))
        assertEquals("250mcg", OcrTextNormalizer.normalize("250 ug"))
    }

    @Test
    fun collapsesWhitespace() {
        assertEquals("1일 3회", OcrTextNormalizer.normalize("1일   3회"))
        assertEquals("식후 30분", OcrTextNormalizer.normalize("  식후 30분  "))
    }

    @Test
    fun doesNotInventMissingUnitsOrDigits() {
        // No unit present -> nothing is appended; bare number stays bare.
        assertEquals("타이레놀 500", OcrTextNormalizer.normalize("타이레놀 500"))
        // "분" is not a dosage unit and must be left untouched.
        assertEquals("30분", OcrTextNormalizer.normalize("30분"))
    }

    @Test
    fun rawTextIsNotRequiredToEqualNormalized() {
        val raw = "５００ＭＧ"
        val normalized = OcrTextNormalizer.normalize(raw)
        assertEquals("500mg", normalized)
        // The caller keeps the raw string verbatim; normalization returns a new value.
        assertEquals("５００ＭＧ", raw)
    }
}
