package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineStatistics
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * A month grid of the person's migraines: a day with one or more is marked in the chart colour (with the
 * count from two up) and a day with something taken carries a small second dot. Tapping a marked day calls
 * [onOpenDay]. The month cannot move past the current one.
 */
@Composable
fun MigraineCalendarCard(
    entries: List<MigraineEntry>,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val locale = LocalConfiguration.current.locales[0]
    val currentMonth = remember { YearMonth.now(zone) }
    var shownText by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    val shown = YearMonth.parse(shownText)

    val monthRange = shown.atDay(1)..shown.atEndOfMonth()
    val counts = remember(entries, shown) { MigraineStatistics.dayCounts(entries, monthRange, zone) }
    val taken = remember(entries, shown) { MigraineStatistics.daysWithSomethingTaken(entries, monthRange, zone) }
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    TrendCard(title = stringResource(R.string.migraine_calendar_title), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { shownText = shown.minusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.migraine_calendar_previous))
            }
            Text(
                text = shown.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { shownText = shown.plusMonths(1).toString() }, enabled = shown < currentMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.migraine_calendar_next))
            }
        }
        WeekdayHeader(firstDayOfWeek, locale)
        val leadingBlanks = (shown.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..shown.lengthOfMonth()).map { shown.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day -> DayCell(day, counts, taken, locale, onOpenDay, Modifier.weight(1f)) }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
        Text(
            stringResource(R.string.migraine_calendar_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek, locale: java.util.Locale) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (0L..6L).map { firstDayOfWeek.plus(it) }.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.NARROW, locale),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate?,
    counts: Map<LocalDate, Int>,
    taken: Set<LocalDate>,
    locale: java.util.Locale,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier
) {
    if (day == null) {
        Box(modifier.height(CellHeight))
        return
    }
    val count = counts[day] ?: 0
    val marked = count > 0
    val description = if (marked) {
        val base = "${day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))}, " +
            pluralStringResource(R.plurals.migraine_calendar_count, count, count)
        if (day in taken) "$base, ${stringResource(R.string.migraine_calendar_taken)}" else base
    } else {
        day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .height(CellHeight)
            .clip(MaterialTheme.shapes.small)
            .background(if (marked) colors.secondary else colors.surface)
            .then(if (marked) Modifier.clickable { onOpenDay(day) } else Modifier)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (marked) colors.onSecondary else colors.onSurface
        )
        if (count >= 2) Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSecondary)
        if (day in taken) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(colors.onSecondary))
        }
    }
}

private val CellHeight = 40.dp
