package io.github.jeiel85.rxscan.feature.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.github.jeiel85.rxscan.engine.imagequality.ImageQualityAnalyzer
import io.github.jeiel85.rxscan.engine.imagequality.LumaImage
import io.github.jeiel85.rxscan.engine.imagequality.QualityReport

/**
 * Throttled live-frame quality guidance (03_OCR_PIPELINE.md §2). Converts a
 * downsampled Y plane to [LumaImage] and runs the pure [ImageQualityAnalyzer].
 * This is guidance only — full OCR never runs here, only on the captured still.
 *
 * Text height is not measurable from a live frame, so a high value is passed to
 * skip the TEXT_TOO_SMALL check until capture; live guidance reflects blur,
 * glare, exposure, and clipping.
 */
class QualityFrameAnalyzer(
    private val analyzer: ImageQualityAnalyzer = ImageQualityAnalyzer(),
    private val targetWidth: Int = 160,
    private val onResult: (QualityReport) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            onResult(analyzer.evaluate(toLumaImage(image), estimatedTextHeightPx = Int.MAX_VALUE))
        } finally {
            image.close()
        }
    }

    private fun toLumaImage(image: ImageProxy): LumaImage {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val srcW = image.width
        val srcH = image.height
        val step = maxOf(1, srcW / targetWidth)
        val outW = maxOf(1, srcW / step)
        val outH = maxOf(1, srcH / step)
        val out = IntArray(outW * outH)
        var index = 0
        var y = 0
        while (y < outH) {
            val srcY = y * step
            var x = 0
            while (x < outW) {
                val pos = srcY * rowStride + x * step * pixelStride
                out[index++] = buffer.get(pos).toInt() and 0xFF
                x++
            }
            y++
        }
        return LumaImage(outW, outH, out)
    }
}
