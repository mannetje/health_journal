package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.min

/** How the breathing exercise is drawn; the user picks one on the setup screen. */
enum class BreathingVisual { LOTUS, CIRCLE, SQUARE }

private const val MIN_SCALE = 0.35f
private const val PETALS = 8

/**
 * Draws one frame of the breathing visual. [fill] is 0 (breathed out) to 1 (breathed in);
 * [squareProgress] is 0 to 1 around the square, used only by [BreathingVisual.SQUARE].
 */
@Composable
fun BreathingCanvas(
    visual: BreathingVisual,
    fill: Float,
    squareProgress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        when (visual) {
            BreathingVisual.LOTUS -> drawLotus(fill, color)
            BreathingVisual.CIRCLE -> drawBreathingCircle(fill, color)
            BreathingVisual.SQUARE -> drawSquare(squareProgress, color)
        }
    }
}

private fun scaleFor(fill: Float) = MIN_SCALE + (1f - MIN_SCALE) * fill.coerceIn(0f, 1f)

private fun DrawScope.drawBreathingCircle(fill: Float, color: Color) {
    val radius = min(size.width, size.height) / 2f
    drawCircle(color.copy(alpha = 0.15f), radius = radius, center = center, style = Stroke(width = 2f))
    drawCircle(color.copy(alpha = 0.55f), radius = radius * scaleFor(fill), center = center)
}

private fun DrawScope.drawLotus(fill: Float, color: Color) {
    val reach = min(size.width, size.height) / 2f * scaleFor(fill)
    val width = reach * 0.42f
    val petal = Path().apply {
        moveTo(0f, 0f)
        quadraticTo(width, -reach * 0.5f, 0f, -reach)
        quadraticTo(-width, -reach * 0.5f, 0f, 0f)
        close()
    }
    for (i in 0 until PETALS) {
        rotate(degrees = 360f / PETALS * i, pivot = center) {
            translate(left = center.x, top = center.y) { drawPath(petal, color.copy(alpha = 0.35f)) }
        }
    }
}

private fun DrawScope.drawSquare(progress: Float, color: Color) {
    val side = min(size.width, size.height) * 0.8f
    val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
    drawRect(color.copy(alpha = 0.3f), topLeft = topLeft, size = Size(side, side), style = Stroke(width = 6f))
    val p = (progress.coerceIn(0f, 1f) * 4f)
    val edge = p.toInt().coerceAtMost(3)
    val t = p - edge
    val dot = when (edge) {
        0 -> Offset(topLeft.x + side * t, topLeft.y)
        1 -> Offset(topLeft.x + side, topLeft.y + side * t)
        2 -> Offset(topLeft.x + side * (1 - t), topLeft.y + side)
        else -> Offset(topLeft.x, topLeft.y + side * (1 - t))
    }
    drawCircle(color, radius = side * 0.05f, center = dot)
}
