package nl.healthjournal.domain.model.mindfulness

enum class BellKind { START, INTERVAL, END }

/** A bell at a moment, in milliseconds since the start. */
data class Bell(val atMillis: Long, val kind: BellKind)

/**
 * A silent-sitting timer: a start bell, optional interval bells and an end bell. It has no clock; the caller
 * passes the elapsed milliseconds and fires the bells that have come due.
 */
class MeditationTimer(val durationMinutes: Int, val intervalMinutes: Int? = null) {
    init {
        require(durationMinutes in MIN_MINUTES..MAX_MINUTES) {
            "Duration must be $MIN_MINUTES to $MAX_MINUTES minutes, got: $durationMinutes"
        }
        require(intervalMinutes == null || intervalMinutes in 1 until durationMinutes) {
            "Interval must be at least 1 minute and shorter than the duration, got: $intervalMinutes"
        }
    }

    val durationMillis: Long = durationMinutes * 60_000L

    /** Start at 0, an interval bell every [intervalMinutes] before the end, and the end bell. Sorted by time. */
    val bells: List<Bell> = buildList {
        add(Bell(0, BellKind.START))
        if (intervalMinutes != null) {
            var minute = intervalMinutes
            while (minute < durationMinutes) {
                add(Bell(minute * 60_000L, BellKind.INTERVAL))
                minute += intervalMinutes
            }
        }
        add(Bell(durationMillis, BellKind.END))
    }

    fun remainingMillis(elapsedMillis: Long): Long =
        (durationMillis - elapsedMillis.coerceAtLeast(0)).coerceAtLeast(0)

    fun isFinished(elapsedMillis: Long): Boolean = elapsedMillis >= durationMillis

    /**
     * The bells from [nextIndex] on whose time has come. The caller keeps the index of the next bell to fire,
     * so every bell sounds once even when the ticker runs late.
     */
    fun bellsDue(nextIndex: Int, elapsedMillis: Long): List<Bell> =
        bells.drop(nextIndex).takeWhile { it.atMillis <= elapsedMillis }

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 120

        val PRESET_MINUTES = listOf(5, 10, 15, 20, 30)
        val INTERVAL_PRESET_MINUTES = listOf(1, 2, 5, 10)
    }
}
