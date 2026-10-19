package nl.healthjournal.app.ui.history.charts

import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/** The change between two selected entries: [delta] later minus earlier, [percent] of the earlier (null when that is 0) and the time [between]. */
data class SelectionDifference(val delta: Float, val percent: Double?, val between: Duration)

/**
 * Which entries of a chart are selected. Pure, so it is tested on the JVM. A selection is a list of timestamps in
 * time order: empty, one entry, or two entries to compare.
 */
object PointSelection {

    /** The entry nearest in time to [target]; on a tie the newest. Null without entries. */
    fun nearest(timestamps: Collection<Instant>, target: Instant): Instant? =
        timestamps.minWithOrNull(compareBy({ abs(Duration.between(it, target).seconds) }, { -it.epochSecond }))

    /**
     * The selection after tapping [tapped]: nothing or two selected starts a new selection, one selected adds
     * the second entry, and tapping the one selected entry again clears the selection.
     */
    fun next(current: List<Instant>, tapped: Instant): List<Instant> = when {
        current.size == 1 && current.first() == tapped -> emptyList()
        current.size == 1 -> listOf(current.first(), tapped).sorted()
        else -> listOf(tapped)
    }

    /** The change from the earlier to the later entry. */
    fun difference(earlier: Float, later: Float, earlierAt: Instant, laterAt: Instant): SelectionDifference {
        val delta = later - earlier
        val percent = if (earlier != 0f) delta.toDouble() / earlier * 100 else null
        return SelectionDifference(delta, percent, Duration.between(earlierAt, laterAt))
    }
}
