package io.github.jeiel85.rxscan.engine.imagequality

import io.github.jeiel85.rxscan.core.model.Pt
import io.github.jeiel85.rxscan.core.model.Quad
import io.github.jeiel85.rxscan.core.model.ScanError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageQualityAnalyzerTest {
    private val analyzer = ImageQualityAnalyzer()
    private val adequateTextPx = 24

    private fun filled(width: Int, height: Int, value: Int): LumaImage =
        LumaImage(width, height, IntArray(width * height) { value })

    /** Vertical stripes: high Laplacian variance (sharp), no glare, no shadow. */
    private fun sharp(width: Int = 16, height: Int = 16): LumaImage =
        LumaImage(width, height, IntArray(width * height) { i ->
            if ((i % width) % 2 == 0) 30 else 220
        })

    @Test
    fun sharpWellExposedImagePasses() {
        val report = analyzer.evaluate(sharp(), adequateTextPx)
        assertTrue(report.flags.toString(), report.accepted)
        assertEquals(emptyList<String>(), report.guidanceKo)
        assertEquals(0.0, report.signals.glareRatio, 1e-9)
    }

    @Test
    fun flatImageFlagsBlurWithGuidance() {
        val report = analyzer.evaluate(filled(16, 16, 128), adequateTextPx)
        assertTrue(QualityFlag.BLUR in report.flags)
        assertFalse(report.accepted)
        assertEquals(ScanError.CAPTURE_TOO_BLURRY, report.primaryError)
        assertTrue(report.guidanceKo.contains(ScanError.CAPTURE_TOO_BLURRY.guidanceKo))
    }

    @Test
    fun overexposedImageFlagsGlare() {
        val report = analyzer.evaluate(filled(16, 16, 255), adequateTextPx)
        assertTrue(QualityFlag.GLARE in report.flags)
        assertEquals(1.0, report.signals.glareRatio, 1e-9)
    }

    @Test
    fun darkImageFlagsUnderexposure() {
        val report = analyzer.evaluate(filled(16, 16, 2), adequateTextPx)
        assertTrue(QualityFlag.UNDEREXPOSURE in report.flags)
    }

    @Test
    fun smallTextFlagsTextTooSmall() {
        val report = analyzer.evaluate(sharp(), estimatedTextHeightPx = 8)
        assertTrue(QualityFlag.TEXT_TOO_SMALL in report.flags)
        assertTrue(report.guidanceKo.contains(ScanError.TEXT_TOO_SMALL.guidanceKo))
    }

    @Test
    fun documentTouchingFrameEdgeFlagsClipping() {
        // A boundary whose top-left corner sits on the frame edge is clipped.
        val boundary = Quad(
            topLeft = Pt(0f, 0f),
            topRight = Pt(15f, 1f),
            bottomRight = Pt(15f, 15f),
            bottomLeft = Pt(1f, 15f),
        )
        val report = analyzer.evaluate(sharp(), adequateTextPx, documentBoundary = boundary)
        assertTrue(QualityFlag.CLIPPING in report.flags)
        assertTrue(report.guidanceKo.contains(ScanError.DOCUMENT_CLIPPED.guidanceKo))
    }

    @Test
    fun wellInsetDocumentDoesNotFlagClipping() {
        val boundary = Quad(
            topLeft = Pt(40f, 40f),
            topRight = Pt(260f, 40f),
            bottomRight = Pt(260f, 360f),
            bottomLeft = Pt(40f, 360f),
        )
        val report = analyzer.evaluate(sharp(300, 400), adequateTextPx, documentBoundary = boundary)
        assertFalse(QualityFlag.CLIPPING in report.flags)
    }

    @Test
    fun guidanceFollowsStableEnumOrderAcrossFlags() {
        // Flat + dark + tiny text -> BLUR, UNDEREXPOSURE, TEXT_TOO_SMALL in enum order.
        val report = analyzer.evaluate(filled(16, 16, 2), estimatedTextHeightPx = 4)
        val expected = listOf(
            ScanError.CAPTURE_TOO_BLURRY.guidanceKo,
            ScanError.OCR_INSUFFICIENT.guidanceKo,
            ScanError.TEXT_TOO_SMALL.guidanceKo,
        )
        assertEquals(expected, report.guidanceKo)
    }
}
