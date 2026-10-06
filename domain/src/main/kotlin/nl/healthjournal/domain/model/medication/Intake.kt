package nl.healthjournal.domain.model.medication

import nl.healthjournal.domain.model.metrics.EntryComment
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

/** Pending and missed are derived from the log and the clock, never stored. */
enum class IntakeStatus { TAKEN, SKIPPED }

/**
 * The recorded outcome for a planned intake, or an as-needed dose when [planned] is null.
 * A planned intake is unique by (medication, planned time); as-needed doses are unique by id.
 */
data class Intake(
    val id: IntakeId,
    val medicationId: MedicationId,
    val planned: LocalDateTime?,
    val status: IntakeStatus,
    val takenAt: Instant?,
    /** Set when the amount taken differs from the planned dose. */
    val actualAmount: BigDecimal? = null,
    /** For example an injection site; the app never suggests one. */
    val comment: EntryComment? = null
) {
    init {
        if (planned == null) {
            require(status == IntakeStatus.TAKEN) { "An as-needed dose can only be recorded as taken" }
            require(takenAt != null) { "An as-needed dose needs the time it was taken" }
        }
        if (status == IntakeStatus.SKIPPED) {
            require(takenAt == null) { "A skipped intake has no time taken" }
            require(actualAmount == null) { "A skipped intake has no actual amount" }
        }
        actualAmount?.let(Dosage::requireValidAmount)
    }

    val isAsNeeded: Boolean
        get() = planned == null
}
