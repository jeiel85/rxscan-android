package io.github.jeiel85.rxscan.engine.ocr

import android.graphics.Bitmap

/** A preprocessed image variant handed to the on-device recognizer. */
data class OcrImage(
    val bitmap: Bitmap,
    val variant: ImageVariant,
    val rotationDegrees: Int = 0,
)

/**
 * On-device OCR boundary. Implementations run entirely locally — no scan-derived
 * network request is permitted from the scan flow (03_OCR_PIPELINE.md, AGENTS.md).
 */
interface OcrEngine {
    suspend fun recognize(image: OcrImage): OcrPassResult
    fun close()
}
