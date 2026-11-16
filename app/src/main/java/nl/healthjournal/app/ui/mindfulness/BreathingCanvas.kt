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
import androidx.compose.ui.graphics.StrokeCap
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
 * [squareProgress] is 0 to 1 around the square, used only by [BreathingVisual.SQUARE]. [drift] is a slow 0 to 1
 * loop that never stops while the exercise runs, used only by [BreathingVisual.CIRCLE]; it stays 0 when the
 * user has turned animations off.
 */
@Composable
fun BreathingCanvas(
    visual: BreathingVisual,
    fill: Float,
    squareProgress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    drift: Float = 0f,
) {
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        when (visual) {
            BreathingVisual.LOTUS -> drawLotus(fill, color)
            BreathingVisual.CIRCLE -> drawBreathingCircle(fill, color, drift)
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

private const val RIPPLES = 14

/** The fraction of the breath the outermost ripple waits before it follows; inner ripples wait less. */
private const val RIPPLE_LAG = 0.40f

/** How many bright bands travel across the rings at the same time. */
private const val RIPPLE_WAVES = 3f

/**
 * Still water with ripples: a drop in the middle and many thin rings around it. On the in-breath the rings spread
 * outwards one after the other, the outer ones last, like a drop spreading; on the out-breath they draw back in,
 * the outer ones first. Each ring is a crest with a soft glow and a fainter trough just inside it, and the rings
 * fade and thin towards the edge. On top of that a band of light travels outwards over the rings without stopping
 * ([drift], 0 to 1 per loop), which keeps the picture gently alive during the holds as well.
 */
private fun DrawScope.drawBreathingCircle(fill: Float, color: Color, drift: Float) {
    val full = min(size.width, size.height) / 2f
    val t = soft(fill)
    // A faint guide ring marks the fullest breath.
    drawCircle(color.copy(alpha = 0.12f), radius = full, center = center, style = Stroke(width = 1.5f))
    // The water itself: a barely tinted pool that grows with the outermost ripple.
    val pool = full * scaleFor(layerOpening(fill, RIPPLE_LAG))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.14f), color.copy(alpha = 0.02f)),
            center = center,
            radius = pool
        ),
        radius = pool,
        center = center
    )
    // Draw from the outside in, so the brighter inner rings sit on top.
    for (i in RIPPLES - 1 downTo 0) {
        val place = i / (RIPPLES - 1f)
        val radius = full * scaleFor(layerOpening(fill, RIPPLE_LAG * place)) * (0.10f + 0.90f * place)
        // The travelling band: it moves outwards as drift grows, from the drop to the edge.
        val band = 0.5f + 0.5f * sin(2f * PI.toFloat() * (place * RIPPLE_WAVES - drift))
        val strength = (1f - place * 0.8f) * (0.55f + 0.45f * band)
        val width = 2.6f - 1.5f * place
        // A wide, faint stroke gives the ring its soft glow.
        drawCircle(color.copy(alpha = 0.06f * strength), radius = radius, center = center, style = Stroke(width = width * 4f))
        // The trough, just inside the crest.
        drawCircle(color.copy(alpha = 0.08f * strength), radius = radius * 0.965f, center = center, style = Stroke(width = 1f))
        // The crest.
        drawCircle(color.copy(alpha = 0.10f + 0.50f * strength), radius = radius, center = center, style = Stroke(width = width))
    }
    // The drop at the centre glows brighter as the water fills.
    val drop = full * 0.06f
    drawCircle(color.copy(alpha = 0.12f + 0.10f * t), radius = drop * 2.6f, center = center)
    drawCircle(color.copy(alpha = 0.40f + 0.25f * t), radius = drop, center = center)
}

/** One ring of petals: how many, how long (share of the full reach), how wide, and how it turns while breathing. */
private class PetalLayer(
    val count: Int,
    val offsetDegrees: Float,
    val length: Float,
    val width: Float,
    val alpha: Float,
    val turnDegrees: Float,
    /** Share of the breath the layer waits before it moves: outer petals open last and close first. */
    val delay: Float,
)

private val lotusLayers = listOf(
    PetalLayer(count = 8, offsetDegrees = 0f, length = 1.00f, width = 0.38f, alpha = 0.10f, turnDegrees = 5f, delay = 0.30f),
    PetalLayer(count = 8, offsetDegrees = 22.5f, length = 0.80f, width = 0.36f, alpha = 0.16f, turnDegrees = -4f, delay = 0.20f),
    PetalLayer(count = 8, offsetDegrees = 0f, length = 0.60f, width = 0.34f, alpha = 0.22f, turnDegrees = 3f, delay = 0.10f),
    PetalLayer(count = 6, offsetDegrees = 30f, length = 0.38f, width = 0.32f, alpha = 0.30f, turnDegrees = -2f, delay = 0f),
)

private const val STAMENS = 12

/** Where a layer is in its own opening: 0 closed to 1 open, starting after [PetalLayer.delay] of the breath. */
private fun layerOpening(fill: Float, delay: Float): Float =
    soft(((fill.coerceIn(0f, 1f) - delay) / (1f - delay)).coerceIn(0f, 1f))

private fun DrawScope.drawLotus(fill: Float, color: Color) {
    val t = soft(fill)
    val full = min(size.width, size.height) / 2f
    val maxReach = full * scaleFor(t)
    lotusLayers.forEach { layer ->
        // Each ring follows the breath a little later than the one inside it, so the flower unfolds from the
        // centre outwards and folds back from the outside in.
        val reach = full * scaleFor(layerOpening(fill, layer.delay)) * layer.length
        val width = reach * layer.width * 0.8f
        val petal = Path().apply {
            moveTo(0f, 0f)
            cubicTo(width * 1.2f, -reach * 0.22f, width * 0.55f, -reach * 0.98f, 0f, -reach)
            cubicTo(-width * 0.55f, -reach * 0.98f, -width * 1.2f, -reach * 0.22f, 0f, 0f)
            close()
        }
        // The vein runs up the middle of the petal and bends a little, as on a real petal.
        val vein = Path().apply {
            moveTo(0f, -reach * 0.12f)
            quadraticTo(width * 0.18f, -reach * 0.5f, 0f, -reach * 0.82f)
        }
        // Deeper at the base and lighter at the tip.
        val shade = Brush.linearGradient(
            colors = listOf(color.copy(alpha = layer.alpha * 1.8f), color.copy(alpha = layer.alpha * 0.45f)),
            start = Offset.Zero,
            end = Offset(0f, -reach),
        )
        for (i in 0 until layer.count) {
            val degrees = 360f / layer.count * i + layer.offsetDegrees + layer.turnDegrees * t
            rotate(degrees = degrees, pivot = center) {
                translate(left = center.x, top = center.y) {
                    drawPath(petal, shade)
                    drawPath(petal, color.copy(alpha = 0.35f), style = Stroke(width = 1.5f, join = StrokeJoin.Round))
                    drawPath(vein, color.copy(alpha = 0.32f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
                }
            }
        }
    }
    val glow = maxReach * 0.1f
    drawCircle(color.copy(alpha = 0.18f), radius = glow * 2.2f, center = center)
    // Small stamens around the heart; they brighten as the flower opens.
    for (i in 0 until STAMENS) {
        val angle = (2.0 * PI * i / STAMENS).toFloat()
        val ring = glow * (1.5f + 0.6f * t)
        drawCircle(
            color.copy(alpha = 0.2f + 0.4f * t),
            radius = glow * 0.16f,
            center = Offset(center.x + ring * cos(angle), center.y + ring * sin(angle))
        )
    }
    drawCircle(color.copy(alpha = 0.6f), radius = glow, center = center)
}

private const val TRAIL_STEPS = 36
private const val TRAIL_STEP = 0.007f
private const val INNER_SQUARES = 4

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
    // Fainter squares nested inside the outline, like the rings of the circle.
    for (n in 1..INNER_SQUARES) {
        val inset = side * 0.055f * n
        val inner = side - 2f * inset
        val r = (corner - inset).coerceAtLeast(side * 0.04f)
        drawRoundRect(
            color.copy(alpha = 0.16f * (1f - n / (INNER_SQUARES + 1f))),
            topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
            size = Size(inner, inner),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 1.5f, join = StrokeJoin.Round)
        )
    }
    drawRoundRect(
        color.copy(alpha = 0.25f),
        topLeft = topLeft,
        size = Size(side, side),
        cornerRadius = rounding,
        style = Stroke(width = 4f, join = StrokeJoin.Round)
    )
    val p = progress.coerceIn(0f, 1f)
    // A fading comet trail behind the dot, then a soft halo and the dot itself.
    for (i in TRAIL_STEPS downTo 1) {
        val at = p - i * TRAIL_STEP
        if (at < 0f) continue
        val fade = 1f - i / (TRAIL_STEPS + 1f)
        drawCircle(
            color.copy(alpha = 0.34f * fade * fade),
            radius = side * 0.036f * fade,
            center = squarePoint(at, topLeft, side, corner)
        )
    }
    val dot = squarePoint(p, topLeft, side, corner)
    drawCircle(color.copy(alpha = 0.12f), radius = side * 0.11f, center = dot)
    drawCircle(color.copy(alpha = 0.25f), radius = side * 0.075f, center = dot)
    drawCircle(color.copy(alpha = 0.85f), radius = side * 0.04f, center = dot)
}
