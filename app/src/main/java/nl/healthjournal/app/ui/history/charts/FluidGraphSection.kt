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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.fluid.DrinkIcon
import nl.healthjournal.app.ui.fluid.nameRes
import nl.healthjournal.domain.metrics.model.DayTotal
import nl.healthjournal.domain.metrics.model.FluidCalendarDay
import nl.healthjournal.domain.metrics.model.FluidCalendarMonth
import nl.healthjournal.domain.metrics.model.FluidDayTotals
import nl.healthjournal.domain.metrics.model.FluidDrinkTotals
import nl.healthjournal.domain.metrics.model.FluidIntakeEntry
import nl.healthjournal.domain.metrics.model.FluidLimits
import nl.healthjournal.domain.metrics.model.FluidPosition
import nl.healthjournal.domain.metrics.model.FluidStatistics
import nl.healthjournal.domain.metrics.model.FluidTimeOfDay
import nl.healthjournal.domain.metrics.model.FluidWeekdayAverages
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** The neutral position sentence of a day total against the person's own numbers; null without numbers. */
@Composable
fun fluidPositionText(position: FluidPosition, limits: FluidLimits): String? = when (position) {
    FluidPosition.NONE -> null
    FluidPosition.BELOW_MIN -> stringResource(R.string.fluid_position_below, limits.minMl ?: 0)
    FluidPosition.WITHIN -> stringResource(R.string.fluid_position_within, limits.minMl ?: 0, limits.maxMl ?: 0)
    FluidPosition.ABOVE_MAX -> stringResource(R.string.fluid_position_above, limits.maxMl ?: 0)
}

/**
 * The Fluid view of the History graph section: figures, daily totals with the person's own numbers as dashed lines,
 * the average per weekday and per time of day, and the total per drink; or the calendar of the same days. One hue,
 * no colour that judges a day, nothing that ranks or coaches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidGraphSection(
    entries: List<FluidIntakeEntry>,
    allEntries: List<FluidIntakeEntry>,
    limits: FluidLimits,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val locale = LocalConfiguration.current.locales[0]
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val days = remember(entries) { FluidDayTotals.of(entries, zone) }
    val allDays = remember(allEntries) { FluidDayTotals.of(allEntries, zone) }

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
            FluidCalendarCard(days = allDays, limits = limits, onOpenDay = onOpenDay)
            return@Column
        }
        val stats = remember(days, limits) { FluidStatistics.of(days, limits) }
        if (stats == null) {
            TrendCard(title = stringResource(R.string.history_filter_fluid)) { Text(stringResource(R.string.fluid_empty_range)) }
            return@Column
        }
        FiguresCard(stats, limits, locale)
        DailyCard(days, limits, locale)
        val weekdays = remember(days, firstDay) { FluidWeekdayAverages.of(days, firstDay) }
        TrendCard(title = stringResource(R.string.fluid_weekday_title)) {
            BarTrendChart(
                bars = weekdays.map { (d, ml) -> Bar(d.getDisplayName(TextStyle.SHORT, locale), ml.toDouble()) },
                valueText = { it.toInt().toString() }
            )
        }
        val blocks = remember(entries) { FluidTimeOfDay.of(entries, zone) }
        TrendCard(title = stringResource(R.string.fluid_timeofday_title), subtitle = stringResource(R.string.fluid_timeofday_note)) {
            BarTrendChart(
                bars = blocks.map { (b, ml) -> Bar("%02d".format(b * FluidTimeOfDay.BLOCK_HOURS), ml.toDouble()) },
                valueText = { it.toInt().toString() }
            )
        }
        DrinksCard(entries)
    }
}

@Composable
private fun FiguresCard(stats: FluidStatistics, limits: FluidLimits, locale: Locale) {
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
    val chips = listOf(
        StatChip(stringResource(R.string.fluid_average), "${stats.averageMl} ml"),
        StatChip(stringResource(R.string.fluid_highest), "${stats.highest.ml} ml"),
        StatChip(stringResource(R.string.fluid_days_logged), stats.daysLogged.toString())
    )
    val description = chips.joinToString(", ") { "${it.label} ${it.value}" } + ", ${stats.highest.date.format(date)}"
    TrendCard(
        title = stringResource(R.string.fluid_figures_title),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        StatChipRow(chips)
        Text(stats.highest.date.format(date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (limits.isSet) {
            StatChipRow(
                listOfNotNull(
                    limits.minMl?.let { StatChip(stringResource(R.string.fluid_days_below), stats.below.toString()) },
                    StatChip(stringResource(R.string.fluid_days_within), stats.within.toString()),
                    limits.maxMl?.let { StatChip(stringResource(R.string.fluid_days_above), stats.above.toString()) }
                )
            )
        }
    }
}

@Composable
private fun DailyCard(days: List<DayTotal>, limits: FluidLimits, locale: Locale) {
    val byDate = remember(days) { days.associate { it.date to it.ml } }
    val first = days.first().date
    val last = days.last().date
    val format = remember(locale) { DateTimeFormatter.ofPattern("d/M", locale) }
    val bars = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }
        .map { Bar(it.format(format), byDate[it]?.toDouble()) }.toList()
    TrendCard(title = stringResource(R.string.fluid_daily_title)) {
        BarTrendChart(
            bars = bars,
            valueText = { it.toInt().toString() },
            referenceLines = listOfNotNull(limits.minMl?.toDouble(), limits.maxMl?.toDouble())
        )
        limits.minMl?.let { Text(stringResource(R.string.fluid_line_min, it), style = MaterialTheme.typography.bodySmall) }
        limits.maxMl?.let { Text(stringResource(R.string.fluid_line_max, it), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DrinksCard(entries: List<FluidIntakeEntry>) {
    val totals = remember(entries) { FluidDrinkTotals.of(entries) }
    val highest = (totals.maxOfOrNull { it.ml } ?: 1).coerceAtLeast(1)
    TrendCard(title = stringResource(R.string.fluid_drinks_title)) {
        totals.forEach { total ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrinkIcon(total.drink, Modifier.size(24.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(total.drink.nameRes))
                        Text("${total.ml} ml", style = MaterialTheme.typography.titleMedium)
                    }
                    Box(
                        Modifier.fillMaxWidth(total.ml.toFloat() / highest).height(8.dp)
                            .clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        }
    }
}

/**
 * A month grid of the person's daily totals: every day with entries is shaded in one hue by its total against the
 * month's highest day, the total is printed, and a small shape (not a colour) says where it sits against the person's
 * own numbers. Tapping a day with entries calls [onOpenDay]. The month cannot move past the current one.
 */
@Composable
fun FluidCalendarCard(
    days: List<DayTotal>,
    limits: FluidLimits,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val locale = LocalConfiguration.current.locales[0]
    val currentMonth = remember { YearMonth.now(zone) }
    var shownText by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    val shown = YearMonth.parse(shownText)
    val month = remember(days, shown, limits) { FluidCalendarMonth.of(days, shown, limits).associateBy { it.date } }
    val highest = month.values.maxOfOrNull { it.ml } ?: 0
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    TrendCard(title = stringResource(R.string.fluid_calendar_title), modifier = modifier) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0L..6L).map { firstDayOfWeek.plus(it) }.forEach { d: DayOfWeek ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
        val leadingBlanks = (shown.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..shown.lengthOfMonth()).map { shown.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    FluidDayCell(day, day?.let { month[it] }, highest, limits, locale, onOpenDay, Modifier.weight(1f))
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
        Text(
            stringResource(R.string.fluid_calendar_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FluidDayCell(
    day: LocalDate?,
    data: FluidCalendarDay?,
    highest: Int,
    limits: FluidLimits,
    locale: Locale,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier
) {
    if (day == null) {
        Box(modifier.height(56.dp))
        return
    }
    val colors = MaterialTheme.colorScheme
    val dateText = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    if (data == null) {
        Column(
            modifier = modifier.height(56.dp).clip(MaterialTheme.shapes.small).background(colors.surface)
                .clearAndSetSemantics { contentDescription = dateText },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium) }
        return
    }
    val step = FluidCalendarMonth.shadeStep(data.ml, highest)
    val background = colors.primary.copy(alpha = 0.2f + 0.2f * step)
    val onBackground = if (step >= 3) colors.onPrimary else colors.onSurface
    val marker = when (data.position) {
        FluidPosition.BELOW_MIN -> "▾"
        FluidPosition.WITHIN -> "●"
        FluidPosition.ABOVE_MAX -> "▴"
        FluidPosition.NONE -> ""
    }
    val positionText = fluidPositionText(data.position, limits)
    val description = stringResource(R.string.fluid_calendar_day_description, dateText, data.ml) +
        (positionText?.let { ", $it" } ?: "")
    Column(
        modifier = modifier.height(56.dp).clip(MaterialTheme.shapes.small).background(background)
            .clickable(role = Role.Button) { onOpenDay(day) }
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = onBackground)
        Text(data.ml.toString(), style = MaterialTheme.typography.labelMedium, color = onBackground, maxLines = 1, softWrap = false)
        if (marker.isNotEmpty()) Text(marker, style = MaterialTheme.typography.labelSmall, color = onBackground)
    }
}
