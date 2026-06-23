package io.github.jeiel85.rxscan.feature.scan

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.jeiel85.rxscan.core.model.Quad

/**
 * Document boundary overlay (08_UX_SPEC.md §3, Screen B). Draws the detected
 * document quadrilateral when available, otherwise a guide frame showing where
 * to place the dispensing bag. No fake progress indicator is shown.
 */
@Composable
fun DocumentBoundaryOverlay(modifier: Modifier = Modifier, boundary: Quad? = null) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val strokePx = 3.dp.toPx()
        if (boundary != null) {
            val path = Path().apply {
                val c = boundary.corners
                moveTo(c[0].x, c[0].y)
                for (i in 1 until c.size) lineTo(c[i].x, c[i].y)
                close()
            }
            drawPath(path, color, style = Stroke(width = strokePx))
        } else {
            val inset = size.minDimension * 0.08f
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = strokePx),
            )
        }
    }
}
