package io.github.jeiel85.rxscan.engine.imagequality

import io.github.jeiel85.rxscan.core.model.Quad
import io.github.jeiel85.rxscan.core.model.ScanError

/** Calibrated quality flags from 03_OCR_PIPELINE.md §2/§10. */
enum class QualityFlag(val scanError: ScanError) {
    BLUR(ScanError.CAPTURE_TOO_BLURRY),
    GLARE(ScanError.GLARE_OVER_TEXT),
    UNDEREXPOSURE(ScanError.OCR_INSUFFICIENT),
    TEXT_TOO_SMALL(ScanError.TEXT_TOO_SMALL),
    CLIPPING(ScanError.DOCUMENT_CLIPPED),
}

/** Raw measured signals; kept separate from the pass/fail decision for auditability. */
data class QualitySignals(
    val blurVariance: Double,
    val glareRatio: Double,
    val underexposureRatio: Double,
    val estimatedTextHeightPx: Int,
    /** Minimum distance of any document corner to the frame edge, or null when no boundary was detected. */
    val documentMarginPx: Float?,
)

/**
 * Calibrated thresholds. Injectable so calibration and tests do not hard-code
 * magic numbers at the call site.
 */
data class QualityThresholds(
    val minBlurVariance: Double = 120.0,
    val maxGlareRatio: Double = 0.10,
    val maxUnderexposureRatio: Double = 0.35,
    val minTextHeightPx: Int = 16,
    val minDocumentMarginPx: Float = 4f,
    val glareLumaFloor: Int = 250,
    val shadowLumaCeil: Int = 8,
)

/**
 * Result of the capture quality gate. [accepted] is true only when no flag
 * remains (03_OCR_PIPELINE.md §2 "no critical quality flag remains"). Every flag
 * yields actionable Korean guidance — never a medicine guess (AGENTS.md).
 */
data class QualityReport(
    val signals: QualitySignals,
    val flags: Set<QualityFlag>,
    val accepted: Boolean,
    val guidanceKo: List<String>,
    val primaryError: ScanError?,
)

/**
 * Pure capture-quality gate: variance-of-Laplacian blur, overexposure (glare)
 * and underexposure ratios, document-clipping (from the detected boundary), and
 * text-size adequacy.
 *
 * The median text height and document boundary are supplied by the on-device
 * document/OCR stage because they cannot be measured from raw luminance alone;
 * passing them in keeps this gate honest and deterministic.
 */
class ImageQualityAnalyzer(private val thresholds: QualityThresholds = QualityThresholds()) {

    fun evaluate(
        image: LumaImage,
        estimatedTextHeightPx: Int,
        documentBoundary: Quad? = null,
    ): QualityReport {
        val margin = documentBoundary?.let { boundaryMargin(it, image.width, image.height) }
        val signals = QualitySignals(
            blurVariance = laplacianVariance(image),
            glareRatio = ratioAtLeast(image, thresholds.glareLumaFloor),
            underexposureRatio = ratioAtMost(image, thresholds.shadowLumaCeil),
            estimatedTextHeightPx = estimatedTextHeightPx,
            documentMarginPx = margin,
        )

        val flags = buildSet {
            if (signals.blurVariance < thresholds.minBlurVariance) add(QualityFlag.BLUR)
            if (signals.glareRatio > thresholds.maxGlareRatio) add(QualityFlag.GLARE)
            if (signals.underexposureRatio > thresholds.maxUnderexposureRatio) add(QualityFlag.UNDEREXPOSURE)
            if (estimatedTextHeightPx < thresholds.minTextHeightPx) add(QualityFlag.TEXT_TOO_SMALL)
            if (margin != null && margin < thresholds.minDocumentMarginPx) add(QualityFlag.CLIPPING)
        }

        // Stable, user-facing order regardless of set iteration order.
        val ordered = QualityFlag.entries.filter { it in flags }
        return QualityReport(
            signals = signals,
            flags = flags,
            accepted = flags.isEmpty(),
            guidanceKo = ordered.map { it.scanError.guidanceKo },
            primaryError = ordered.firstOrNull()?.scanError,
        )
    }

    /** Smallest distance from any document corner to the frame edge (negative if outside). */
    private fun boundaryMargin(boundary: Quad, width: Int, height: Int): Float {
        var min = Float.MAX_VALUE
        for (corner in boundary.corners) {
            val distances = floatArrayOf(corner.x, corner.y, width - corner.x, height - corner.y)
            for (d in distances) if (d < min) min = d
        }
        return min
    }

    private fun laplacianVariance(image: LumaImage): Double {
        if (image.width < 3 || image.height < 3) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until image.height - 1) {
            for (x in 1 until image.width - 1) {
                val lap = 4 * image.at(x, y) -
                    image.at(x - 1, y) - image.at(x + 1, y) -
                    image.at(x, y - 1) - image.at(x, y + 1)
                sum += lap
                sumSq += lap.toDouble() * lap
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        return sumSq / count - mean * mean
    }

    private fun ratioAtLeast(image: LumaImage, floor: Int): Double {
        var hits = 0
        for (v in image.luma) if (v >= floor) hits++
        return hits.toDouble() / image.luma.size
    }

    private fun ratioAtMost(image: LumaImage, ceil: Int): Double {
        var hits = 0
        for (v in image.luma) if (v <= ceil) hits++
        return hits.toDouble() / image.luma.size
    }
}
