package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.nhg.getBpColor
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import kotlin.math.ceil

/**
 * The blood pressure figures below the trend chart: the readings as dots on the NHG bands, how steady they
 * are, and the change against the previous period. Figures only: no target, no good or bad colour.
 */
@Composable
fun BloodPressureStatisticsSection(entries: List<BloodPressureEntry>, range: TrendDateRange, modifier: Modifier = Modifier) {
    val stats = remember(entries, range) {
        BloodPressureStatistics(
            entries.map { BpPoint(it.timestamp, it.reading.systolic, it.reading.diastolic, it.reading.pulse) },
            range
        )
    }
    if (stats.current.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScatterCard(stats.current)
        SteadinessCard(stats)
        CompareCard(stats, range)
    }
}

private const val LEFT_PAD_DP = 36f
private const val BOTTOM_PAD_DP = 22f
private const val TICK_STEP = 20

@Composable
private fun ScatterCard(points: List<BpPoint>) {
    val axes = remember(points) { ScatterAxes.of(points) }
    val normal = getBpColor(NhgBloodPressureCategory.NORMAL)
    val high = getBpColor(NhgBloodPressureCategory.HIGH)
    val serious = getBpColor(NhgBloodPressureCategory.SERIOUSLY_RAISED)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.onSurface
    val ringColor = MaterialTheme.colorScheme.surface
    val newest = points.last()
    val description = stringResource(R.string.bp_scatter_content_description, points.size, newest.systolic, newest.diastolic)

    TrendCard(title = stringResource(R.string.bp_scatter_title), subtitle = stringResource(R.string.bp_scatter_hint)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .semantics { contentDescription = description }
        ) {
            val left = LEFT_PAD_DP.dp.toPx()
            val bottom = BOTTOM_PAD_DP.dp.toPx()
            val plot = Size(size.width - left, size.height - bottom)
            fun x(dia: Number) = left + (dia.toFloat() - axes.diastolicMin) / (axes.diastolicMax - axes.diastolicMin) * plot.width
            fun y(sys: Number) = plot.height - (sys.toFloat() - axes.systolicMin) / (axes.systolicMax - axes.systolicMin) * plot.height

            // Bands, from the outer one inwards: a reading is in the innermost band it fits.
            drawRect(serious.copy(alpha = ZONE_ALPHA), Offset(left, 0f), plot)
            drawRect(
                high.copy(alpha = ZONE_ALPHA),
                Offset(left, y(BpZones.SERIOUS_SYSTOLIC)),
                Size(x(BpZones.SERIOUS_DIASTOLIC) - left, plot.height - y(BpZones.SERIOUS_SYSTOLIC))
            )
            drawRect(
                normal.copy(alpha = ZONE_ALPHA),
                Offset(left, y(BpZones.HIGH_SYSTOLIC)),
                Size(x(BpZones.HIGH_DIASTOLIC) - left, plot.height - y(BpZones.HIGH_SYSTOLIC))
            )

            var tick = ceil(axes.diastolicMin / TICK_STEP.toDouble()).toInt() * TICK_STEP
            while (tick <= axes.diastolicMax) {
                drawLine(gridColor, Offset(x(tick), 0f), Offset(x(tick), plot.height), strokeWidth = 1.dp.toPx())
                val label = measurer.measure(tick.toString(), labelStyle)
                drawText(label, topLeft = Offset(x(tick) - label.size.width / 2f, plot.height + 4.dp.toPx()))
                tick += TICK_STEP
            }
            tick = ceil(axes.systolicMin / TICK_STEP.toDouble()).toInt() * TICK_STEP
            while (tick <= axes.systolicMax) {
                drawLine(gridColor, Offset(left, y(tick)), Offset(size.width, y(tick)), strokeWidth = 1.dp.toPx())
                val label = measurer.measure(tick.toString(), labelStyle)
                drawText(label, topLeft = Offset(left - label.size.width - 4.dp.toPx(), y(tick) - label.size.height / 2f))
                tick += TICK_STEP
            }
            drawRect(gridColor, Offset(left, 0f), plot, style = Stroke(width = 1.dp.toPx()))

            // Older readings fade so the newest ones stand out.
            val radius = 5.dp.toPx()
            points.forEachIndexed { i, p ->
                val age = if (points.size == 1) 1f else i / (points.size - 1f)
                val center = Offset(x(p.diastolic), y(p.systolic))
                drawCircle(dotColor.copy(alpha = MIN_DOT_ALPHA + (1f - MIN_DOT_ALPHA) * age), radius, center)
                drawCircle(ringColor, radius, center, style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

private const val ZONE_ALPHA = 0.35f
private const val MIN_DOT_ALPHA = 0.35f

@Composable
private fun SteadinessCard(stats: BloodPressureStatistics) {
    val steadiness = stats.steadiness
    TrendCard(title = stringResource(R.string.bp_steady_title)) {
        if (steadiness == null) {
            Text(stringResource(R.string.bp_steady_needs_more), style = MaterialTheme.typography.bodyMedium)
        } else {
            StatChipRow(
                chips = listOf(
                    StatChip(
                        stringResource(R.string.bp_steady_usual),
                        stringResource(R.string.bp_pair_mmhg, formatDecimal(steadiness.usualSystolic, 1), formatDecimal(steadiness.usualDiastolic, 1))
                    ),
                    StatChip(
                        stringResource(R.string.bp_steady_biggest),
                        stringResource(R.string.bp_pair_mmhg, steadiness.biggestSystolic.toString(), steadiness.biggestDiastolic.toString())
                    )
                )
            )
            Text(
                stringResource(R.string.bp_steady_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompareCard(stats: BloodPressureStatistics, range: TrendDateRange) {
    val days = range.days
    val subtitle = if (days == null) {
        stringResource(R.string.bp_compare_all)
    } else {
        stringResource(R.string.bp_compare_subtitle, days.toInt())
    }
    val rows = BpMetric.entries.mapNotNull { metric -> stats.summary(metric)?.let { metric to it } }
    TrendCard(title = stringResource(R.string.bp_compare_title), subtitle = subtitle) {
        rows.forEachIndexed { index, (metric, summary) ->
            if (index > 0) HorizontalDivider()
            MetricRow(metric, summary)
        }
    }
}

@Composable
private fun MetricRow(metric: BpMetric, summary: MetricSummary) {
    val unit = if (metric == BpMetric.PULSE) "bpm" else "mmHg"
    val name = stringResource(
        when (metric) {
            BpMetric.SYSTOLIC -> R.string.trend_bp_systolic
            BpMetric.DIASTOLIC -> R.string.trend_bp_diastolic
            BpMetric.PULSE -> R.string.bp_metric_pulse
            BpMetric.MEAN_ARTERIAL -> R.string.bp_metric_map
            BpMetric.PULSE_PRESSURE -> R.string.bp_metric_pulse_pressure
        }
    )
    val latest = "${formatDecimal(summary.latest, 1)} $unit"
    val spread = "${formatDecimal(summary.min, 0)}–${formatDecimal(summary.max, 0)} $unit"
    val change = when {
        summary.change == null -> DASH
        summary.isStable -> stringResource(R.string.bp_compare_stable)
        else -> "${signed(summary.change)} $unit"
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(latest, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                spread,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                change,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

private const val DASH = "—"
