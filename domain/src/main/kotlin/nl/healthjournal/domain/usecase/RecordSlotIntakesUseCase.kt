package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.MedicationId
import java.time.Instant
import java.time.LocalDateTime

/** "Taken all": records one taken outcome per medication of a slot, all at the same [takenAt]. */
class RecordSlotIntakesUseCase(
    private val recordIntake: RecordIntakeUseCase
) {
    suspend operator fun invoke(
        medicationIds: List<MedicationId>,
        planned: LocalDateTime,
        takenAt: Instant = Instant.now()
    ): List<Intake> = medicationIds.distinct().map { id ->
        recordIntake(id, planned, IntakeStatus.TAKEN, takenAt)
    }
}
