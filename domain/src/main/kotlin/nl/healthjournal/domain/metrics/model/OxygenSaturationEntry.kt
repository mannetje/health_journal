package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.command.*
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Instant
import nl.healthjournal.domain.shared.model.EntryComment

/**
 * One oxygen saturation reading. There is no category: the app records what is entered and does not assess it
 * (ADR 0026). Context, site and [oxygenGiven] are notes by the person.
 */
data class OxygenSaturationEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val saturation: OxygenSaturationPercent,
    val pulse: Int? = null,
    val context: OxygenContext? = null,
    val site: OxygenSite? = null,
    val oxygenGiven: Boolean = false,
    val comment: EntryComment? = null
) {
    init {
        pulse?.let {
            require(it in MIN_PULSE..MAX_PULSE) { "Pulse must be between $MIN_PULSE and $MAX_PULSE bpm, got: $it" }
        }
    }

    companion object {
        /** Null when [pulse] is a valid heart rate in bpm; the same rule as for blood pressure. */
        fun pulseError(pulse: Int): DomainError.Invalid? = BloodPressureReading.pulseError(pulse)

        const val MIN_PULSE = BloodPressureReading.MIN_PULSE
        const val MAX_PULSE = BloodPressureReading.MAX_PULSE
    }
}
