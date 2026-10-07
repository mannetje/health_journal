package nl.healthjournal.domain.model.medication

import java.time.LocalDateTime

/** Finds the next moment a reminder is due. Pure, so it can be tested without any clock or alarm. */
object NextSlot {
    /** A recurring schedule is due again within [DayPattern.EveryNDays.MAX_INTERVAL] days, so one more day is enough. */
    private const val SEARCH_DAYS = DayPattern.EveryNDays.MAX_INTERVAL + 1

    /**
     * The earliest planned local time strictly after [after] over [medications], or null when nothing is planned.
     * Archived medications, ended schedules and as-needed medications add nothing; the schedule version in force on
     * each date decides, the same rule the pillbox uses ([Medication.plannedFor]).
     */
    fun after(medications: List<Medication>, after: LocalDateTime): LocalDateTime? {
        val first = after.toLocalDate()
        for (offset in 0..SEARCH_DAYS) {
            val date = first.plusDays(offset.toLong())
            val next = medications
                .flatMap { it.plannedFor(date) }
                .filter { it.isAfter(after) }
                .minOrNull()
            if (next != null) return next
        }
        return null
    }
}
