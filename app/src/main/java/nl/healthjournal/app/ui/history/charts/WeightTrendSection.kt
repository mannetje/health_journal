package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.common.weightFromKg
import nl.healthjournal.app.ui.nhg.getBmiColor
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.nhg.NhgBmiCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrendSection(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    val sorted = remember(entries) { entries.sortedBy { it.timestamp } }
    var windowSize by remember { mutableIntStateOf(5) }
    val units = LocalDisplayUnits.current

    TrendCard(title = "${stringResource(R.string.trend_weight_title)} (${units.weightSymbol})", modifier = modifier) {
        val rawPoints = sorted.map { ChartPoint(it.timestamp, units.weightFromKg(it.weight.value.toDouble()).toFloat()) }
        val averagePoints = movingAverage(rawPoints, windowSize)
        val averageColor = MaterialTheme.colorScheme.primary

        LineTrendChart(
            series = if (rawPoints.size >= 2) {
                listOf(
                    ChartSeries(rawPoints, MaterialTheme.colorScheme.secondary, strokeWidthDp = 2f),
                    ChartSeries(averagePoints, averageColor, strokeWidthDp = 3f)
                )
            } else {
                emptyList()
            },
            emptyMessage = stringResource(R.string.trend_insufficient_data)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(3, 5, 7, 10).forEach { size ->
                FilterChip(
                    selected = windowSize == size,
                    onClick = { windowSize = size },
                    label = { Text(stringResource(R.string.trend_moving_average_n, size)) }
                )
            }
        }

        if (sorted.isNotEmpty()) {
            val latest = sorted.last()
            val shown = sorted.map { units.weightFromKg(it.weight.value.toDouble()) }
            val min = shown.min()
            val max = shown.max()
            val change = if (sorted.size >= 2) shown.last() - shown.first() else 0.0

            StatChipRow(
                chips = listOf(
                    StatChip(stringResource(R.string.trend_stat_latest), formatWeight(shown.last(), units)),
                    StatChip(stringResource(R.string.trend_stat_change), formatSignedWeight(change, units)),
                    StatChip(stringResource(R.string.trend_stat_min), formatWeight(min, units)),
                    StatChip(stringResource(R.string.trend_stat_max), formatWeight(max, units))
                )
            )

            val latestBmi = latest.bmi
            if (latestBmi != null) {
                val category = NhgBmiCategory.classify(latestBmi)
                Text(
                    text = stringResource(R.string.trend_weight_latest_bmi, formatDecimal(latestBmi.toDouble(), 1), category.label()),
                    color = getBmiColor(category),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatWeight(shown: Double, units: DisplayUnits): String = "${formatDecimal(shown, 1)} ${units.weightSymbol}"
private fun formatSignedWeight(shown: Double, units: DisplayUnits): String {
    val sign = if (shown > 0) "+" else ""
    return "$sign${formatDecimal(shown, 1)} ${units.weightSymbol}"
}
