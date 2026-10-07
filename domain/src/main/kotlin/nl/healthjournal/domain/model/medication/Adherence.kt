package nl.healthjournal.domain.model.medication

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Counts over the chosen range. Only status, never a judgement. */
data class AdherenceCounts(
    val taken: Int,
    val skipped: Int,
    val missed: Int,
    /** As-needed doses logged in the range; always 0 for a scheduled medication. */
    val asNeededDoses: Int
) {
    /** Planned intakes in the past: taken, skipped and missed. One still inside the grace period is not counted yet. */
    val due: Int
        get() = taken + skipped + missed

    /** Taken divided by due as a whole percent, half up; null when nothing was due (and for as-needed). */
    val percentage: Int?
        get() = Adherence.percent(taken, due)
}

data class AdherenceRow(val medication: Medication, val counts: AdherenceCounts)

data class MissedIntake(val medication: Medication, val planned: LocalDateTime)

data class AdherenceReport(
    val from: LocalDate,
    val to: LocalDate,
    val rows: List<AdherenceRow>,
    val overall: AdherenceCounts?,
    /** Consecutive days up to today with every planned intake taken; independent of the range. */
    val streak: Int,
    /** Newest first. */
    val missed: List<MissedIntake>
)

/** Pure calculation over schedule versions and recorded outcomes, using the same plan as the pillbox. */
object Adherence {
    val RANGES_IN_DAYS = listOf(7, 30, 90)

    /** The streak looks back at most this far. */
    private const val MAX_STREAK_DAYS = 365

    /** Taken divided by due, rounded half up to a whole percent; null when [due] is 0. */
    fun percent(taken: Int, due: Int): Int? =
        if (due <= 0) null else ((200L * taken + due) / (2L * due)).toInt()

    /**
     * Adherence for the last [days] days including today. [now] is local wall-clock time and [zone] places
     * as-needed doses (an instant) on a local date. Outcomes whose planned time the schedule no longer produces
     * are ignored, because planned times are taken from the schedule only.
     */
    fun report(
        medications: List<Medication>,
        intakes: List<Intake>,
        days: Int,
        now: LocalDateTime,
        zone: ZoneId
    ): AdherenceReport {
        require(days > 0) { "The range must be at least one day" }
        val today = now.toLocalDate()
        val from = today.minusDays((days - 1).toLong())
        val planned = intakes.filter { it.planned != null }.associateBy { it.medicationId to it.planned }
        val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()

        val missedList = mutableListOf<MissedIntake>()
        val rows = medications.mapNotNull { medication ->
            var taken = 0
            var skipped = 0
            var missed = 0
            for (date in dates) {
                for (time in medication.plannedFor(date)) {
                    when (Pillbox.statusOf(time, planned[medication.id to time], now)) {
                        PlannedStatus.TAKEN -> taken++
                        PlannedStatus.SKIPPED -> skipped++
                        PlannedStatus.MISSED -> {
                            missed++
                            missedList += MissedIntake(medication, time)
                        }
                        PlannedStatus.PENDING -> Unit
                    }
                }
            }
            val doses = if (medication.isAsNeeded) {
                intakes.count {
                    it.medicationId == medication.id && it.planned == null &&
                        it.takenAt!!.atZone(zone).toLocalDate() in from..today
                }
            } else {
                0
            }
            AdherenceCounts(taken, skipped, missed, doses).takeIf { it.due > 0 || it.asNeededDoses > 0 }?.let { AdherenceRow(medication, it) }
        }.sortedBy { it.medication.name.value.lowercase() }

        val overall = rows.takeIf { it.isNotEmpty() }?.let { all ->
            AdherenceCounts(
                taken = all.sumOf { it.counts.taken },
                skipped = all.sumOf { it.counts.skipped },
                missed = all.sumOf { it.counts.missed },
                asNeededDoses = all.sumOf { it.counts.asNeededDoses }
            )
        }

        return AdherenceReport(
            from = from,
            to = today,
            rows = rows,
            overall = overall,
            streak = streak(medications, planned, now),
            missed = missedList.sortedByDescending { it.planned }
        )
    }

    /**
     * Days in a row, counting back from today, on which something was planned and all of it was taken. A day with
     * nothing planned, or one still waiting for an outcome, neither extends nor breaks the streak.
     */
    private fun streak(
        medications: List<Medication>,
        planned: Map<Pair<MedicationId, LocalDateTime?>, Intake>,
        now: LocalDateTime
    ): Int {
        var count = 0
        var date = now.toLocalDate()
        repeat(MAX_STREAK_DAYS) {
            val statuses = medications.flatMap { medication ->
                medication.plannedFor(date).map { Pillbox.statusOf(it, planned[medication.id to it], now) }
            }
            when {
                statuses.any { it == PlannedStatus.SKIPPED || it == PlannedStatus.MISSED } -> return count
                statuses.isNotEmpty() && statuses.all { it == PlannedStatus.TAKEN } -> count++
            }
            date = date.minusDays(1)
        }
        return count
    }
}
