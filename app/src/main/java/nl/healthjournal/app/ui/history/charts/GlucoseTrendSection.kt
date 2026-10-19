package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.common.glucoseDigits
import nl.healthjournal.app.ui.common.glucoseFromMmol
import nl.healthjournal.app.ui.nhg.getGlucoseColor
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory

@Composable
fun GlucoseTrendSection(entries: List<GlucoseEntry>, modifier: Modifier = Modifier) {
    val sorted = remember(entries) { entries.sortedBy { it.timestamp } }

    val units = LocalDisplayUnits.current

    TrendCard(title = "${stringResource(R.string.trend_glucose_title)} (${units.glucoseSymbol})", modifier = modifier) {
        val points = sorted.map {
            ChartPoint(it.timestamp, units.glucoseFromMmol(it.glucose.valueInMmolL.toDouble()).toFloat())
        }
        val fastingColor = MaterialTheme.colorScheme.primary
        val postColor = MaterialTheme.colorScheme.tertiary
        val lowColor = getGlucoseColor(NhgGlucoseCategory.HYPOGLYCAEMIA)
        val fastingLimitColor = getGlucoseColor(NhgGlucoseCategory.IMPAIRED_FASTING)
        val postLimitColor = getGlucoseColor(NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE)
        val categoryColors = sorted.map { getGlucoseColor(it.category) }
        val series = if (points.size >= 2) {
            buildList {
                val first = points.first().timestamp
                val last = points.last().timestamp
                listOf(
                    GLUCOSE_LOW_LIMIT to lowColor,
                    GLUCOSE_FASTING_LIMIT to fastingLimitColor,
                    GLUCOSE_POSTPRANDIAL_LIMIT to postLimitColor
                ).forEach { (mmol, color) ->
                    val shown = units.glucoseFromMmol(mmol).toFloat()
                    add(ChartSeries(listOf(ChartPoint(first, shown), ChartPoint(last, shown)), color, strokeWidthDp = 1.5f))
                }
                listOf(GlucoseContext.FASTING to fastingColor, GlucoseContext.POSTPRANDIAL to postColor).forEach { (context, color) ->
                    val indices = sorted.indices.filter { sorted[it].context == context }
                    if (indices.isNotEmpty()) {
                        add(ChartSeries(indices.map { points[it] }, color, strokeWidthDp = 3f, pointColors = indices.map { categoryColors[it] }))
                    }
                }
            }
        } else {
            emptyList()
        }

        LineTrendChart(series = series, emptyMessage = stringResource(R.string.trend_insufficient_data))

        if (points.size >= 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(fastingColor, GlucoseContext.FASTING.label())
                LegendDot(postColor, GlucoseContext.POSTPRANDIAL.label())
            }
            Text(
                stringResource(
                    R.string.glucose_limits_legend,
                    formatDecimal(units.glucoseFromMmol(GLUCOSE_LOW_LIMIT), units.glucoseDigits),
                    formatDecimal(units.glucoseFromMmol(GLUCOSE_FASTING_LIMIT), units.glucoseDigits),
                    formatDecimal(units.glucoseFromMmol(GLUCOSE_POSTPRANDIAL_LIMIT), units.glucoseDigits),
                    units.glucoseSymbol
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sorted.isNotEmpty()) {
            val latest = sorted.last()
            val average = sorted.map { units.glucoseFromMmol(it.glucose.valueInMmolL.toDouble()) }.average()

            listOf(GlucoseContext.FASTING, GlucoseContext.POSTPRANDIAL).forEach { context ->
                val forContext = sorted.filter { it.context == context }
                if (forContext.isNotEmpty()) {
                    val normalCount = forContext.count { it.category == NhgGlucoseCategory.NORMAL }
                    val outOfRangeCount = forContext.size - normalCount
                    CategoryDistributionBar(
                        segments = listOf(
                            DistributionSegment(
                                label = stringResource(R.string.trend_glucose_tir_normal, context.label()),
                                fraction = normalCount.toFloat() / forContext.size,
                                count = normalCount,
                                color = getGlucoseColor(NhgGlucoseCategory.NORMAL)
                            ),
                            DistributionSegment(
                                label = stringResource(R.string.trend_glucose_tir_out_of_range, context.label()),
                                fraction = outOfRangeCount.toFloat() / forContext.size,
                                count = outOfRangeCount,
                                color = getGlucoseColor(NhgGlucoseCategory.IMPAIRED_FASTING)
                            )
                        )
                    )
                }
            }

            val overallTirPercent = (sorted.count { it.category == NhgGlucoseCategory.NORMAL }.toFloat() / sorted.size * 100).roundToInt()

            StatChipRow(
                chips = listOf(
                    StatChip(
                        stringResource(R.string.trend_stat_latest),
                        formatDecimal(units.glucoseFromMmol(latest.glucose.valueInMmolL.toDouble()), units.glucoseDigits),
                        getGlucoseColor(latest.category)
                    ),
                    StatChip(stringResource(R.string.trend_stat_average), formatDecimal(average, units.glucoseDigits)),
                    StatChip(stringResource(R.string.trend_stat_tir), "$overallTirPercent%"),
                    StatChip(stringResource(R.string.trend_stat_count), sorted.size.toString())
                )
            )
        }
    }
}

/** NHG reference values in mmol/L: low limit, fasting normal maximum, post-meal normal limit. */
private const val GLUCOSE_LOW_LIMIT = 3.9
private const val GLUCOSE_FASTING_LIMIT = 6.0
private const val GLUCOSE_POSTPRANDIAL_LIMIT = 7.8

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
