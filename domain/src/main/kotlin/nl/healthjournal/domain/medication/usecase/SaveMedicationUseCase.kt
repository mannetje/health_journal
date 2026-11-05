package nl.healthjournal.domain.medication.usecase

import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.medication.model.Dosage
import nl.healthjournal.domain.medication.model.DoseUnit
import nl.healthjournal.domain.medication.model.Medication
import nl.healthjournal.domain.medication.model.MedicationForm
import nl.healthjournal.domain.medication.model.MedicationId
import nl.healthjournal.domain.medication.model.MedicationName
import nl.healthjournal.domain.medication.model.PillAppearance
import nl.healthjournal.domain.medication.model.Schedule
import nl.healthjournal.domain.medication.model.ScheduleVersion
import nl.healthjournal.domain.medication.model.Strength
import nl.healthjournal.domain.medication.model.StrengthUnit
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.medication.port.MedicationRepositoryPort
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Creates a medication, or updates the details of an existing one ([existingId]).
 * An update keeps the schedule versions and the archived state; use [ChangeMedicationScheduleUseCase] for those.
 */
class SaveMedicationUseCase(
    private val medicationRepository: MedicationRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        name: String,
        form: MedicationForm?,
        strengthAmount: BigDecimal?,
        strengthUnit: StrengthUnit?,
        amountPerIntake: BigDecimal,
        doseUnit: DoseUnit,
        appearance: PillAppearance?,
        schedule: Schedule,
        comment: String? = null,
        effectiveFrom: LocalDate = LocalDate.now(),
        existingId: MedicationId? = null
    ): Medication {
        val strength = if (strengthAmount != null && strengthUnit != null) Strength(strengthAmount, strengthUnit) else null
        val dosage = Dosage(strength, amountPerIntake, doseUnit)
        val existing = existingId?.let { medicationRepository.getMedication(it) }

        val medication = Medication(
            id = existing?.id ?: existingId ?: MedicationId.generate(),
            profileId = profileId,
            name = MedicationName.of(name),
            form = form,
            dosage = dosage,
            appearance = appearance,
            schedules = existing?.schedules ?: listOf(ScheduleVersion(effectiveFrom, schedule)),
            comment = EntryComment.ofOrNull(comment),
            archivedFrom = existing?.archivedFrom
        )
        medicationRepository.saveMedication(medication)
        return medication
    }
}
