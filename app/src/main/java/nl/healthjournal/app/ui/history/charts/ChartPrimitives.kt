package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

private const val FULL_SIZE_POINTS = 30
private const val SMALLEST_SIZE_POINTS = 300
private const val FULL_MARKER_DP = 9f
private const val SMALLEST_MARKER_DP = 3f

/**
 * Marker diameter in dp for a series of [pointCount] points: full size up to 30 points, shrinking linearly to
 * 3 dp at 300 points and more, so dots on a long history never merge into a thick band.
 */
internal fun markerSizeDp(pointCount: Int): Float {
    val t = ((pointCount - FULL_SIZE_POINTS).toFloat() / (SMALLEST_SIZE_POINTS - FULL_SIZE_POINTS)).coerceIn(0f, 1f)
    return FULL_MARKER_DP + (SMALLEST_MARKER_DP - FULL_MARKER_DP) * t
}

data class ChartPoint(val timestamp: Instant, val value: Float)

/**
 * One line. [pointColors], when given, is parallel to [points] and draws a marker per point in that colour
 * (used to tint readings by band); the markers shrink as the series grows, see [markerSizeDp]. A series with a
 * [selection] has entries the user can tap; reference lines and averages have none.
 */
data class ChartSeries(
    val points: List<ChartPoint>,
    val color: Color,
    val strokeWidthDp: Float = 3f,
    val pointColors: List<Color>? = null,
    val selection: SeriesSelection? = null
)

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

    // x-values are whole hours relative to the earliest point. Raw epoch seconds would give Vico
    // a 1-second x step (it derives the step from the GCD of the deltas), which breaks zoom and
    // axis label spacing; hours keep the step coarse while still separating same-day points.
    val referenceEpochSecond = remember(series) { allPoints.minOf { it.timestamp.epochSecond } }
    val modelProducer = remember { CartesianChartModelProducer() }

    // Selection state is keyed on the points, not on the series: a ChartSeries holds lambdas, so it is never
    // equal to a rebuilt copy and would reset the selection on every recomposition.
    val dataKey = series.map { it.points }
    var selected by remember(dataKey) { mutableStateOf(emptyList<Instant>()) }
    val selectable = remember(dataKey) {
        series.filter { it.selection != null }.flatMap { s -> s.points.map { it.timestamp } }.distinct()
    }
    val onTap by rememberUpdatedState { x: Double ->
        val target = Instant.ofEpochSecond(referenceEpochSecond + Math.round(x * SECONDS_PER_HOUR))
        PointSelection.nearest(selectable, target)?.let { selected = PointSelection.next(selected, it) }
    }
    val tapController = remember(dataKey) {
        if (selectable.isEmpty()) null else TapSelectionController { x -> onTap(x) }
    }
    val guidelineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guideline = remember(guidelineColor) { LineComponent(fill = Fill(guidelineColor), thickness = 2.dp) }
    val persistentMarkers = remember(selected, referenceEpochSecond, guideline) {
        if (selected.isEmpty()) {
            null
        } else {
            val marker = GuidelineMarker(guideline)
            val scope: CartesianChart.PersistentMarkerScope.(ExtraStore) -> Unit = {
                selected.forEach { at -> marker at hoursSince(referenceEpochSecond, at).toDouble() }
            }
            scope
        }
    }

    LaunchedEffect(series, referenceEpochSecond) {
        modelProducer.runTransaction {
            lineModel {
                series.forEach { s ->
                    series(
                        x = s.points.map { hoursSince(referenceEpochSecond, it.timestamp) },
                        y = s.points.map { it.value }
                    )
                }
            }
        }
    }

    val tickState = remember(referenceEpochSecond) { AxisTickState() }
    val bottomAxisFormatter = remember(referenceEpochSecond, tickState) {
        val zone = ZoneId.systemDefault()
        CartesianValueFormatter { context, x, _ ->
            val instant = Instant.ofEpochSecond(referenceEpochSecond + Math.round(x * SECONDS_PER_HOUR))
            val step = tickState.step ?: chooseTickStep((context.ranges.maxX - context.ranges.minX) / 24.0)
            formatTick(step, instant, showYear = x in tickState.yearLabelXs, zone = zone)
        }
    }

    val lineProvider = LineCartesianLayer.LineProvider.series(
        series.map { s ->
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(s.color)),
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = s.strokeWidthDp.dp),
                pointProvider = s.pointColors?.let { colors -> tintedPoints(s, colors, referenceEpochSecond) }
            )
        }
    )

    Column {
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = lineProvider, rangeProvider = PaddedYRangeProvider),
                marker = if (tapController != null) HiddenMarker else null,
                persistentMarkers = persistentMarkers,
                markerController = tapController ?: CartesianMarkerController.rememberShowOnPress(),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = CartesianValueFormatter.decimal(decimalCount = 1),
                    itemPlacer = VerticalAxis.ItemPlacer.count(count = { 5 })
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisFormatter,
                    itemPlacer = remember(referenceEpochSecond, tickState) {
                        CalendarItemPlacer(referenceEpochSecond, tickState)
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp.dp),
            scrollState = rememberVicoScrollState(),
            zoomState = rememberVicoZoomState(initialZoom = Zoom.Content)
        )
    }
    SelectionSummary(selected, series)
    }
}

/** Never shown: Vico only reports taps when a marker is set, the visible selection is a persistent [GuidelineMarker]. */
private object HiddenMarker : CartesianMarker

/** A vertical line through a selected entry. */
private class GuidelineMarker(private val line: LineComponent) : CartesianMarker {
    override fun drawOverLayers(context: CartesianDrawingContext, targets: List<CartesianMarker.Target>) {
        val bounds = context.layerBounds
        targets.forEach { line.drawVertical(context, it.canvasX, bounds.top, bounds.bottom) }
    }
}

/** Reports the x-value of every tap and never changes the marker visibility, so [HiddenMarker] stays hidden. */
private class TapSelectionController(private val onTap: (Double) -> Unit) : CartesianMarkerController {
    override val acceptsLongPress = false

    override fun shouldAcceptInteraction(interaction: Interaction, targets: List<CartesianMarker.Target>): Boolean {
        if (interaction is Interaction.Tap && targets.isNotEmpty()) onTap(targets.first().x)
        return false
    }

    override fun shouldShowMarker(interaction: Interaction, targets: List<CartesianMarker.Target>) = false
}

private const val SECONDS_PER_HOUR = 3600L

/** Markers keyed by the x-value ([hoursSince]) of their point, so each reading keeps its own colour. */
private class TintedPointProvider(
    private val points: Map<Double, LineCartesianLayer.Point>,
    private val largest: LineCartesianLayer.Point
) : LineCartesianLayer.PointProvider {
    override fun getPoint(entry: LineCartesianLayerModel.Entry, extraStore: ExtraStore) = points[entry.x]

    override fun getLargestPoint(extraStore: ExtraStore) = largest
}

private fun tintedPoints(series: ChartSeries, colors: List<Color>, referenceEpochSecond: Long): LineCartesianLayer.PointProvider {
    val sizeDp = markerSizeDp(series.points.size)
    fun marker(color: Color) = LineCartesianLayer.Point(
        component = ShapeComponent(
            fill = Fill(color),
            shape = CircleShape,
            strokeFill = Fill(series.color),
            strokeThickness = (sizeDp / 6f).dp
        ),
        size = sizeDp.dp
    )
    val points = series.points.indices.associate { i ->
        hoursSince(referenceEpochSecond, series.points[i].timestamp).toDouble() to marker(colors[i])
    }
    return TintedPointProvider(points, marker(series.color))
}

private fun hoursSince(referenceEpochSecond: Long, instant: Instant): Float =
    ((instant.epochSecond - referenceEpochSecond) / SECONDS_PER_HOUR).toFloat()

/** Fits the y-range to the data (10% padding) instead of Vico's default of always including 0. */
private object PaddedYRangeProvider : CartesianLayerRangeProvider {
    private fun padding(minY: Double, maxY: Double) = ((maxY - minY) * 0.1).takeIf { it > 0 } ?: 1.0

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) =
        minY - padding(minY, maxY)

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore) =
        maxY + padding(minY, maxY)
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
