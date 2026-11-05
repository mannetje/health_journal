package nl.healthjournal.domain.testing

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort

/** In-memory medications and intakes, with the real repository's replace rule. */
class FakeMedicationRepository : MedicationRepositoryPort {
    val medications = mutableMapOf<MedicationId, Medication>()
    val intakes = mutableListOf<Intake>()

    override suspend fun saveMedication(medication: Medication) {
        medications[medication.id] = medication
    }

    override suspend fun getMedication(id: MedicationId): Medication? = medications[id]

    override suspend fun getMedications(profileId: ProfileId, includeArchived: Boolean): List<Medication> =
        medications.values.filter { it.profileId == profileId && (includeArchived || it.archivedFrom == null) }

    override suspend fun deleteMedication(id: MedicationId) {
        medications.remove(id)
        intakes.removeAll { it.medicationId == id }
    }

    override suspend fun saveIntake(intake: Intake) {
        // Same replace rule as the real repository: one outcome per (medication, planned time).
        if (intake.planned != null) {
            intakes.removeAll { it.medicationId == intake.medicationId && it.planned == intake.planned }
        }
        intakes.removeAll { it.id == intake.id }
        intakes.add(intake)
    }

    override suspend fun getIntake(medicationId: MedicationId, planned: LocalDateTime): Intake? =
        intakes.firstOrNull { it.medicationId == medicationId && it.planned == planned }

    override suspend fun deleteIntake(id: IntakeId) {
        intakes.removeAll { it.id == id }
    }

    override suspend fun getAllIntakes(profileId: ProfileId): List<Intake> =
        intakes.filter { medications[it.medicationId]?.profileId == profileId }

    override suspend fun getIntakesForDay(profileId: ProfileId, date: LocalDate, zone: ZoneId): List<Intake> =
        getAllIntakes(profileId).filter { intake ->
            val planned = intake.planned
            if (planned != null) planned.toLocalDate() == date
            else intake.takenAt!!.atZone(zone).toLocalDate() == date
        }
}
