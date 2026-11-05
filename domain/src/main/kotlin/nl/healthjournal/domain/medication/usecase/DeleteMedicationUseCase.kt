package nl.healthjournal.domain.medication.usecase

import nl.healthjournal.domain.medication.model.MedicationId
import nl.healthjournal.domain.medication.port.MedicationRepositoryPort

/** Permanently removes the medication together with its recorded intakes. */
class DeleteMedicationUseCase(
    private val medicationRepository: MedicationRepositoryPort
) {
    suspend operator fun invoke(id: MedicationId) {
        medicationRepository.deleteMedication(id)
    }
}
