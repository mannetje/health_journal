package nl.healthjournal.domain.medication.usecase

import nl.healthjournal.domain.medication.model.Medication
import nl.healthjournal.domain.medication.model.MedicationId
import nl.healthjournal.domain.medication.model.Schedule
import nl.healthjournal.domain.medication.port.MedicationRepositoryPort
import java.time.LocalDate

/**
 * Adds a schedule version applying from [effectiveFrom] (default today). Recorded outcomes are never changed;
 * an earlier date changes the derived past planned intakes and is the caller's explicit choice.
 */
class ChangeMedicationScheduleUseCase(
    private val medicationRepository: MedicationRepositoryPort
) {
    suspend operator fun invoke(
        id: MedicationId,
        schedule: Schedule,
        effectiveFrom: LocalDate = LocalDate.now()
    ): Medication {
        val medication = requireNotNull(medicationRepository.getMedication(id)) { "Medication not found: $id" }
        val updated = medication.withScheduleFrom(effectiveFrom, schedule)
        medicationRepository.saveMedication(updated)
        return updated
    }
}
