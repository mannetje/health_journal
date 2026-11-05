package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.MedicationId
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
