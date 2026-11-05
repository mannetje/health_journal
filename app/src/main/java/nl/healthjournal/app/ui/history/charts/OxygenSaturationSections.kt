package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.logging.labelRes
import nl.healthjournal.domain.metrics.model.OxygenSaturationEntry
import nl.healthjournal.domain.metrics.model.OxygenStatistics

/** The oxygen saturation trend in percent. One plain line: no limit lines and no colours that judge a value (ADR 0026). */
@Composable
fun OxygenTrendSection(entries: List<OxygenSaturationEntry>, modifier: Modifier = Modifier) {
    val sorted = remember(entries) { entries.sortedBy { it.timestamp } }
    TrendCard(title = "${stringResource(R.string.trend_oxygen_title)} (%)", modifier = modifier) {
        val series = if (sorted.size >= 2) {
            listOf(
                ChartSeries(
                    sorted.map { ChartPoint(it.timestamp, it.saturation.value.toFloat()) },
                    MaterialTheme.colorScheme.primary,
                    strokeWidthDp = 3f,
                    selection = SeriesSelection(null) { "${it.toInt()}%" }
                )
            )
        } else {
            emptyList()
        }
        LineTrendChart(series = series, emptyMessage = stringResource(R.string.trend_insufficient_data))
    }
}

/**
 * Lowest, highest, average, count and the count per situation, over the readings in the selected range. Figures
 * only. The tick "oxygen given" and the measuring site change nothing here.
 */
@Composable
fun OxygenStatisticsSection(entries: List<OxygenSaturationEntry>, modifier: Modifier = Modifier) {
    val stats = remember(entries) { OxygenStatistics.of(entries) } ?: return
    val lowest = stringResource(R.string.oxygen_lowest)
    val highest = stringResource(R.string.oxygen_highest)
    val average = stringResource(R.string.oxygen_average)
    val count = stringResource(R.string.oxygen_count)
    val description = "$lowest ${stats.lowest}%, $highest ${stats.highest}%, $average ${stats.average}%, $count ${stats.count}"
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TrendCard(
            title = stringResource(R.string.oxygen_figures_title),
            modifier = Modifier.semantics { contentDescription = description }
        ) {
            StatChipRow(
                chips = listOf(
                    StatChip(lowest, "${stats.lowest}%"),
                    StatChip(highest, "${stats.highest}%"),
                    StatChip(average, "${stats.average}%"),
                    StatChip(count, stats.count.toString())
                )
            )
        }
        if (stats.countPerContext.isNotEmpty()) {
            TrendCard(title = stringResource(R.string.oxygen_per_context_title)) {
                stats.countPerContext.entries.sortedByDescending { it.value }.forEach { (context, n) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(context?.labelRes ?: R.string.oxygen_context_none))
                        Text(n.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
