package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import nl.healthjournal.app.ui.nhg.getBpColor
import nl.healthjournal.app.ui.nhg.label
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.BloodPressureReading
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory

private const val GAUGE_SYSTOLIC_MIN = 80f
private const val GAUGE_SYSTOLIC_MAX = 200f
private const val GAUGE_DIASTOLIC_MIN = 40f
private const val GAUGE_DIASTOLIC_MAX = 120f

@Composable
fun BloodPressureTrendSection(entries: List<BloodPressureEntry>, modifier: Modifier = Modifier) {
    val sorted = remember(entries) { entries.sortedBy { it.timestamp } }

    TrendCard(title = stringResource(R.string.trend_bp_title), modifier = modifier) {
        val systolicPoints = sorted.map { ChartPoint(it.timestamp, it.reading.systolic.toFloat()) }
        val diastolicPoints = sorted.map { ChartPoint(it.timestamp, it.reading.diastolic.toFloat()) }

        LineTrendChart(
            series = if (sorted.size >= 2) {
                listOf(
                    ChartSeries(systolicPoints, MaterialTheme.colorScheme.error, strokeWidthDp = 3f),
                    ChartSeries(diastolicPoints, MaterialTheme.colorScheme.primary, strokeWidthDp = 3f)
                )
            } else {
                emptyList()
            },
            emptyMessage = stringResource(R.string.trend_insufficient_data)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(color = MaterialTheme.colorScheme.error, label = stringResource(R.string.trend_bp_systolic))
            LegendDot(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.trend_bp_diastolic))
        }

        if (sorted.isNotEmpty()) {
            val avgReading = averageBloodPressure(sorted.map { it.reading })
            val avgCategory = NhgBloodPressureCategory.classify(avgReading)
            val avgColor = getBpColor(avgCategory)

            Text(
                text = stringResource(R.string.trend_bp_average, avgReading.systolic, avgReading.diastolic, avgCategory.label()),
                style = MaterialTheme.typography.bodyMedium,
                color = avgColor
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.trend_bp_gauge_systolic), style = MaterialTheme.typography.labelSmall)
                GaugeBar(
                    value = avgReading.systolic.toFloat(),
                    min = GAUGE_SYSTOLIC_MIN,
                    max = GAUGE_SYSTOLIC_MAX,
                    markerColor = avgColor
                )
                Text(stringResource(R.string.trend_bp_gauge_diastolic), style = MaterialTheme.typography.labelSmall)
                GaugeBar(
                    value = avgReading.diastolic.toFloat(),
                    min = GAUGE_DIASTOLIC_MIN,
                    max = GAUGE_DIASTOLIC_MAX,
                    markerColor = avgColor
                )
            }

            val minSystolic = sorted.minOf { it.reading.systolic }
            val maxSystolic = sorted.maxOf { it.reading.systolic }
            val minDiastolic = sorted.minOf { it.reading.diastolic }
            val maxDiastolic = sorted.maxOf { it.reading.diastolic }

            StatChipRow(
                chips = listOf(
                    StatChip(stringResource(R.string.trend_bp_min), "$minSystolic/$minDiastolic"),
                    StatChip(stringResource(R.string.trend_bp_max), "$maxSystolic/$maxDiastolic")
                )
            )

            val total = sorted.size
            val distribution = sorted.groupingBy { it.category }.eachCount()
                .toList()
                .sortedByDescending { it.second }
                .map { (category, count) ->
                    DistributionSegment(
                        label = category.label(),
                        fraction = count.toFloat() / total,
                        count = count,
                        color = getBpColor(category)
                    )
                }
            CategoryDistributionBar(segments = distribution)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** Mean systolic and diastolic, each rounded to the nearest whole mmHg (half up). */
internal fun averageBloodPressure(readings: List<BloodPressureReading>): BloodPressureReading =
    BloodPressureReading(
        readings.map { it.systolic }.average().roundToInt(),
        readings.map { it.diastolic }.average().roundToInt()
    )
