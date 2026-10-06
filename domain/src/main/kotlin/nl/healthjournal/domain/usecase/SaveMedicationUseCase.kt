package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Dosage
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.ScheduleVersion
import nl.healthjournal.domain.model.medication.Strength
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
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
