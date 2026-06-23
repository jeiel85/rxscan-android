package io.github.jeiel85.rxscan.engine.imagequality

/**
 * Row-major 8-bit luminance buffer. Decoupled from Android bitmaps so the
 * quality gate is pure Kotlin and unit-testable on the JVM. The device capture
 * layer converts a CameraX frame's Y plane into this type.
 */
class LumaImage(
    val width: Int,
    val height: Int,
    val luma: IntArray,
) {
    init {
        require(width > 0 && height > 0) { "image must be non-empty" }
        require(luma.size == width * height) { "luma length must equal width*height" }
    }

    fun at(x: Int, y: Int): Int = luma[y * width + x]
}
