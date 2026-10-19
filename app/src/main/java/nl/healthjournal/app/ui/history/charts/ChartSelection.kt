package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.formatDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/** How the entries of a series are shown when selected: an optional [name] and the value text ([format] includes the unit). */
class SeriesSelection(val name: String?, val format: (Float) -> String)

/** The values of the selected entries and, with two selected, the difference between them. Figures only. */
@Composable
internal fun SelectionSummary(selected: List<Instant>, series: List<ChartSeries>, modifier: Modifier = Modifier) {
    if (selected.isEmpty()) return
    val selectable = series.filter { it.selection != null }
    val locale = LocalConfiguration.current.locales[0]
    val dateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())

    fun valueOf(s: ChartSeries, at: Instant): Float? = s.points.firstOrNull { it.timestamp == at }?.value
    fun labelled(s: ChartSeries, text: String): String = s.selection?.name?.let { "$it: $text" } ?: text

    Column(modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        selected.forEach { at ->
            val values = selectable.mapNotNull { s -> valueOf(s, at)?.let { labelled(s, s.selection!!.format(it)) } }
            Text(
                "${dateTime.format(at)} · ${values.joinToString(" · ")}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        val (earlier, later) = selected.let { it.first() to it.last() }
        val comparable = selected.size == 2 && selectable.any { valueOf(it, earlier) != null && valueOf(it, later) != null }
        if (comparable) {
            Text(
                stringResource(R.string.chart_selection_difference, durationText(Duration.between(earlier, later))),
                style = MaterialTheme.typography.labelLarge
            )
            selectable.forEach { s ->
                val from = valueOf(s, earlier)
                val to = valueOf(s, later)
                if (from != null && to != null) {
                    val difference = PointSelection.difference(from, to, earlier, later)
                    Text(
                        labelled(s, differenceText(s.selection!!, difference)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun differenceText(selection: SeriesSelection, difference: SelectionDifference): String {
    val sign = when {
        difference.delta > 0f -> "+"
        difference.delta < 0f -> "-"
        else -> ""
    }
    val amount = sign + selection.format(abs(difference.delta))
    val percent = difference.percent?.let { " ($sign${formatDecimal(abs(it), 1)} %)" }.orEmpty()
    return amount + percent
}

@Composable
private fun durationText(between: Duration): String {
    val resources = LocalContext.current.resources
    val days = between.toDays().toInt()
    return if (days >= 1) {
        resources.getQuantityString(R.plurals.chart_selection_days, days, days)
    } else {
        val hours = between.toHours().toInt()
        resources.getQuantityString(R.plurals.chart_selection_hours, hours, hours)
    }
}
