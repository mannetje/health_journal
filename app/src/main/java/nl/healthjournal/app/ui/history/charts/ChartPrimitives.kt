package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

data class ChartPoint(val timestamp: Instant, val value: Float, val color: Color? = null)

data class ChartSeries(val points: List<ChartPoint>, val defaultColor: Color, val strokeWidthDp: Float = 3f)

private val axisLabelFormatter = DateTimeFormatter.ofPattern("d MMM").withZone(ZoneId.systemDefault())

/**
 * A Canvas-based line chart drawing one or more [series] against a shared time x-axis and
 * value y-axis. Supports horizontal pan and pinch-to-zoom over the visible date range via
 * [Modifier.pointerInput] transform gestures (no third-party charting/gesture library, per ADR 0003).
 */
@Composable
fun LineTrendChart(
    series: List<ChartSeries>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    heightDp: Int = 220
) {
    val allPoints = series.flatMap { it.points }
    if (allPoints.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var panFraction by remember { mutableFloatStateOf(0f) }

    val minTime = allPoints.minOf { it.timestamp.epochSecond }
    val maxTime = allPoints.maxOf { it.timestamp.epochSecond }
    val minValue = allPoints.minOf { it.value }
    val maxValue = allPoints.maxOf { it.value }
    val valuePadding = ((maxValue - minValue) * 0.1f).let { if (it <= 0f) 1f else it }
    val yMin = minValue - valuePadding
    val yMax = maxValue + valuePadding

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 8f)
                    val panDeltaFraction = -pan.x / size.width.coerceAtLeast(1)
                    panFraction = (panFraction + panDeltaFraction / scale).coerceIn(0f, 1f - 1f / scale)
                }
            }
    ) {
        val leftPadding = 44.dp.toPx()
        val bottomPadding = 24.dp.toPx()
        val chartWidth = size.width - leftPadding
        val chartHeight = size.height - bottomPadding

        val totalRange = (maxTime - minTime).coerceAtLeast(1)
        val visibleRange = (totalRange / scale).toLong().coerceAtLeast(1)
        val visibleStart = minTime + (totalRange * panFraction).toLong()
        val visibleEnd = min(maxTime, visibleStart + visibleRange)

        fun xFor(epochSecond: Long): Float {
            val clamped = epochSecond.coerceIn(visibleStart, visibleEnd)
            val fraction = (clamped - visibleStart).toFloat() / (visibleEnd - visibleStart).coerceAtLeast(1)
            return leftPadding + fraction * chartWidth
        }

        fun yFor(value: Float): Float {
            val fraction = (value - yMin) / (yMax - yMin).coerceAtLeast(0.0001f)
            return chartHeight - fraction * chartHeight
        }

        // Horizontal gridlines + y-axis labels (4 bands)
        val bands = 4
        for (i in 0..bands) {
            val value = yMin + (yMax - yMin) * i / bands
            val y = yFor(value)
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            val label = textMeasurer.measure(
                text = "%.0f".format(value),
                style = androidx.compose.ui.text.TextStyle(color = axisTextColor, fontSize = 10.sp)
            )
            drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
        }

        // X-axis date labels at start/end of visible range
        val startLabel = textMeasurer.measure(
            text = axisLabelFormatter.format(Instant.ofEpochSecond(visibleStart)),
            style = androidx.compose.ui.text.TextStyle(color = axisTextColor, fontSize = 10.sp)
        )
        val endLabel = textMeasurer.measure(
            text = axisLabelFormatter.format(Instant.ofEpochSecond(visibleEnd)),
            style = androidx.compose.ui.text.TextStyle(color = axisTextColor, fontSize = 10.sp)
        )
        drawText(startLabel, topLeft = Offset(leftPadding, chartHeight + 4.dp.toPx()))
        drawText(endLabel, topLeft = Offset(size.width - endLabel.size.width, chartHeight + 4.dp.toPx()))

        // Series lines (per-segment colored when a point declares its own color)
        series.forEach { s ->
            val strokeWidth = s.strokeWidthDp.dp.toPx()
            for (i in 0 until s.points.size - 1) {
                val from = s.points[i]
                val to = s.points[i + 1]
                val segmentColor = from.color ?: to.color ?: s.defaultColor
                drawLine(
                    color = segmentColor,
                    start = Offset(xFor(from.timestamp.epochSecond), yFor(from.value)),
                    end = Offset(xFor(to.timestamp.epochSecond), yFor(to.value)),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            s.points.forEach { point ->
                drawCircle(
                    color = point.color ?: s.defaultColor,
                    radius = strokeWidth,
                    center = Offset(xFor(point.timestamp.epochSecond), yFor(point.value))
                )
            }
        }
    }
}

fun movingAverage(points: List<ChartPoint>, window: Int): List<ChartPoint> {
    if (window <= 1 || points.isEmpty()) return points
    val result = mutableListOf<ChartPoint>()
    for (i in points.indices) {
        val start = max(0, i - window + 1)
        val slice = points.subList(start, i + 1)
        val average = slice.sumOf { it.value.toDouble() } / slice.size
        result += ChartPoint(points[i].timestamp, average.toFloat())
    }
    return result
}
