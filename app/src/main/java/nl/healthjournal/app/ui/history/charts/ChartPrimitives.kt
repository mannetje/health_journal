package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

data class ChartPoint(val timestamp: Instant, val value: Float)

data class ChartSeries(val points: List<ChartPoint>, val color: Color, val strokeWidthDp: Float = 3f)

private val axisLabelFormatter = DateTimeFormatter.ofPattern("d MMM").withZone(ZoneId.systemDefault())
private val xToInstantKey = ExtraStore.Key<Map<Float, Instant>>()

/**
 * A [Vico](https://github.com/patrykandpatrick/vico)-backed line chart drawing one or more
 * [series] against a shared time x-axis and value y-axis. Pan and pinch-to-zoom over the visible
 * date range are handled natively by [CartesianChartHost] (see ADR 0003 for why a charting
 * library is used here instead of hand-rolled `Canvas` drawing).
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
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // x-values are seconds relative to the earliest point, not raw epoch seconds: a Float's
    // ~24-bit mantissa can't distinguish adjacent raw epoch-second values (~1.7e9), which would
    // collide nearby timestamps onto the same x position.
    val referenceEpochSecond = remember(series) { allPoints.minOf { it.timestamp.epochSecond } }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(series, referenceEpochSecond) {
        val xToInstant = allPoints.associate { point ->
            (point.timestamp.epochSecond - referenceEpochSecond).toFloat() to point.timestamp
        }
        modelProducer.runTransaction {
            lineModel {
                series.forEach { s ->
                    series(
                        x = s.points.map { (it.timestamp.epochSecond - referenceEpochSecond).toFloat() },
                        y = s.points.map { it.value }
                    )
                }
            }
            extras { extraStore -> extraStore[xToInstantKey] = xToInstant }
        }
    }

    val bottomAxisFormatter = remember(referenceEpochSecond) {
        CartesianValueFormatter { context, x, _ ->
            val instant = context.model.extraStore.getOrNull(xToInstantKey)?.get(x.toFloat())
                ?: Instant.ofEpochSecond(referenceEpochSecond + x.toLong())
            axisLabelFormatter.format(instant)
        }
    }

    val lineProvider = LineCartesianLayer.LineProvider.series(
        series.map { s ->
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(s.color)),
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = s.strokeWidthDp.dp)
            )
        }
    )

    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = lineProvider),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomAxisFormatter)
            ),
            modelProducer = modelProducer,
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp.dp),
            scrollState = rememberVicoScrollState(),
            zoomState = rememberVicoZoomState()
        )
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
