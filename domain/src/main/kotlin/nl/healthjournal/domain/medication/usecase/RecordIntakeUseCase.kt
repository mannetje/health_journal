package nl.healthjournal.domain.medication.usecase

import nl.healthjournal.domain.medication.model.Intake
import nl.healthjournal.domain.medication.model.IntakeId
import nl.healthjournal.domain.medication.model.IntakeStatus
import nl.healthjournal.domain.medication.model.MedicationId
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.medication.port.MedicationRepositoryPort
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/**
 * Records the outcome of a planned intake ([planned] set) or an as-needed dose ([planned] null).
 * Recording again for the same planned intake replaces the earlier outcome.
 */
class RecordIntakeUseCase(
    private val medicationRepository: MedicationRepositoryPort,
    private val clock: Clock = Clock.systemUTC()
) {
    suspend operator fun invoke(
        medicationId: MedicationId,
        planned: LocalDateTime?,
        status: IntakeStatus = IntakeStatus.TAKEN,
        takenAt: Instant? = null,
        actualAmount: BigDecimal? = null,
        comment: String? = null
    ): Intake {
        requireNotNull(medicationRepository.getMedication(medicationId)) { "Medication not found: $medicationId" }

        val existing = planned?.let { medicationRepository.getIntake(medicationId, it) }
        val intake = Intake(
            id = existing?.id ?: IntakeId.generate(),
            medicationId = medicationId,
            planned = planned,
            status = status,
            takenAt = if (status == IntakeStatus.TAKEN) takenAt ?: clock.instant() else null,
            actualAmount = if (status == IntakeStatus.TAKEN) actualAmount else null,
            comment = EntryComment.ofOrNull(comment)
        )
        medicationRepository.saveIntake(intake)
        return intake
    }
}
