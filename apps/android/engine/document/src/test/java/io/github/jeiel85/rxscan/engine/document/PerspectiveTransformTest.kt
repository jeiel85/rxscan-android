package io.github.jeiel85.rxscan.engine.document

import io.github.jeiel85.rxscan.core.model.Pt
import io.github.jeiel85.rxscan.core.model.Quad
import org.junit.Assert.assertEquals
import org.junit.Test

class PerspectiveTransformTest {
    private val eps = 1e-3f

    // A perspective-distorted capture of a document, in image pixels.
    private val distorted = Quad(
        topLeft = Pt(40f, 60f),
        topRight = Pt(360f, 30f),
        bottomRight = Pt(390f, 470f),
        bottomLeft = Pt(20f, 430f),
    )
    private val corrected = PerspectiveTransform.rectangle(300f, 400f)

    private fun assertPt(expected: Pt, actual: Pt) {
        assertEquals("x", expected.x, actual.x, eps)
        assertEquals("y", expected.y, actual.y, eps)
    }

    @Test
    fun mapsSourceCornersOntoDestinationRectangle() {
        val t = PerspectiveTransform.fromCorners(distorted, corrected)
        assertPt(corrected.topLeft, t.map(distorted.topLeft))
        assertPt(corrected.topRight, t.map(distorted.topRight))
        assertPt(corrected.bottomRight, t.map(distorted.bottomRight))
        assertPt(corrected.bottomLeft, t.map(distorted.bottomLeft))
    }

    @Test
    fun inverseMapsCorrectedBoxesBackToOriginal() {
        val forward = PerspectiveTransform.fromCorners(distorted, corrected)
        val back = forward.invert()
        // A box detected in the corrected image maps back onto the original quad.
        assertPt(distorted.topLeft, back.map(corrected.topLeft))
        assertPt(distorted.bottomRight, back.map(corrected.bottomRight))
    }

    @Test
    fun forwardThenInverseRoundTripsInteriorPoint() {
        val forward = PerspectiveTransform.fromCorners(distorted, corrected)
        val back = forward.invert()
        val p = Pt(123f, 270f)
        assertPt(p, back.map(forward.map(p)))
    }

    @Test
    fun identityQuadMapsToItself() {
        val square = Quad(Pt(0f, 0f), Pt(10f, 0f), Pt(10f, 10f), Pt(0f, 10f))
        val t = PerspectiveTransform.fromCorners(square, square)
        assertPt(Pt(4f, 7f), t.map(Pt(4f, 7f)))
    }
}
