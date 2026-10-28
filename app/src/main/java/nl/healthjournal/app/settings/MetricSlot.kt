package nl.healthjournal.app.settings

import nl.healthjournal.app.ui.history.HistoryFilter
import nl.healthjournal.app.ui.logging.MetricType

/**
 * A metric the person can place in their own order on the Log tabs and History chips. Migraine has its own
 * logging flow, so it has no [MetricType].
 */
enum class MetricSlot(val metricType: MetricType?, val historyFilter: HistoryFilter) {
    WEIGHT(MetricType.WEIGHT, HistoryFilter.WEIGHT),
    BLOOD_PRESSURE(MetricType.BLOOD_PRESSURE, HistoryFilter.BLOOD_PRESSURE),
    GLUCOSE(MetricType.GLUCOSE, HistoryFilter.GLUCOSE),
    MIGRAINE(null, HistoryFilter.MIGRAINE),
    WAIST_CIRCUMFERENCE(MetricType.WAIST_CIRCUMFERENCE, HistoryFilter.WAIST_CIRCUMFERENCE),
    ACTIVITY(MetricType.ACTIVITY, HistoryFilter.ACTIVITY);

    companion object {
        /** The order when the person has not chosen one. */
        val DEFAULT_ORDER: List<MetricSlot> = entries.toList()

        /**
         * Reads a stored order: unknown and repeated names are dropped (the first one counts) and slots the
         * stored value lacks follow in their default order, so a metric added later needs no migration.
         */
        fun parseOrder(stored: String?): List<MetricSlot> = complete(
            stored.orEmpty().split(',').mapNotNull { name -> entries.firstOrNull { it.name == name.trim() } }
        )

        fun serialiseOrder(order: List<MetricSlot>): String = complete(order).joinToString(",") { it.name }

        /** The order with repeats dropped and every missing slot appended in its default position. */
        fun complete(order: List<MetricSlot>): List<MetricSlot> =
            order.distinct() + DEFAULT_ORDER.filterNot { it in order }
    }
}
