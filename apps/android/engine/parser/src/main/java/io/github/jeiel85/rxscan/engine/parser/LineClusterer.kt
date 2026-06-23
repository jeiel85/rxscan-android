package io.github.jeiel85.rxscan.engine.parser

import io.github.jeiel85.rxscan.engine.ocr.OcrToken
import kotlin.math.abs

/** A clustered text row: tokens ordered left-to-right with their joined normalized text. */
data class TextRow(
    val tokens: List<OcrToken>,
    val yCenter: Float,
    val text: String,
)

/**
 * Deterministic row clustering (03_OCR_PIPELINE.md §7). Groups OCR tokens into
 * rows by vertical overlap of their baselines, then orders each row left-to-right.
 * Pure function: same tokens always yield the same rows.
 */
object LineClusterer {
    fun clusterRows(tokens: List<OcrToken>): List<TextRow> {
        val positioned = tokens.filter { it.polygon.isNotEmpty() }
        if (positioned.isEmpty()) return emptyList()

        val sorted = positioned.sortedWith(compareBy({ yCenter(it) }, { xLeft(it) }))
        val rows = mutableListOf<MutableList<OcrToken>>()
        val rowCenters = mutableListOf<Float>()
        for (token in sorted) {
            val cy = yCenter(token)
            val tolerance = 0.6f * height(token)
            val lastIndex = rows.lastIndex
            if (lastIndex >= 0 && abs(rowCenters[lastIndex] - cy) <= tolerance) {
                rows[lastIndex].add(token)
                rowCenters[lastIndex] = rows[lastIndex].map { yCenter(it) }.average().toFloat()
            } else {
                rows.add(mutableListOf(token))
                rowCenters.add(cy)
            }
        }
        return rows.map { row ->
            val ordered = row.sortedBy { xLeft(it) }
            TextRow(
                tokens = ordered,
                yCenter = ordered.map { yCenter(it) }.average().toFloat(),
                text = ordered.joinToString(" ") { it.normalizedText }.trim(),
            )
        }
    }

    private fun yCenter(token: OcrToken): Float = token.polygon.map { it.y }.average().toFloat()

    private fun height(token: OcrToken): Float {
        val ys = token.polygon.map { it.y }
        val h = (ys.max() - ys.min())
        return if (h <= 0f) 1f else h
    }

    private fun xLeft(token: OcrToken): Float = token.polygon.minOf { it.x }
}
