package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

/**
 * Records the outcome of a planned intake ([planned] set) or an as-needed dose ([planned] null).
 * Recording again for the same planned intake replaces the earlier outcome.
 */
class RecordIntakeUseCase(
    private val medicationRepository: MedicationRepositoryPort
) {
    suspend operator fun invoke(
        medicationId: MedicationId,
        planned: LocalDateTime?,
        status: IntakeStatus = IntakeStatus.TAKEN,
        takenAt: Instant = Instant.now(),
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
            takenAt = if (status == IntakeStatus.TAKEN) takenAt else null,
            actualAmount = if (status == IntakeStatus.TAKEN) actualAmount else null,
            comment = EntryComment.ofOrNull(comment)
        )
        medicationRepository.saveIntake(intake)
        return intake
    }
}
