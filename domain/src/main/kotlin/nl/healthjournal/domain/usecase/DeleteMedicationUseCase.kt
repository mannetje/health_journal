package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort

/** Permanently removes the medication together with its recorded intakes. */
class DeleteMedicationUseCase(
    private val medicationRepository: MedicationRepositoryPort
) {
    suspend operator fun invoke(id: MedicationId) {
        medicationRepository.deleteMedication(id)
    }
}
