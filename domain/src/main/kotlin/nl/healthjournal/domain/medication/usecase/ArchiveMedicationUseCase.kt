package nl.healthjournal.domain.medication.usecase

import nl.healthjournal.domain.medication.model.Medication
import nl.healthjournal.domain.medication.model.MedicationId
import nl.healthjournal.domain.medication.port.MedicationRepositoryPort
import java.time.LocalDate

/** Stops planned intakes from [from] (default today); earlier days and recorded outcomes stay as they were. */
class ArchiveMedicationUseCase(
    private val medicationRepository: MedicationRepositoryPort
) {
    suspend operator fun invoke(id: MedicationId, from: LocalDate = LocalDate.now()): Medication {
        val medication = requireNotNull(medicationRepository.getMedication(id)) { "Medication not found: $id" }
        val archived = medication.archivedOn(from)
        medicationRepository.saveMedication(archived)
        return archived
    }
}
