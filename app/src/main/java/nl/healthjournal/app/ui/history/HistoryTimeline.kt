package nl.healthjournal.app.ui.history

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** One item of the History list below the optional chart: a month header or one entry row. */
sealed interface TimelineItem {
    data class Header(val month: YearMonth, val count: Int) : TimelineItem
    data class Entry(val row: Int) : TimelineItem
}

/**
 * The year and month structure of the shown History entries. The selected period is never stored: it is
 * derived from the list position ([monthAt]), and a jump is a list position ([indexOf]), so scrolling and
 * the chips cannot disagree.
 *
 * [instants] are the shown entries, newest first. [leading] is the number of list items before the first
 * header (the trend chart). Grouping uses [zone], the same zone the row text is formatted in.
 */
class HistoryTimeline(instants: List<Instant>, zone: ZoneId, private val leading: Int = 0) {

    val items: List<TimelineItem>

    /** Months that have entries, newest first. */
    val months: List<YearMonth>

    /** Years that have entries, oldest first (like the chips). */
    val years: List<Int>

    private val headerIndex: Map<YearMonth, Int>

    init {
        val counts = linkedMapOf<YearMonth, Int>()
        val monthOfRow = instants.map { YearMonth.from(it.atZone(zone)) }
        monthOfRow.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        val built = ArrayList<TimelineItem>(instants.size + counts.size)
        val headers = HashMap<YearMonth, Int>()
        var previous: YearMonth? = null
        monthOfRow.forEachIndexed { row, month ->
            if (month != previous) {
                headers[month] = leading + built.size
                built.add(TimelineItem.Header(month, counts.getValue(month)))
                previous = month
            }
            built.add(TimelineItem.Entry(row))
        }
        items = built
        headerIndex = headers
        months = counts.keys.sortedDescending()
        years = months.map { it.year }.distinct().sorted()
    }

    /** There is something to navigate when the entries span at least two months. */
    val navigable: Boolean get() = months.size >= 2

    /** The month of the item at [listIndex]; the newest month while the chart or the first header is shown. */
    fun monthAt(listIndex: Int): YearMonth? {
        if (months.isEmpty()) return null
        val index = listIndex - leading
        if (index <= 0) return months.first()
        val item = items.getOrNull(index) ?: return months.last()
        return when (item) {
            is TimelineItem.Header -> item.month
            is TimelineItem.Entry -> monthOfRow(index)
        }
    }

    /** List index of the header of [month] (the newest entry of that month follows it), or null without entries. */
    fun indexOf(month: YearMonth): Int? = headerIndex[month]

    /** List index of the newest month of [year] that has entries. */
    fun indexOfYear(year: Int): Int? = months.firstOrNull { it.year == year }?.let { headerIndex[it] }

    /** The months of [year] that have entries. */
    fun monthsWithEntries(year: Int): Set<YearMonth> = months.filter { it.year == year }.toSet()

    private fun monthOfRow(itemIndex: Int): YearMonth {
        var i = itemIndex
        while (i > 0) {
            val item = items[i]
            if (item is TimelineItem.Header) return item.month
            i--
        }
        return (items[0] as TimelineItem.Header).month
    }
}
