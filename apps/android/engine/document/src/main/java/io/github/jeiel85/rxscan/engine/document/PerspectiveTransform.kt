package io.github.jeiel85.rxscan.engine.document

import io.github.jeiel85.rxscan.core.model.Pt
import io.github.jeiel85.rxscan.core.model.Quad

/**
 * 3x3 projective transform between image planes, stored as a row-major matrix
 * with h22 == 1. Used to perspective-correct the captured document and — via
 * [invert] — to map OCR boxes from the corrected image back onto the displayed
 * original (03_OCR_PIPELINE.md §3: "Preserve a transform matrix so OCR boxes can
 * be mapped back to the displayed original").
 *
 * Pure math (Double precision); no Android dependency, fully unit-testable.
 */
class PerspectiveTransform internal constructor(private val m: DoubleArray) {
    init {
        require(m.size == 9) { "matrix must have 9 elements" }
    }

    fun map(p: Pt): Pt {
        val x = p.x.toDouble()
        val y = p.y.toDouble()
        val denom = m[6] * x + m[7] * y + m[8]
        require(kotlin.math.abs(denom) > 1e-12) { "degenerate projection for $p" }
        val u = (m[0] * x + m[1] * y + m[2]) / denom
        val v = (m[3] * x + m[4] * y + m[5]) / denom
        return Pt(u.toFloat(), v.toFloat())
    }

    /** Inverse transform (corrected -> original). */
    fun invert(): PerspectiveTransform = PerspectiveTransform(invert3x3(m))

    companion object {
        /**
         * Homography mapping the [src] quad corners onto the [dst] quad corners.
         * Solves the 8-unknown DLT system from the four corner correspondences.
         */
        fun fromCorners(src: Quad, dst: Quad): PerspectiveTransform {
            val s = src.corners
            val d = dst.corners
            val a = Array(8) { DoubleArray(8) }
            val b = DoubleArray(8)
            for (i in 0 until 4) {
                val x = s[i].x.toDouble()
                val y = s[i].y.toDouble()
                val u = d[i].x.toDouble()
                val v = d[i].y.toDouble()
                val r = i * 2
                a[r] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -x * u, -y * u)
                b[r] = u
                a[r + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -x * v, -y * v)
                b[r + 1] = v
            }
            val h = solve(a, b)
            return PerspectiveTransform(
                doubleArrayOf(h[0], h[1], h[2], h[3], h[4], h[5], h[6], h[7], 1.0),
            )
        }

        /** Axis-aligned destination rectangle of the given size, origin at (0,0). */
        fun rectangle(width: Float, height: Float): Quad = Quad(
            topLeft = Pt(0f, 0f),
            topRight = Pt(width, 0f),
            bottomRight = Pt(width, height),
            bottomLeft = Pt(0f, height),
        )

        private fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
            val n = b.size
            for (col in 0 until n) {
                var pivot = col
                for (r in col + 1 until n) {
                    if (kotlin.math.abs(a[r][col]) > kotlin.math.abs(a[pivot][col])) pivot = r
                }
                require(kotlin.math.abs(a[pivot][col]) > 1e-12) { "singular correspondence system" }
                val tmpRow = a[col]; a[col] = a[pivot]; a[pivot] = tmpRow
                val tmpB = b[col]; b[col] = b[pivot]; b[pivot] = tmpB
                val diag = a[col][col]
                for (r in 0 until n) {
                    if (r == col) continue
                    val factor = a[r][col] / diag
                    if (factor == 0.0) continue
                    for (c in col until n) a[r][c] -= factor * a[col][c]
                    b[r] -= factor * b[col]
                }
            }
            return DoubleArray(n) { b[it] / a[it][it] }
        }

        private fun invert3x3(m: DoubleArray): DoubleArray {
            val det = m[0] * (m[4] * m[8] - m[5] * m[7]) -
                m[1] * (m[3] * m[8] - m[5] * m[6]) +
                m[2] * (m[3] * m[7] - m[4] * m[6])
            require(kotlin.math.abs(det) > 1e-12) { "non-invertible transform" }
            val inv = DoubleArray(9)
            inv[0] = (m[4] * m[8] - m[5] * m[7]) / det
            inv[1] = (m[2] * m[7] - m[1] * m[8]) / det
            inv[2] = (m[1] * m[5] - m[2] * m[4]) / det
            inv[3] = (m[5] * m[6] - m[3] * m[8]) / det
            inv[4] = (m[0] * m[8] - m[2] * m[6]) / det
            inv[5] = (m[2] * m[3] - m[0] * m[5]) / det
            inv[6] = (m[3] * m[7] - m[4] * m[6]) / det
            inv[7] = (m[1] * m[6] - m[0] * m[7]) / det
            inv[8] = (m[0] * m[4] - m[1] * m[3]) / det
            // Normalize so h22 == 1 to keep the representation canonical.
            val s = inv[8]
            return DoubleArray(9) { inv[it] / s }
        }
    }
}
