package nl.healthjournal.app.ui.history.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.nhg.getGlucoseColor
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import kotlin.math.roundToInt

@Composable
fun GlucoseTrendSection(entries: List<GlucoseEntry>, modifier: Modifier = Modifier) {
    val sorted = remember(entries) { entries.sortedBy { it.timestamp } }

    TrendCard(title = stringResource(R.string.trend_glucose_title), modifier = modifier) {
        val points = sorted.map {
            ChartPoint(it.timestamp, it.glucose.valueInMmolL.toFloat())
        }

        LineTrendChart(
            series = if (points.size >= 2) {
                listOf(ChartSeries(points, MaterialTheme.colorScheme.primary, strokeWidthDp = 3f))
            } else {
                emptyList()
            },
            emptyMessage = stringResource(R.string.trend_insufficient_data)
        )

        if (sorted.isNotEmpty()) {
            val latest = sorted.last()
            val average = sorted.map { it.glucose.valueInMmolL.toDouble() }.average()

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
                        "%.1f".format(latest.glucose.valueInMmolL),
                        getGlucoseColor(latest.category)
                    ),
                    StatChip(stringResource(R.string.trend_stat_average), "%.1f".format(average)),
                    StatChip(stringResource(R.string.trend_stat_tir), "$overallTirPercent%"),
                    StatChip(stringResource(R.string.trend_stat_count), sorted.size.toString())
                )
            )
        }
    }
}
