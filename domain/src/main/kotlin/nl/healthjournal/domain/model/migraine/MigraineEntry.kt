package nl.healthjournal.domain.model.migraine

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.EntryComment
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One migraine the person noted. Only [start] is required. A missing [end] means it is ongoing.
 * Everything else is the person's own record: nothing is scored, ranked or interpreted, and the two
 * "helped me" ticks are their own note, never counted.
 */
data class MigraineEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val start: Instant,
    val wokeUpWithIt: Boolean = false,
    val end: Instant? = null,
    val intensity: Int? = null,
    val areas: Set<HeadArea> = emptySet(),
    val painCharacters: Set<PainCharacter> = emptySet(),
    val type: MigraineType? = null,
    val symptoms: Set<MigraineSymptom> = emptySet(),
    val triggers: Set<MigraineTrigger> = emptySet(),
    val customTriggerIds: Set<MigraineTagId> = emptySet(),
    val tookSomething: Boolean = false,
    val medicationText: String? = null,
    val secondDose: Boolean = false,
    val intensityAfter2Hours: Int? = null,
    val medicationHelpedMe: Boolean = false,
    val reliefMethods: Set<ReliefMethod> = emptySet(),
    val customReliefIds: Set<MigraineTagId> = emptySet(),
    val reliefHelpedMe: Boolean = false,
    val activities: DailyActivities? = null,
    val comment: EntryComment? = null
) {
    init {
        require(end == null || !end.isBefore(start)) { "End time ($end) must not be before start time ($start)" }
        require(intensity == null || intensity in INTENSITY_RANGE) { "Intensity must be 0 to 10, got: $intensity" }
        require(intensityAfter2Hours == null || intensityAfter2Hours in INTENSITY_RANGE) {
            "Intensity after 2 hours must be 0 to 10, got: $intensityAfter2Hours"
        }
        require(intensityAfter2Hours == null || tookSomething) {
            "Intensity after 2 hours is only allowed when something was taken"
        }
        require(!medicationHelpedMe || tookSomething) { "'This helped me' for medication needs something taken" }
        require(!reliefHelpedMe || reliefMethods.isNotEmpty() || customReliefIds.isNotEmpty()) {
            "'This helped me' for relief needs a relief method"
        }
        require(medicationText == null || medicationText == medicationText.trim()) { "Medication text must be trimmed" }
        require(medicationText == null || (medicationText.isNotEmpty() && medicationText.length <= MAX_MEDICATION_TEXT)) {
            "Medication text must be 1 to $MAX_MEDICATION_TEXT characters"
        }
    }

    val ongoing: Boolean get() = end == null

    /** Null while ongoing. */
    val duration: Duration? get() = end?.let { Duration.between(start, it) }

    fun startDay(zone: ZoneId): LocalDate = start.atZone(zone).toLocalDate()

    fun startWeekday(zone: ZoneId): DayOfWeek = start.atZone(zone).dayOfWeek

    fun startTimeOfDay(zone: ZoneId): TimeOfDay = TimeOfDay.of(start.atZone(zone).toLocalTime())

    companion object {
        val INTENSITY_RANGE = 0..10
        const val MAX_MEDICATION_TEXT = 100

        /** Trims; null for null or blank; throws when over [MAX_MEDICATION_TEXT] after trimming. */
        fun normalizeMedicationText(raw: String?): String? {
            val text = raw?.trim().orEmpty()
            require(text.length <= MAX_MEDICATION_TEXT) { "Medication text must be at most $MAX_MEDICATION_TEXT characters" }
            return text.ifEmpty { null }
        }
    }
}
