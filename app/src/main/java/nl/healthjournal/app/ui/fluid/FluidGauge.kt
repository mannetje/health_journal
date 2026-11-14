package nl.healthjournal.app.ui.fluid

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.FluidGaugeStyle

/**
 * The gauge that fills with today's total: a glass, or a woman or man outline. The scale is the person's own
 * maximum, else their own minimum, with a tick for each number set. With neither it fills in whole glasses and shows
 * no scale, tick or percentage. Animation is skipped when the system asks to reduce motion.
 */
@Composable
fun FluidGauge(
    style: FluidGaugeStyle,
    totalMl: Int,
    minMl: Int?,
    maxMl: Int?,
    glassMl: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    val scale = maxMl ?: minMl
    val target = if (scale != null) (totalMl.toFloat() / scale).coerceIn(0f, 1f)
    else (totalMl / glassMl.coerceAtLeast(1)).coerceAtMost(GLASSES).toFloat() / GLASSES
    val level by animateFloatAsState(target, if (reduceMotion) snap() else tween(600), label = "gauge")
    val outline = MaterialTheme.colorScheme.outline
    val water = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(
        if (style == FluidGaugeStyle.GLASS) R.string.fluid_glass_description else R.string.fluid_body_description,
        totalMl
    )
    val body = remember(style) { if (style == FluidGaugeStyle.GLASS) null else bodyPath(style == FluidGaugeStyle.WOMAN) }

    Canvas(modifier.semantics { contentDescription = description }) {
        if (body == null) {
            val glass = glassPath(size)
            clipPath(glass) {
                val top = size.height * (1f - level)
                drawRect(water.copy(alpha = 0.55f), topLeft = Offset(0f, top), size = Size(size.width, size.height - top))
            }
            drawPath(glass, outline, style = Stroke(width = 3.dp.toPx()))
            // A tick for each number the person set, on the same scale as the fill.
            if (scale != null) {
                listOfNotNull(minMl, maxMl).forEach { n ->
                    val y = size.height * (1f - (n.toFloat() / scale).coerceIn(0f, 1f))
                    drawLine(tick, Offset(size.width * 0.1f, y), Offset(size.width * 0.9f, y), strokeWidth = 2.dp.toPx())
                }
            }
        } else {
            // The outline is drawn in a 100 by 200 box, scaled evenly and centred.
            val unit = minOf(size.width / BODY_W, size.height / BODY_H)
            withTransform({
                translate((size.width - BODY_W * unit) / 2f, (size.height - BODY_H * unit) / 2f)
                scale(unit, unit, Offset.Zero)
            }) {
                clipPath(body) {
                    val top = BODY_H * (1f - level)
                    drawRect(water.copy(alpha = 0.55f), topLeft = Offset(0f, top), size = Size(BODY_W, BODY_H - top))
                }
                // Ticks stay inside the figure so they never float over empty space.
                if (scale != null) {
                    clipPath(body) {
                        listOfNotNull(minMl, maxMl).forEach { n ->
                            val y = BODY_H * (1f - (n.toFloat() / scale).coerceIn(0f, 1f))
                            drawLine(tick, Offset(0f, y), Offset(BODY_W, y), strokeWidth = 1.5.dp.toPx() / unit)
                        }
                    }
                }
                drawPath(body, outline, style = Stroke(width = 3.dp.toPx() / unit, join = StrokeJoin.Round))
            }
        }
    }
}

internal fun DrawScope.glassPath(size: Size): Path = Path().apply {
    val inset = size.width * 0.18f
    moveTo(inset * 0.4f, 0f)
    lineTo(size.width - inset * 0.4f, 0f)
    lineTo(size.width - inset, size.height)
    lineTo(inset, size.height)
    close()
}

private const val GLASSES = 8
private const val BODY_W = 100f
private const val BODY_H = 200f

// The left half of each outline from the neck down to the crotch; the right half is its mirror.
private val MAN_HALF = listOf(
    40f to 32f, 16f to 40f, 6f to 98f, 13f to 104f, 22f to 98f, 27f to 62f, 29f to 108f, 29f to 190f, 46f to 190f, 49f to 122f
)
private val WOMAN_HALF = listOf(
    41f to 32f, 22f to 40f, 13f to 96f, 19f to 102f, 26f to 96f, 31f to 62f, 35f to 86f, 27f to 118f, 31f to 190f, 47f to 190f, 50f to 128f
)

/** A plain standing outline: a head and one body shape, the woman narrower at the shoulders and wider at the hips. */
private fun bodyPath(woman: Boolean): Path = Path().apply {
    val half = if (woman) WOMAN_HALF else MAN_HALF
    addOval(Rect(Offset(36f, 2f), Size(28f, 28f)))
    moveTo(half.first().first, half.first().second)
    half.drop(1).forEach { (x, y) -> lineTo(x, y) }
    half.reversed().forEach { (x, y) -> lineTo(BODY_W - x, y) }
    close()
}
