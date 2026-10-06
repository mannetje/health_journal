package nl.healthjournal.domain.model.medication

import nl.healthjournal.domain.model.common.ProfileId
import java.math.BigDecimal

/** Placeholder names only ("Medication A"); tests never use real medicine names. */
fun medication(
    name: String,
    schedules: List<ScheduleVersion>,
    profileId: ProfileId = ProfileId.generate()
) = Medication(
    id = MedicationId.generate(),
    profileId = profileId,
    name = MedicationName.of(name),
    form = MedicationForm.TABLET,
    dosage = Dosage(Strength(BigDecimal("10"), StrengthUnit.MG), BigDecimal.ONE, DoseUnit.TABLETS),
    appearance = null,
    schedules = schedules
)
