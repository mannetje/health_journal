package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.profile.model.Profile

data class FhirExportData(
    val profile: Profile?,
    val weights: List<WeightEntry>,
    val bloodPressures: List<BloodPressureEntry>
)
