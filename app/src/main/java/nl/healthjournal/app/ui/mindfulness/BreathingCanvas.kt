package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** How the breathing exercise is drawn; the user picks one on the setup screen. */
enum class BreathingVisual { LOTUS, CIRCLE, SQUARE }

private const val MIN_SCALE = 0.35f

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

/** Eases a 0..1 value so shapes open and close softly instead of at a constant speed. */
private fun soft(fill: Float): Float {
    val x = fill.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun DrawScope.drawBreathingCircle(fill: Float, color: Color) {
    val full = min(size.width, size.height) / 2f
    val radius = full * scaleFor(soft(fill))
    // A faint guide ring marks the fullest breath.
    drawCircle(color.copy(alpha = 0.12f), radius = full, center = center, style = Stroke(width = 1.5f))
    // Soft layers around the core, from a wide halo to a bright centre.
    listOf(1.0f to 0.07f, 0.84f to 0.10f, 0.68f to 0.14f, 0.52f to 0.20f).forEach { (share, alpha) ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha * 0.6f), color.copy(alpha = alpha)),
                center = center,
                radius = radius * share
            ),
            radius = radius * share,
            center = center
        )
    }
    drawCircle(color.copy(alpha = 0.3f), radius = radius, center = center, style = Stroke(width = 1.5f))
    drawCircle(color.copy(alpha = 0.45f), radius = radius * 0.12f, center = center)
}

/** One ring of petals: how many, how long (share of the full reach), how wide, and how it turns while breathing. */
private class PetalLayer(
    val count: Int,
    val offsetDegrees: Float,
    val length: Float,
    val width: Float,
    val alpha: Float,
    val turnDegrees: Float,
)

private val lotusLayers = listOf(
    PetalLayer(count = 8, offsetDegrees = 0f, length = 1.00f, width = 0.38f, alpha = 0.10f, turnDegrees = 5f),
    PetalLayer(count = 8, offsetDegrees = 22.5f, length = 0.80f, width = 0.36f, alpha = 0.16f, turnDegrees = -4f),
    PetalLayer(count = 8, offsetDegrees = 0f, length = 0.60f, width = 0.34f, alpha = 0.22f, turnDegrees = 3f),
    PetalLayer(count = 6, offsetDegrees = 30f, length = 0.38f, width = 0.32f, alpha = 0.30f, turnDegrees = -2f),
)

private fun DrawScope.drawLotus(fill: Float, color: Color) {
    val t = soft(fill)
    val maxReach = min(size.width, size.height) / 2f * scaleFor(t)
    lotusLayers.forEach { layer ->
        val reach = maxReach * layer.length
        val width = reach * layer.width * 0.8f
        val petal = Path().apply {
            moveTo(0f, 0f)
            cubicTo(width * 1.2f, -reach * 0.22f, width * 0.55f, -reach * 0.98f, 0f, -reach)
            cubicTo(-width * 0.55f, -reach * 0.98f, -width * 1.2f, -reach * 0.22f, 0f, 0f)
            close()
        }
        for (i in 0 until layer.count) {
            val degrees = 360f / layer.count * i + layer.offsetDegrees + layer.turnDegrees * t
            rotate(degrees = degrees, pivot = center) {
                translate(left = center.x, top = center.y) {
                    drawPath(petal, color.copy(alpha = layer.alpha))
                    drawPath(petal, color.copy(alpha = 0.35f), style = Stroke(width = 1.5f, join = StrokeJoin.Round))
                }
            }
        }
    }
    val glow = maxReach * 0.1f
    drawCircle(color.copy(alpha = 0.18f), radius = glow * 2.2f, center = center)
    drawCircle(color.copy(alpha = 0.6f), radius = glow, center = center)
}

private const val TRAIL_STEPS = 16
private const val TRAIL_STEP = 0.012f

/**
 * A point on a rounded square, [progress] 0..1 clockwise. The four quarters start and end in the middle of a
 * corner, so each breathing phase covers one side together with the soft turns on either end.
 */
private fun squarePoint(progress: Float, topLeft: Offset, side: Float, corner: Float): Offset {
    val straight = side - 2f * corner
    val arc = (PI / 2.0).toFloat() * corner
    val quarter = straight + arc
    val distance = (((progress % 1f) + 1f) % 1f) * 4f * quarter
    val k = (distance / quarter).toInt().coerceAtMost(3)
    val d = distance - k * quarter
    val rad = (PI / 180.0).toFloat()
    fun centreOf(index: Int) = when (index % 4) {
        0 -> Offset(topLeft.x + corner, topLeft.y + corner)
        1 -> Offset(topLeft.x + side - corner, topLeft.y + corner)
        2 -> Offset(topLeft.x + side - corner, topLeft.y + side - corner)
        else -> Offset(topLeft.x + corner, topLeft.y + side - corner)
    }
    fun onArc(centre: Offset, degrees: Float) =
        Offset(centre.x + corner * cos(degrees * rad), centre.y + corner * sin(degrees * rad))
    val startDegrees = 225f + 90f * k
    return when {
        d < arc / 2f -> onArc(centreOf(k), startDegrees + d / corner / rad)
        d < arc / 2f + straight -> {
            val from = onArc(centreOf(k), startDegrees + 45f)
            val along = d - arc / 2f
            Offset(from.x + along * cos(90f * k * rad), from.y + along * sin(90f * k * rad))
        }
        else -> onArc(centreOf(k + 1), 270f + 90f * k + (d - arc / 2f - straight) / corner / rad)
    }
}

private fun DrawScope.drawSquare(progress: Float, color: Color) {
    val side = min(size.width, size.height) * 0.8f
    val corner = side * 0.2f
    val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
    val rounding = CornerRadius(corner, corner)
    drawRoundRect(color.copy(alpha = 0.05f), topLeft = topLeft, size = Size(side, side), cornerRadius = rounding)
    drawRoundRect(
        color.copy(alpha = 0.25f),
        topLeft = topLeft,
        size = Size(side, side),
        cornerRadius = rounding,
        style = Stroke(width = 4f, join = StrokeJoin.Round)
    )
    val p = progress.coerceIn(0f, 1f)
    // A fading trail behind the dot, then a soft halo and the dot itself.
    for (i in TRAIL_STEPS downTo 1) {
        val at = p - i * TRAIL_STEP
        if (at < 0f) continue
        val fade = 1f - i / (TRAIL_STEPS + 1f)
        drawCircle(
            color.copy(alpha = 0.28f * fade),
            radius = side * 0.03f * fade,
            center = squarePoint(at, topLeft, side, corner)
        )
    }
    val dot = squarePoint(p, topLeft, side, corner)
    drawCircle(color.copy(alpha = 0.12f), radius = side * 0.11f, center = dot)
    drawCircle(color.copy(alpha = 0.25f), radius = side * 0.075f, center = dot)
    drawCircle(color.copy(alpha = 0.85f), radius = side * 0.04f, center = dot)
}
