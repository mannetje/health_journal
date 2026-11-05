package nl.healthjournal.domain.medication.port

import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.medication.model.Intake
import nl.healthjournal.domain.medication.model.IntakeId
import nl.healthjournal.domain.medication.model.Medication
import nl.healthjournal.domain.medication.model.MedicationId
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

interface MedicationRepositoryPort {
    suspend fun saveMedication(medication: Medication)
    suspend fun getMedication(id: MedicationId): Medication?

    /** Medications of [profileId]; archived ones only when [includeArchived]. */
    suspend fun getMedications(profileId: ProfileId, includeArchived: Boolean = false): List<Medication>

    /** Removes the medication and all its recorded intakes. */
    suspend fun deleteMedication(id: MedicationId)

    /** Inserts, or replaces the outcome with the same (medication, planned time) when [Intake.planned] is set. */
    suspend fun saveIntake(intake: Intake)
    suspend fun getIntake(medicationId: MedicationId, planned: LocalDateTime): Intake?
    suspend fun deleteIntake(id: IntakeId)

    /** Every recorded outcome of [profileId], for export and duplicate checks on import. */
    suspend fun getAllIntakes(profileId: ProfileId): List<Intake>

    /** Outcomes planned on [date], plus as-needed doses taken on [date] in [zone], for [profileId]. */
    suspend fun getIntakesForDay(profileId: ProfileId, date: LocalDate, zone: ZoneId): List<Intake>
}
