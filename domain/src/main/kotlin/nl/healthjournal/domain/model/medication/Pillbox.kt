package nl.healthjournal.domain.model.medication

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Derived state of one planned intake. Pending and missed are never stored. */
enum class PlannedStatus { PENDING, MISSED, TAKEN, SKIPPED }

data class SlotItem(
    val medication: Medication,
    val planned: LocalDateTime,
    val status: PlannedStatus,
    val intake: Intake?
)

/** All planned intakes at the same local time. Derived, never stored; the unit "Taken all" acts on. */
data class Slot(val time: LocalDateTime, val items: List<SlotItem>) {
    /** Rows that "Taken all" records: the ones still without an outcome. */
    val openItems: List<SlotItem>
        get() = items.filter { it.status == PlannedStatus.PENDING || it.status == PlannedStatus.MISSED }
}

/**
 * One day of the pillbox: slots in time order, plus intakes that are logged but not planned
 * (as-needed doses and orphans whose planned time the schedule no longer produces).
 */
data class PillboxDay(
    val date: LocalDate,
    val slots: List<Slot>,
    val logged: List<Intake>
)

object Pillbox {
    /** An intake with no outcome this long after its planned time is shown as missed. */
    val GRACE_PERIOD: Duration = Duration.ofHours(2)

    fun statusOf(planned: LocalDateTime, intake: Intake?, now: LocalDateTime): PlannedStatus = when {
        intake?.status == IntakeStatus.TAKEN -> PlannedStatus.TAKEN
        intake?.status == IntakeStatus.SKIPPED -> PlannedStatus.SKIPPED
        now.isBefore(planned.plus(GRACE_PERIOD)) -> PlannedStatus.PENDING
        else -> PlannedStatus.MISSED
    }

    /**
     * Builds the pillbox for [date] from [medications] (the active profile's) and their [intakes].
     * [now] is local wall-clock time; [zone] places as-needed doses (an instant) on a local date.
     */
    fun dayOf(
        date: LocalDate,
        medications: List<Medication>,
        intakes: List<Intake>,
        now: LocalDateTime,
        zone: ZoneId
    ): PillboxDay {
        val byMedication = medications.associateBy { it.id }
        val planned = intakes.filter { it.planned != null }.associateBy { it.medicationId to it.planned }

        val items = medications.flatMap { medication ->
            medication.plannedFor(date).map { time ->
                val intake = planned[medication.id to time]
                SlotItem(medication, time, statusOf(time, intake, now), intake)
            }
        }
        val slots = items
            .groupBy { it.planned }
            .toSortedMap()
            .map { (time, group) -> Slot(time, group.sortedBy { it.medication.name.value.lowercase() }) }

        val plannedKeys = items.map { it.medication.id to it.planned }.toSet()
        val logged = intakes
            .filter { it.medicationId in byMedication }
            .filter { intake ->
                val time = intake.planned
                if (time == null) {
                    intake.takenAt!!.atZone(zone).toLocalDate() == date
                } else {
                    time.toLocalDate() == date && (intake.medicationId to time) !in plannedKeys
                }
            }
            .sortedBy { it.planned ?: it.takenAt!!.atZone(zone).toLocalDateTime() }

        return PillboxDay(date, slots, logged)
    }
}
