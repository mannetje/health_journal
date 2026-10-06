package nl.healthjournal.domain.model.medication

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Which days a recurring schedule is due. */
sealed interface DayPattern {
    /** Due on the selected weekdays. */
    data class Weekdays(val days: Set<DayOfWeek>) : DayPattern {
        init {
            require(days.isNotEmpty()) { "At least one weekday is required" }
        }
    }

    /** Due on the start date and then every [interval] days (7 for a weekly injection, 3 for a patch). */
    data class EveryNDays(val interval: Int) : DayPattern {
        init {
            require(interval in 1..MAX_INTERVAL) { "Interval must be 1 to $MAX_INTERVAL days, got: $interval" }
        }

        companion object {
            const val MAX_INTERVAL = 365
        }
    }

    companion object {
        val EVERY_DAY: DayPattern = Weekdays(DayOfWeek.values().toSet())
    }
}

sealed interface Schedule {
    /** No planned intakes; doses are logged when taken. */
    data object AsNeeded : Schedule

    data class Recurring(
        val times: List<LocalTime>,
        val days: DayPattern,
        val start: LocalDate,
        val end: LocalDate? = null
    ) : Schedule {
        init {
            require(times.isNotEmpty()) { "At least one time is required" }
            require(times.size <= MAX_TIMES_PER_DAY) { "At most $MAX_TIMES_PER_DAY times a day, got: ${times.size}" }
            require(times.distinct().size == times.size) { "Times must be distinct" }
            require(times == times.sorted()) { "Times must be in ascending order" }
            require(end == null || !end.isBefore(start)) { "End date must not be before the start date" }
        }

        fun isDueOn(date: LocalDate): Boolean {
            if (date.isBefore(start)) return false
            if (end != null && date.isAfter(end)) return false
            return when (days) {
                is DayPattern.Weekdays -> date.dayOfWeek in days.days
                is DayPattern.EveryNDays -> ChronoUnit.DAYS.between(start, date) % days.interval == 0L
            }
        }

        companion object {
            const val MAX_TIMES_PER_DAY = 8

            /** Sorts and de-duplicates [times] so callers can pass them in the order the user entered. */
            fun of(times: Collection<LocalTime>, days: DayPattern, start: LocalDate, end: LocalDate? = null): Recurring =
                Recurring(times.distinct().sorted(), days, start, end)
        }
    }

    /** Planned local wall-clock times on [date]; empty for as-needed. */
    fun plannedFor(date: LocalDate): List<LocalDateTime> = when (this) {
        AsNeeded -> emptyList()
        is Recurring -> if (isDueOn(date)) times.map { date.atTime(it) } else emptyList()
    }
}

/** A schedule that applies from [effectiveFrom] onward, until a later version takes over. */
data class ScheduleVersion(val effectiveFrom: LocalDate, val schedule: Schedule)
