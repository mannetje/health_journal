package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.common.weightFromKg
import nl.healthjournal.domain.model.metrics.WeightEntry
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

/**
 * The weight figures around the chart: lowest and highest weight, the change per period and the change per
 * month and per year. They use all weight entries and ignore the range selector. Figures only, no targets.
 */
@Composable
fun WeightStatisticsSection(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    val zone = ZoneId.systemDefault()
    val stats = remember(entries) {
        WeightStatistics(entries.map { WeightPoint(it.timestamp, it.weight.value.toDouble()) }, zone)
    }
    if (stats.lowest == null) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExtremesCard(stats)
        ChangeTableCard(stats)
        MonthlyChangeCard(stats)
        YearlyChangeCard(stats)
    }
}

@Composable
private fun ExtremesCard(stats: WeightStatistics) {
    val units = LocalDisplayUnits.current
    val locale = LocalConfiguration.current.locales[0]
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    fun text(kg: Double, at: Instant) =
        "${formatDecimal(units.weightFromKg(kg), 1)} ${units.weightSymbol}\n${date.format(at.atZone(ZoneId.systemDefault()))}"
    val lowest = stats.lowest ?: return
    val highest = stats.highest ?: return
    TrendCard(title = stringResource(R.string.stats_extremes_title)) {
        StatChipRow(
            chips = listOf(
                StatChip(stringResource(R.string.stats_lowest), text(lowest.kg, lowest.instant)),
                StatChip(stringResource(R.string.stats_highest), text(highest.kg, highest.instant))
            )
        )
    }
}

@Composable
private fun ChangeTableCard(stats: WeightStatistics) {
    val units = LocalDisplayUnits.current
    var percent by rememberSaveable { mutableStateOf(false) }
    val rows = ChangePeriod.entries.map { it to stats.changeOver(it) }
    TrendCard(title = stringResource(R.string.stats_change_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !percent, onClick = { percent = false }, label = { Text(units.weightSymbol) })
            FilterChip(selected = percent, onClick = { percent = true }, label = { Text("%") })
        }
        val perWeek = stringResource(R.string.stats_per_week, if (percent) "%" else units.weightSymbol)
        TableRow(stringResource(R.string.stats_col_period), stringResource(R.string.stats_col_change), perWeek, header = true)
        HorizontalDivider()
        rows.forEach { (period, change) ->
            val name = stringResource(periodName(period))
            if (change == null) {
                TableRow(name, DASH, DASH)
            } else if (percent) {
                TableRow(name, signed(change.percent) + " %", signed(change.percentPerWeek) + " %")
            } else {
                val unit = units.weightSymbol
                TableRow(
                    name,
                    signed(units.weightFromKg(change.kg)) + " $unit",
                    signed(units.weightFromKg(change.kgPerWeek)) + " $unit"
                )
            }
        }
    }
}

@Composable
private fun TableRow(period: String, change: String, perWeek: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(period, style = style, color = color, modifier = Modifier.weight(1.2f))
        Text(change, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(perWeek, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MonthlyChangeCard(stats: WeightStatistics) {
    val units = LocalDisplayUnits.current
    val locale = LocalConfiguration.current.locales[0]
    val years = stats.years
    var chosen by rememberSaveable { mutableIntStateOf(0) }
    val year = if (chosen in years) chosen else years.last()
    val changes = remember(stats, year) { stats.monthlyChange(year) }
    TrendCard(title = "${stringResource(R.string.stats_monthly_title)} (${units.weightSymbol})") {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            years.forEach { y ->
                FilterChip(selected = y == year, onClick = { chosen = y }, label = { Text(y.toString(), maxLines = 1) })
            }
        }
        if (changes.isEmpty()) {
            Text(stringResource(R.string.trend_insufficient_data), style = MaterialTheme.typography.bodyMedium)
        } else {
            BarTrendChart(
                bars = Month.entries.map { Bar(it.getDisplayName(TextStyle.SHORT, locale), changes[it]?.let(units::weightFromKg)) },
                valueText = ::signed
            )
        }
    }
}

@Composable
private fun YearlyChangeCard(stats: WeightStatistics) {
    val units = LocalDisplayUnits.current
    val changes = remember(stats) { stats.yearlyChange() }
    TrendCard(title = "${stringResource(R.string.stats_yearly_title)} (${units.weightSymbol})") {
        if (changes.isEmpty()) {
            Text(stringResource(R.string.trend_insufficient_data), style = MaterialTheme.typography.bodyMedium)
        } else {
            BarTrendChart(
                bars = changes.map { (y, kg) -> Bar(y.toString(), units.weightFromKg(kg)) },
                valueText = ::signed
            )
        }
    }
}

private const val DASH = "—"

private fun periodName(period: ChangePeriod): Int = when (period) {
    ChangePeriod.WEEK -> R.string.stats_period_week
    ChangePeriod.FIFTEEN_DAYS -> R.string.stats_period_15_days
    ChangePeriod.MONTH -> R.string.stats_period_month
    ChangePeriod.THREE_MONTHS -> R.string.stats_period_3_months
    ChangePeriod.SIX_MONTHS -> R.string.stats_period_6_months
    ChangePeriod.YEAR -> R.string.stats_period_year
}

/** One decimal with an explicit plus for a gain; a value that rounds to zero carries no sign. */
internal fun signed(value: Double): String {
    val text = formatDecimal(value, 1)
    return when {
        text.trimStart('-').all { it == '0' || it == '.' || it == ',' } -> text.trimStart('-')
        value > 0 -> "+$text"
        else -> text
    }
}
