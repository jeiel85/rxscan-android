package io.github.jeiel85.rxscan.engine.parser

import io.github.jeiel85.rxscan.core.model.Pt
import io.github.jeiel85.rxscan.engine.ocr.ImageVariant
import io.github.jeiel85.rxscan.engine.ocr.OcrToken
import org.junit.Assert.assertEquals
import org.junit.Test

class LineClustererTest {
    private fun tok(text: String, x: Float, y: Float, w: Float = 40f, h: Float = 16f): OcrToken =
        OcrToken(
            rawText = text,
            normalizedText = text,
            polygon = listOf(Pt(x, y), Pt(x + w, y), Pt(x + w, y + h), Pt(x, y + h)),
            lineId = "l",
            blockId = "b",
            sourceVariant = ImageVariant.PERSPECTIVE_COLOR,
            engineConfidence = null,
            consensusCount = 1,
            flags = emptySet(),
        )

    @Test
    fun clustersTokensIntoRowsOrderedLeftToRight() {
        // Two rows; tokens supplied out of reading order.
        val tokens = listOf(
            tok("5mg", x = 120f, y = 12f),
            tok("한미약품", x = 10f, y = 60f),
            tok("암로디핀정", x = 10f, y = 10f),
        )
        val rows = LineClusterer.clusterRows(tokens)
        assertEquals(2, rows.size)
        assertEquals("암로디핀정 5mg", rows[0].text)
        assertEquals("한미약품", rows[1].text)
    }

    @Test
    fun emptyInputYieldsNoRows() {
        assertEquals(0, LineClusterer.clusterRows(emptyList()).size)
    }

    @Test
    fun deterministicForSameInput() {
        val tokens = listOf(
            tok("a", x = 0f, y = 0f),
            tok("b", x = 50f, y = 2f),
            tok("c", x = 0f, y = 100f),
        )
        val first = LineClusterer.clusterRows(tokens).map { it.text }
        val second = LineClusterer.clusterRows(tokens).map { it.text }
        assertEquals(first, second)
        assertEquals(listOf("a b", "c"), first)
    }
}
