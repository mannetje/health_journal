package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.common.weightFromKg
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.profile.Profile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.ceil
import kotlin.math.floor

private val WAIST_PERIODS = listOf(ChangePeriod.MONTH, ChangePeriod.THREE_MONTHS, ChangePeriod.SIX_MONTHS, ChangePeriod.YEAR)

/**
 * The waist figures around the chart. Extremes and the change table use all entries; the ratio and the
 * weight scatter use the entries in the selected range. Figures only: no targets, no good or bad colours.
 */
@Composable
fun WaistStatisticsSection(
    waists: List<WaistCircumferenceEntry>,
    allWaists: List<WaistCircumferenceEntry>,
    weights: List<WeightEntry>,
    profile: Profile?,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val stats = remember(allWaists) { WaistStatistics(allWaists.map { WaistPoint(it.timestamp, it.waist.value) }, zone) }
    val newest = stats.newest ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExtremesCard(stats)
        ChangeCard(stats)
        val sex = profile?.sex
        if (sex != null) {
            DistanceCard(newest.cm, WaistLimits.forSex(sex))
        } else {
            NoteCard(stringResource(R.string.waist_distance_title), stringResource(R.string.waist_needs_sex))
        }
        val height = profile?.height?.value
        if (height != null) {
            RatioCard(waists, height)
        } else {
            NoteCard(stringResource(R.string.waist_ratio_title), stringResource(R.string.waist_needs_height))
        }
        WeightScatterCard(waists, weights)
    }
}

@Composable
private fun ExtremesCard(stats: WaistStatistics) {
    val locale = LocalConfiguration.current.locales[0]
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    fun text(cm: Double, at: Instant) = "${formatDecimal(cm, 1)} cm\n${date.format(at.atZone(ZoneId.systemDefault()))}"
    val lowest = stats.lowest ?: return
    val highest = stats.highest ?: return
    TrendCard(title = stringResource(R.string.waist_extremes_title)) {
        StatChipRow(
            chips = listOf(
                StatChip(stringResource(R.string.waist_lowest), text(lowest.cm, lowest.instant)),
                StatChip(stringResource(R.string.waist_highest), text(highest.cm, highest.instant))
            )
        )
    }
}

@Composable
private fun ChangeCard(stats: WaistStatistics) {
    TrendCard(title = stringResource(R.string.waist_change_title)) {
        TableRow(stringResource(R.string.stats_col_period), "cm", "%", header = true)
        HorizontalDivider()
        WAIST_PERIODS.forEach { period ->
            val name = stringResource(periodLabel(period))
            val change = stats.changeOver(period)
            if (change == null) {
                TableRow(name, DASH, DASH)
            } else {
                TableRow(name, signed(change.cm) + " cm", signed(change.percent) + " %")
            }
        }
    }
}

private fun periodLabel(period: ChangePeriod): Int = when (period) {
    ChangePeriod.MONTH -> R.string.stats_period_month
    ChangePeriod.THREE_MONTHS -> R.string.stats_period_3_months
    ChangePeriod.SIX_MONTHS -> R.string.stats_period_6_months
    else -> R.string.stats_period_year
}

@Composable
private fun TableRow(period: String, first: String, second: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(period, style = style, color = color, modifier = Modifier.weight(1.2f))
        Text(first, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(second, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DistanceCard(newestCm: Double, limits: WaistLimits) {
    TrendCard(title = stringResource(R.string.waist_distance_title), subtitle = stringResource(R.string.waist_distance_subtitle)) {
        StatChipRow(
            chips = listOf(
                StatChip(
                    stringResource(R.string.waist_distance_to, formatDecimal(limits.increased, 0)),
                    signed(WaistStatistics.distanceToLimit(newestCm, limits.increased)) + " cm"
                ),
                StatChip(
                    stringResource(R.string.waist_distance_to, formatDecimal(limits.high, 0)),
                    signed(WaistStatistics.distanceToLimit(newestCm, limits.high)) + " cm"
                )
            )
        )
    }
}

@Composable
private fun NoteCard(title: String, note: String) {
    TrendCard(title = title) {
        Text(note, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RatioCard(waists: List<WaistCircumferenceEntry>, heightCm: Int) {
    val sorted = remember(waists) { waists.sortedBy { it.timestamp } }
    val points = sorted.map { ChartPoint(it.timestamp, (it.waist.value / heightCm * 100).toFloat()) }
    TrendCard(
        title = "${stringResource(R.string.waist_ratio_title)} (%)",
        subtitle = stringResource(R.string.waist_ratio_subtitle)
    ) {
        val reference = MaterialTheme.colorScheme.outline
        val series = if (points.size >= 2) {
            listOf(
                ChartSeries(
                    listOf(ChartPoint(points.first().timestamp, 50f), ChartPoint(points.last().timestamp, 50f)),
                    reference,
                    strokeWidthDp = 1.5f
                ),
                ChartSeries(
                    points,
                    MaterialTheme.colorScheme.primary,
                    strokeWidthDp = 3f,
                    selection = SeriesSelection(null) { formatDecimal(it.toDouble(), 1) + " %" }
                )
            )
        } else {
            emptyList()
        }
        LineTrendChart(series = series, emptyMessage = stringResource(R.string.trend_insufficient_data), heightDp = 180)
        points.lastOrNull()?.let {
            StatChipRow(chips = listOf(StatChip(stringResource(R.string.trend_stat_latest), formatDecimal(it.value.toDouble(), 1) + " %")))
        }
    }
}

@Composable
private fun WeightScatterCard(waists: List<WaistCircumferenceEntry>, weights: List<WeightEntry>) {
    val units = LocalDisplayUnits.current
    val pairs = remember(waists, weights) {
        WaistStatistics.pair(
            waists.map { WaistPoint(it.timestamp, it.waist.value) },
            weights.map { WeightPoint(it.timestamp, it.weight.value.toDouble()) }
        )
    }
    val fit = remember(pairs) { WaistStatistics.fit(pairs) }
    TrendCard(
        title = stringResource(R.string.waist_scatter_title),
        subtitle = stringResource(R.string.waist_scatter_subtitle, WaistStatistics.PAIR_WINDOW_DAYS.toInt())
    ) {
        if (pairs.isEmpty()) {
            Text(stringResource(R.string.waist_scatter_needs_more, WaistStatistics.MIN_PAIRS), style = MaterialTheme.typography.bodyMedium)
            return@TrendCard
        }
        val shown = pairs.map { units.weightFromKg(it.kg) to it.cm }
        val xMin = floor(shown.minOf { it.first }) - 1
        val xMax = ceil(shown.maxOf { it.first }) + 1
        val yMin = floor(shown.minOf { it.second }) - 1
        val yMax = ceil(shown.maxOf { it.second }) + 1
        val measurer = rememberTextMeasurer()
        val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        val dotColor = MaterialTheme.colorScheme.primary
        val lineColor = MaterialTheme.colorScheme.onSurface
        val description = stringResource(R.string.waist_scatter_content_description, pairs.size)
        Canvas(modifier = Modifier.fillMaxWidth().height(240.dp).semantics { contentDescription = description }) {
            val left = 40.dp.toPx()
            val bottom = 24.dp.toPx()
            val plot = Size(size.width - left, size.height - bottom)
            fun x(w: Double) = left + ((w - xMin) / (xMax - xMin)).toFloat() * plot.width
            fun y(c: Double) = plot.height - ((c - yMin) / (yMax - yMin)).toFloat() * plot.height

            drawRect(gridColor, Offset(left, 0f), plot, style = Stroke(width = 1.dp.toPx()))
            listOf(xMin, (xMin + xMax) / 2, xMax).forEach { v ->
                val label = measurer.measure(formatDecimal(v, 0), labelStyle)
                drawText(label, topLeft = Offset(x(v) - label.size.width / 2f, plot.height + 4.dp.toPx()))
            }
            listOf(yMin, (yMin + yMax) / 2, yMax).forEach { v ->
                val label = measurer.measure(formatDecimal(v, 0), labelStyle)
                drawText(label, topLeft = Offset(left - label.size.width - 4.dp.toPx(), y(v) - label.size.height / 2f))
            }
            if (fit != null) {
                fun cmAt(w: Double) = fit.intercept + fit.slope * (w / units.weightFromKg(1.0))
                drawLine(lineColor, Offset(x(xMin), y(cmAt(xMin))), Offset(x(xMax), y(cmAt(xMax))), strokeWidth = 2.dp.toPx())
            }
            shown.forEach { (w, c) -> drawCircle(dotColor, 5.dp.toPx(), Offset(x(w), y(c))) }
        }
        if (fit != null) {
            val perUnit = fit.slope / units.weightFromKg(1.0)
            Text(
                stringResource(R.string.waist_scatter_slope, signed(perUnit), units.weightSymbol),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(stringResource(R.string.waist_scatter_needs_more, WaistStatistics.MIN_PAIRS), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private const val DASH = "—"
