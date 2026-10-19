package nl.healthjournal.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The year row and the month row above the History list. Which chips are selected is decided by the caller
 * (the list position); tapping only asks for a jump. Both rows scroll so the selected chip stays visible.
 */
@Composable
fun TimelineRows(
    timeline: HistoryTimeline,
    selected: YearMonth,
    onMonth: (YearMonth) -> Unit,
    onYear: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val yearState = rememberLazyListState()
    val monthState = rememberLazyListState()
    val yearIndex = timeline.years.indexOf(selected.year).coerceAtLeast(0)
    // The year row scrolls only when the year changes; the month row follows the month.
    LaunchedEffect(selected.year) { yearState.animateScrollToItem(yearIndex) }
    LaunchedEffect(selected) { monthState.animateScrollToItem(selected.monthValue - 1) }
    val withEntries = timeline.monthsWithEntries(selected.year)
    val noEntries = stringResource(R.string.history_nav_no_entries)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(state = yearState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(timeline.years) { year ->
                val isSelected = year == selected.year
                FilterChip(
                    selected = isSelected,
                    onClick = { onYear(year) },
                    label = { Text(year.toString(), maxLines = 1) },
                    leadingIcon = if (isSelected) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null
                )
            }
        }
        LazyRow(state = monthState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Month.entries) { month ->
                val target = YearMonth.of(selected.year, month)
                val hasEntries = target in withEntries
                val isSelected = target == selected
                FilterChip(
                    selected = isSelected,
                    enabled = hasEntries,
                    onClick = { onMonth(target) },
                    label = { Text(month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), maxLines = 1) },
                    leadingIcon = if (isSelected) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                    modifier = if (hasEntries) Modifier else Modifier.semantics { stateDescription = noEntries }
                )
            }
        }
    }
}

/** The header before the first entry of a month: "August 2025" and how many entries it holds. */
@Composable
fun MonthHeader(month: YearMonth, count: Int, modifier: Modifier = Modifier) {
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.history_month_header, name, month.year),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = pluralStringResource(R.plurals.history_month_entries, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
