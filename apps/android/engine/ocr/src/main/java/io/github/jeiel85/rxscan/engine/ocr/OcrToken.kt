package io.github.jeiel85.rxscan.engine.ocr

import io.github.jeiel85.rxscan.core.model.Pt

/** Which preprocessing variant produced a token (03_OCR_PIPELINE.md §3). */
enum class ImageVariant {
    PERSPECTIVE_COLOR,
    GRAYSCALE,
    CONTRAST_ENHANCED,
    ADAPTIVE_THRESHOLD,
}

/** Per-token uncertainty markers; never used to fabricate content. */
enum class OcrFlag {
    LOW_CONFIDENCE,
    CONFUSABLE_CHARACTERS,
    JOINED_BY_LAYOUT,
    SENSITIVE_PII,
    FROM_SECONDARY_PASS,
}

/**
 * Auditable OCR output unit (03_OCR_PIPELINE.md §5). [rawText] is the recognizer
 * output verbatim; [normalizedText] is the deterministic normalization. The two
 * are stored separately — normalization must never overwrite raw text.
 */
data class OcrToken(
    val rawText: String,
    val normalizedText: String,
    val polygon: List<Pt>,
    val lineId: String,
    val blockId: String,
    val sourceVariant: ImageVariant,
    val engineConfidence: Float?,
    val consensusCount: Int,
    val flags: Set<OcrFlag>,
)

/** A recognition pass result for one image variant before consensus merging. */
data class OcrPassResult(
    val variant: ImageVariant,
    val tokens: List<OcrToken>,
)
