package nl.healthjournal.app.ui.history.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.nhg.getWaistColor
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.model.nhg.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.model.profile.Profile

/** The waist trend in cm. With a sex in the profile the NHG limits are drawn as lines and the points take their band colour. */
@Composable
fun WaistTrendSection(entries: List<WaistCircumferenceEntry>, profile: Profile?, modifier: Modifier = Modifier) {
    val sorted = remember(entries) { entries.sortedBy { it.timestamp } }
    val sex = profile?.sex
    val limits = sex?.let { WaistLimits.forSex(it) }

    TrendCard(title = "${stringResource(R.string.trend_waist_title)} (cm)", modifier = modifier) {
        val points = sorted.map { ChartPoint(it.timestamp, it.waist.value.toFloat()) }
        val increasedColor = getWaistColor(NhgWaistCircumferenceCategory.INCREASED_RISK)
        val highColor = getWaistColor(NhgWaistCircumferenceCategory.HIGH_RISK)
        val bandColors = sex?.let { s -> sorted.map { getWaistColor(NhgWaistCircumferenceCategory.classify(it.waist, s)) } }
        val series = if (points.size >= 2) {
            buildList {
                if (limits != null) {
                    val first = points.first().timestamp
                    val last = points.last().timestamp
                    fun line(cm: Double, color: androidx.compose.ui.graphics.Color) = ChartSeries(
                        listOf(ChartPoint(first, cm.toFloat()), ChartPoint(last, cm.toFloat())),
                        color,
                        strokeWidthDp = 1.5f
                    )
                    add(line(limits.increased, increasedColor))
                    add(line(limits.high, highColor))
                }
                add(
                    ChartSeries(
                        points,
                        MaterialTheme.colorScheme.primary,
                        strokeWidthDp = 3f,
                        pointColors = bandColors
                    )
                )
            }
        } else {
            emptyList()
        }
        LineTrendChart(series = series, emptyMessage = stringResource(R.string.trend_insufficient_data))

        if (limits != null) {
            Text(
                stringResource(
                    R.string.waist_limits_legend,
                    formatDecimal(limits.increased, 0),
                    formatDecimal(limits.high, 0)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                stringResource(R.string.waist_needs_sex),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
