package nl.healthjournal.domain.model.medication

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.EntryComment
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant

data class Medication(
    val id: MedicationId,
    val profileId: ProfileId,
    val name: MedicationName,
    val form: MedicationForm?,
    val dosage: Dosage,
    val appearance: PillAppearance?,
    /** At least one version, strictly ordered by [ScheduleVersion.effectiveFrom]. */
    val schedules: List<ScheduleVersion>,
    val comment: EntryComment? = null,
    /** Set when archived: planned intakes from this date on are not generated, earlier dates stay as they were. */
    val archivedFrom: LocalDate? = null
) {
    init {
        require(schedules.isNotEmpty()) { "A medication needs at least one schedule version" }
        require(schedules.zipWithNext().all { (a, b) -> a.effectiveFrom.isBefore(b.effectiveFrom) }) {
            "Schedule versions must be strictly ordered by effective-from date"
        }
    }

    /** The version in force on [date]: the latest one with `effectiveFrom <= date`, or null before the first. */
    fun scheduleInForce(date: LocalDate): ScheduleVersion? =
        schedules.lastOrNull { !it.effectiveFrom.isAfter(date) }

    val isAsNeeded: Boolean
        get() = schedules.last().schedule == Schedule.AsNeeded

    /**
     * Planned local wall-clock times on [date], from the schedule version in force on that date.
     * Single source for the pillbox, reminders and adherence. Empty when archived from [date] or earlier.
     */
    fun plannedFor(date: LocalDate): List<LocalDateTime> {
        if (archivedFrom != null && !date.isBefore(archivedFrom)) return emptyList()
        return scheduleInForce(date)?.schedule?.plannedFor(date).orEmpty()
    }

    /**
     * Adds a version applying from [effectiveFrom], or replaces the version that starts on that date.
     * An earlier date than today is the documented exception that changes derived past planned intakes;
     * recorded outcomes are never touched.
     */
    fun withScheduleFrom(effectiveFrom: LocalDate, schedule: Schedule): Medication {
        val kept = schedules.filter { it.effectiveFrom != effectiveFrom }
        val updated = (kept + ScheduleVersion(effectiveFrom, schedule)).sortedBy { it.effectiveFrom }
        return copy(schedules = updated)
    }

    fun archivedOn(date: LocalDate): Medication = copy(archivedFrom = date)
}

/** A planned local time resolved in [zone]; a time inside a daylight-saving gap moves forward. */
fun LocalDateTime.resolveIn(zone: ZoneId): Instant = ZonedDateTime.of(this, zone).toInstant()
