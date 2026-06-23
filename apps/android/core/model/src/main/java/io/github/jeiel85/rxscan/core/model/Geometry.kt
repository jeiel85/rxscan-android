package io.github.jeiel85.rxscan.core.model

/**
 * Plain 2D point used across the scan engines. Deliberately not [android.graphics.PointF]
 * so quality/geometry logic stays pure Kotlin and unit-testable on the JVM
 * (see 12_PROJECT_STRUCTURE.md: "No Android framework types in parser/matcher
 * domain APIs where avoidable").
 */
data class Pt(val x: Float, val y: Float)

/**
 * A document quadrilateral in image pixel coordinates, corners named by their
 * role after orientation is applied. Used by the boundary overlay and the
 * perspective transform.
 */
data class Quad(
    val topLeft: Pt,
    val topRight: Pt,
    val bottomRight: Pt,
    val bottomLeft: Pt,
) {
    val corners: List<Pt> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)
}
