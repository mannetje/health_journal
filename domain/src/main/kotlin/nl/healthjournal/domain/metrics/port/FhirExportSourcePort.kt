package nl.healthjournal.domain.metrics.port

import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.shared.model.ProfileId

/**
 * Read-only port providing entries and profile for FHIR-based export.
 */
interface FhirExportSourcePort {
    suspend fun getWeights(profileId: ProfileId): List<WeightEntry>
    suspend fun getBloodPressures(profileId: ProfileId): List<BloodPressureEntry>
    suspend fun getProfile(profileId: ProfileId): Profile?
}
