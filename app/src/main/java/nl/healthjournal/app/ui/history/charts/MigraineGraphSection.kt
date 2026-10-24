package nl.healthjournal.app.ui.history.charts

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.migraine.painLabel
import nl.healthjournal.app.ui.migraine.reliefLabel
import nl.healthjournal.app.ui.migraine.symptomLabel
import nl.healthjournal.app.ui.migraine.triggerLabel
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineStatistics
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTrigger
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import nl.healthjournal.domain.model.migraine.TimeOfDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The Migraine view of the History graph section: plain counts of the person's own entries in the
 * selected range, in the order the spec lists them, or the calendar of the same entries. Nothing here
 * ranks, scores or compares.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigraineGraphSection(
    entries: List<MigraineEntry>,
    tags: List<MigraineTag>,
    selectedRange: TrendDateRange,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val locale = LocalConfiguration.current.locales[0]
    val range = remember(entries, selectedRange) { migraineDateRange(entries, selectedRange, zone) }
    val stats = remember(entries, tags, range, locale) { MigraineGraphData.of(entries, tags, range, zone, locale) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val views = listOf(R.string.migraine_view_graph, R.string.migraine_view_calendar)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            views.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = showCalendar == (index == 1),
                    onClick = { showCalendar = index == 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = views.size)
                ) { Text(stringResource(label)) }
            }
        }
        if (showCalendar) {
            MigraineCalendarCard(entries = entries, onOpenDay = onOpenDay)
            return@Column
        }
        if (stats.summary.migraines == 0) {
            TrendCard(title = stringResource(R.string.history_filter_migraine)) {
                Text(stringResource(R.string.migraine_graph_empty))
            }
            return@Column
        }
        SummaryTiles(stats.summary)
        TimelineCard(stats)
        DurationCard(stats)
        IntensityCard(stats)
        WeekdayCard(stats)
        HeatMapCard(stats)
        LabelCountsCard(
            R.string.migraine_graph_symptoms,
            stats.symptoms.codes.map { stringResource(symptomLabel(it.key)) to it.value }
        )
        LabelCountsCard(
            R.string.migraine_graph_triggers,
            stats.triggers.codes.map { stringResource(triggerLabel(it.key)) to it.value } + stats.triggers.personal.toList()
        )
        LabelCountsCard(
            R.string.migraine_graph_pain,
            stats.pain.codes.map { stringResource(painLabel(it.key)) to it.value }
        )
        LabelCountsCard(
            R.string.migraine_graph_relief,
            stats.relief.codes.map { stringResource(reliefLabel(it.key)) to it.value } + stats.relief.personal.toList()
        )
    }
}

/** The range ends today, so migraine-free days after the last entry count; "all" starts at the first entry. */
fun migraineDateRange(entries: List<MigraineEntry>, range: TrendDateRange, zone: ZoneId): ClosedRange<LocalDate> {
    val today = LocalDate.now(zone)
    val start = range.days?.let { today.minusDays(it - 1) }
        ?: entries.minOfOrNull { it.startDay(zone) }
        ?: today
    return start..today
}

/** Every figure of the view, computed once per entries, tags and range. */
private class MigraineGraphData(
    val summary: MigraineStatistics.Summary,
    val timeline: List<MigraineStatistics.TimelinePoint>,
    val durations: MigraineStatistics.DurationBuckets,
    val intensities: MigraineStatistics.IntensityBuckets,
    val weekdays: List<Pair<DayOfWeek, Int>>,
    val grid: MigraineStatistics.TimeOfDayGrid,
    val symptoms: MigraineStatistics.LabelCounts<MigraineSymptom>,
    val triggers: MigraineStatistics.LabelCounts<MigraineTrigger>,
    val pain: MigraineStatistics.LabelCounts<PainCharacter>,
    val relief: MigraineStatistics.LabelCounts<ReliefMethod>,
    val locale: Locale
) {
    companion object {
        fun of(entries: List<MigraineEntry>, tags: List<MigraineTag>, range: ClosedRange<LocalDate>, zone: ZoneId, locale: Locale): MigraineGraphData {
            val firstDay = WeekFields.of(locale).firstDayOfWeek
            return MigraineGraphData(
                summary = MigraineStatistics.summary(entries, range, zone),
                timeline = MigraineStatistics.timeline(entries, range, zone),
                durations = MigraineStatistics.durationBuckets(entries, range, zone),
                intensities = MigraineStatistics.intensityBuckets(entries, range, zone),
                weekdays = MigraineStatistics.weekdayCounts(entries, range, zone, firstDay),
                grid = MigraineStatistics.timeOfDayGrid(entries, range, zone),
                symptoms = MigraineStatistics.symptomCounts(entries, range, zone),
                triggers = MigraineStatistics.triggerCounts(entries, range, zone, tags),
                pain = MigraineStatistics.painCharacterCounts(entries, range, zone),
                relief = MigraineStatistics.reliefCounts(entries, range, zone, tags),
                locale = locale
            )
        }
    }
}

@Composable
private fun SummaryTiles(summary: MigraineStatistics.Summary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChipRow(
            listOf(
                StatChip(stringResource(R.string.migraine_graph_tile_migraines), summary.migraines.toString()),
                StatChip(stringResource(R.string.migraine_graph_tile_days), summary.daysWithMigraine.toString())
            )
        )
        StatChipRow(
            listOf(
                StatChip(stringResource(R.string.migraine_graph_tile_taken), summary.daysWithSomethingTaken.toString()),
                StatChip(stringResource(R.string.migraine_graph_tile_free), summary.migraineFreeDays.toString())
            )
        )
    }
}

@Composable
private fun TimelineCard(stats: MigraineGraphData) {
    val zone = remember { ZoneId.systemDefault() }
    // Short day and month: the bar slots are narrow, a full numeric date would overlap its neighbour.
    val format = remember(stats.locale) { DateTimeFormatter.ofPattern("d MMM", stats.locale).withZone(zone) }
    val bars = stats.timeline.map { Bar(label = format.format(it.start), value = it.intensity?.toDouble()) }
    val withoutIntensity = stats.timeline.count { it.intensity == null }
    TrendCard(
        title = stringResource(R.string.migraine_graph_timeline),
        subtitle = stringResource(R.string.migraine_graph_timeline_hint)
    ) {
        BarTrendChart(bars = bars, valueText = { it.toInt().toString() })
        if (withoutIntensity > 0) Note(stringResource(R.string.migraine_graph_without_intensity, withoutIntensity))
    }
}

@Composable
private fun DurationCard(stats: MigraineGraphData) {
    val d = stats.durations
    TrendCard(title = stringResource(R.string.migraine_graph_duration)) {
        CountBars(
            listOf(
                stringResource(R.string.migraine_graph_under_4h) to d.under4h,
                stringResource(R.string.migraine_graph_4_to_12h) to d.from4To12h,
                stringResource(R.string.migraine_graph_over_12h) to d.over12h
            )
        )
        if (d.ongoing > 0) Note(stringResource(R.string.migraine_graph_still_ongoing, d.ongoing))
    }
}

@Composable
private fun IntensityCard(stats: MigraineGraphData) {
    val i = stats.intensities
    TrendCard(title = stringResource(R.string.migraine_graph_intensity)) {
        CountBars(
            listOf(
                stringResource(R.string.migraine_graph_intensity_low) to i.low0To3,
                stringResource(R.string.migraine_graph_intensity_mid) to i.mid4To6,
                stringResource(R.string.migraine_graph_intensity_high) to i.high7To10
            )
        )
        if (i.missing > 0) Note(stringResource(R.string.migraine_graph_without_intensity, i.missing))
    }
}

@Composable
private fun WeekdayCard(stats: MigraineGraphData) {
    val locale = stats.locale
    TrendCard(title = stringResource(R.string.migraine_graph_weekday)) {
        CountBars(stats.weekdays.map { (day, count) -> day.getDisplayName(TextStyle.FULL, locale) to count })
    }
}

@Composable
private fun HeatMapCard(stats: MigraineGraphData) {
    val locale = stats.locale
    val grid = stats.grid
    val peak = maxOf(1, grid.counts.values.maxOrNull() ?: 0)
    val times = TimeOfDay.entries
    TrendCard(title = stringResource(R.string.migraine_graph_heat_map)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.width(HeatLabelWidth))
            times.forEach { Text(stringResource(timeOfDayLabel(it)), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1) }
        }
        stats.weekdays.forEach { (day, _) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(day.getDisplayName(TextStyle.SHORT, locale), Modifier.width(HeatLabelWidth), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                times.forEach { time ->
                    val count = grid.count(day, time)
                    val description = "${day.getDisplayName(TextStyle.FULL, locale)}, ${stringResource(timeOfDayLabel(time))}, $count"
                    val shade = if (count == 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f + 0.75f * count / peak)
                    Box(
                        modifier = Modifier.weight(1f).height(32.dp).background(shade).clearAndSetSemantics { contentDescription = description },
                        contentAlignment = Alignment.Center
                    ) {
                        if (count > 0) Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
        if (grid.wokeUpWithIt > 0) Note(stringResource(R.string.migraine_graph_woke_up_note, grid.wokeUpWithIt))
    }
}

private val HeatLabelWidth = 40.dp

@StringRes
private fun timeOfDayLabel(t: TimeOfDay) = when (t) {
    TimeOfDay.NIGHT -> R.string.migraine_graph_night
    TimeOfDay.MORNING -> R.string.migraine_graph_morning
    TimeOfDay.AFTERNOON -> R.string.migraine_graph_afternoon
    TimeOfDay.EVENING -> R.string.migraine_graph_evening
}

/** A count card that is left out when nothing in the range was noted. Rows keep list order, never a ranking. */
@Composable
private fun LabelCountsCard(@StringRes title: Int, rows: List<Pair<String, Int>>) {
    if (rows.isEmpty()) return
    TrendCard(title = stringResource(title)) { CountBars(rows) }
}

/** Plain horizontal bars, one row per label, the number at the end. */
@Composable
private fun CountBars(rows: List<Pair<String, Int>>) {
    val peak = maxOf(1, rows.maxOfOrNull { it.second } ?: 0)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { (label, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$label, $count" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label, Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Box(Modifier.weight(1f)) {
                    if (count > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(count.toFloat() / peak)
                                .height(14.dp)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
                Text(count.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
