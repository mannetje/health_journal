package nl.healthjournal.domain.medication.usecase

import nl.healthjournal.domain.medication.model.Intake
import nl.healthjournal.domain.medication.model.IntakeStatus
import nl.healthjournal.domain.medication.model.MedicationId
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/** "Taken all": records one taken outcome per medication of a slot, all at the same [takenAt]. */
class RecordSlotIntakesUseCase(
    private val recordIntake: RecordIntakeUseCase,
    private val clock: Clock = Clock.systemUTC()
) {
    suspend operator fun invoke(
        medicationIds: List<MedicationId>,
        planned: LocalDateTime,
        takenAt: Instant? = null
    ): List<Intake> {
        val at = takenAt ?: clock.instant()
        return medicationIds.distinct().map { id -> recordIntake(id, planned, IntakeStatus.TAKEN, at) }
    }
}
