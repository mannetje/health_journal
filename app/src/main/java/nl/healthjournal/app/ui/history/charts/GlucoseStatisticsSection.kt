package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.common.glucoseDigits
import nl.healthjournal.app.ui.common.glucoseFromMmol
import nl.healthjournal.app.ui.nhg.getGlucoseColor
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private val GLUCOSE_PERIODS = listOf(ChangePeriod.MONTH, ChangePeriod.THREE_MONTHS, ChangePeriod.SIX_MONTHS, ChangePeriod.YEAR)
private val CONTEXTS = listOf(GlucoseContext.FASTING, GlucoseContext.POSTPRANDIAL)
private const val DASH = "—"

private fun GlucoseEntry.toPoint() = GlucosePoint(timestamp, glucose.valueInMmolL.toDouble(), context, category)

/** A glucose value in the display unit, without the unit symbol. */
private fun DisplayUnits.shown(mmol: Double): String = formatDecimal(glucoseFromMmol(mmol), glucoseDigits)

/** A glucose difference in the display unit with an explicit plus; a value that rounds to zero carries no sign. */
private fun DisplayUnits.shownSigned(deltaMmol: Double): String {
    val text = shown(deltaMmol)
    return when {
        text.trimStart('-').all { it == '0' || it == '.' || it == ',' } -> text.trimStart('-')
        deltaMmol > 0 -> "+$text"
        else -> text
    }
}

/**
 * The glucose figures around the chart. Time in range and time of day use the readings in the selected range;
 * extremes, change, spread, the 30-day comparison and the largest rise use all readings. Figures only: no
 * targets, no good or bad colours.
 */
@Composable
fun GlucoseStatisticsSection(glucoses: List<GlucoseEntry>, allGlucoses: List<GlucoseEntry>, modifier: Modifier = Modifier) {
    val zone = ZoneId.systemDefault()
    val inRange = remember(glucoses) { glucoses.map { it.toPoint() } }
    val stats = remember(allGlucoses) { GlucoseStatistics(allGlucoses.map { it.toPoint() }, zone) }
    if (stats.newest == null) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeInRangeCard(inRange)
        TimeOfDayCard(inRange, zone)
        ExtremesCard(stats, zone)
        ChangeCard(stats)
        SpreadCard(stats)
        CompareCard(stats)
        RiseCard(stats, zone)
    }
}

@Composable
private fun TimeInRangeCard(points: List<GlucosePoint>) {
    TrendCard(title = stringResource(R.string.glucose_tir_title), subtitle = stringResource(R.string.glucose_tir_subtitle)) {
        if (points.isEmpty()) {
            Text(stringResource(R.string.glucose_no_data), style = MaterialTheme.typography.bodyMedium)
        }
        CONTEXTS.forEach { context ->
            val counts = GlucoseStatistics.categoryCounts(points, context)
            val total = counts.sumOf { it.second }
            if (total > 0) {
                Text(context.label(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                CategoryDistributionBar(
                    segments = counts.map { (category, count) ->
                        DistributionSegment(category.label(context), count.toFloat() / total, count, getGlucoseColor(category))
                    }
                )
            }
        }
    }
}

@Composable
private fun TimeOfDayCard(points: List<GlucosePoint>, zone: ZoneId) {
    val units = LocalDisplayUnits.current
    val parts = remember(points, zone) { GlucoseStatistics.byTimeOfDay(points, zone) }
    TrendCard(
        title = "${stringResource(R.string.glucose_tod_title)} (${units.glucoseSymbol})",
        subtitle = stringResource(R.string.glucose_tod_subtitle)
    ) {
        parts.forEach { part ->
            val name = stringResource(timeOfDayName(part.timeOfDay))
            if (part.average == null || part.lowest == null || part.highest == null) {
                GlucoseRow(name, DASH, DASH, "0")
            } else {
                GlucoseRow(name, units.shown(part.average), "${units.shown(part.lowest)} – ${units.shown(part.highest)}", part.count.toString())
            }
        }
    }
}

private fun timeOfDayName(part: TimeOfDay): Int = when (part) {
    TimeOfDay.NIGHT -> R.string.glucose_tod_night
    TimeOfDay.MORNING -> R.string.glucose_tod_morning
    TimeOfDay.AFTERNOON -> R.string.glucose_tod_afternoon
    TimeOfDay.EVENING -> R.string.glucose_tod_evening
}

@Composable
private fun ExtremesCard(stats: GlucoseStatistics, zone: ZoneId) {
    val units = LocalDisplayUnits.current
    val lowest = stats.lowest ?: return
    val highest = stats.highest ?: return
    TrendCard(title = stringResource(R.string.glucose_extremes_title)) {
        StatChipRow(
            chips = listOf(
                StatChip(stringResource(R.string.glucose_lowest), readingText(units, lowest, zone)),
                StatChip(stringResource(R.string.glucose_highest), readingText(units, highest, zone))
            )
        )
    }
}

@Composable
private fun readingText(units: DisplayUnits, point: GlucosePoint, zone: ZoneId): String =
    "${units.shown(point.mmol)} ${units.glucoseSymbol}\n${dateText(point.instant, zone)}"

@Composable
private fun dateText(instant: Instant, zone: ZoneId): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(instant.atZone(zone))
}

@Composable
private fun ChangeCard(stats: GlucoseStatistics) {
    val units = LocalDisplayUnits.current
    TrendCard(
        title = stringResource(R.string.glucose_change_title),
        subtitle = stringResource(R.string.glucose_change_subtitle, units.glucoseSymbol)
    ) {
        GlucoseRow(stringResource(R.string.stats_col_period), GlucoseContext.FASTING.label(), GlucoseContext.POSTPRANDIAL.label(), header = true)
        HorizontalDivider()
        GLUCOSE_PERIODS.forEach { period ->
            val name = stringResource(periodName(period))
            val fasting = stats.changeOver(GlucoseContext.FASTING, period)?.let { units.shownSigned(it.delta) } ?: DASH
            val post = stats.changeOver(GlucoseContext.POSTPRANDIAL, period)?.let { units.shownSigned(it.delta) } ?: DASH
            GlucoseRow(name, fasting, post)
        }
    }
}

private fun periodName(period: ChangePeriod): Int = when (period) {
    ChangePeriod.MONTH -> R.string.stats_period_month
    ChangePeriod.THREE_MONTHS -> R.string.stats_period_3_months
    ChangePeriod.SIX_MONTHS -> R.string.stats_period_6_months
    else -> R.string.stats_period_year
}

@Composable
private fun SpreadCard(stats: GlucoseStatistics) {
    val units = LocalDisplayUnits.current
    TrendCard(
        title = stringResource(R.string.glucose_spread_title),
        subtitle = stringResource(R.string.glucose_spread_subtitle, units.glucoseSymbol)
    ) {
        GlucoseRow(
            "",
            stringResource(R.string.glucose_col_average),
            stringResource(R.string.glucose_col_sd),
            stringResource(R.string.glucose_col_range),
            header = true
        )
        HorizontalDivider()
        CONTEXTS.forEach { context ->
            val spread = stats.spread(context)
            if (spread == null) {
                GlucoseRow(context.label(), DASH, DASH, DASH)
            } else {
                GlucoseRow(
                    context.label(),
                    units.shown(spread.average),
                    spread.standardDeviation?.let { units.shown(it) } ?: DASH,
                    "${units.shown(spread.lowest)} – ${units.shown(spread.highest)}"
                )
            }
        }
    }
}

@Composable
private fun CompareCard(stats: GlucoseStatistics) {
    val units = LocalDisplayUnits.current
    TrendCard(title = stringResource(R.string.glucose_compare_title)) {
        GlucoseRow(
            "",
            stringResource(R.string.glucose_compare_last),
            stringResource(R.string.glucose_compare_before),
            stringResource(R.string.glucose_compare_change),
            header = true
        )
        HorizontalDivider()
        CONTEXTS.forEach { context ->
            val last = stats.last30Days(context)
            val before = stats.previous30Days(context)
            if (last.count > 0 || before.count > 0) {
                val averageChange = if (last.average != null && before.average != null) units.shownSigned(last.average - before.average) else DASH
                GlucoseRow(
                    stringResource(R.string.glucose_compare_average, context.label()),
                    last.average?.let { units.shown(it) } ?: DASH,
                    before.average?.let { units.shown(it) } ?: DASH,
                    averageChange
                )
                val rangeChange = if (last.normalPercent != null && before.normalPercent != null) {
                    signedPercent(last.normalPercent - before.normalPercent)
                } else {
                    DASH
                }
                GlucoseRow(
                    stringResource(R.string.glucose_compare_in_range, context.label()),
                    last.normalPercent?.let { "${it.roundToInt()} %" } ?: DASH,
                    before.normalPercent?.let { "${it.roundToInt()} %" } ?: DASH,
                    rangeChange
                )
            }
        }
    }
}

/** Percentage points, whole numbers, with an explicit plus. */
private fun signedPercent(points: Double): String {
    val rounded = points.roundToInt()
    return if (rounded > 0) "+$rounded %" else "$rounded %"
}

@Composable
private fun RiseCard(stats: GlucoseStatistics, zone: ZoneId) {
    val units = LocalDisplayUnits.current
    val rise = stats.largestRise()
    TrendCard(
        title = stringResource(R.string.glucose_rise_title),
        subtitle = stringResource(R.string.glucose_rise_subtitle, GlucoseStatistics.RISE_WINDOW_HOURS.toInt())
    ) {
        if (rise == null) {
            Text(
                stringResource(R.string.glucose_rise_none, GlucoseStatistics.RISE_WINDOW_HOURS.toInt()),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            StatChipRow(
                chips = listOf(
                    StatChip(
                        stringResource(R.string.glucose_rise_value, units.shown(rise.from.mmol), units.shown(rise.to.mmol), dateText(rise.to.instant, zone)),
                        "${units.shownSigned(rise.delta)} ${units.glucoseSymbol}"
                    )
                )
            )
        }
    }
}

@Composable
private fun GlucoseRow(first: String, second: String, third: String, fourth: String? = null, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(first, style = style, color = color, modifier = Modifier.weight(1.5f))
        Text(second, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(third, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(if (fourth == null) 1f else 1.4f))
        if (fourth != null) {
            Text(fourth, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}
