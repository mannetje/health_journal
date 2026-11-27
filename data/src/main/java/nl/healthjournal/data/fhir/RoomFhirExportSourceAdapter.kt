package nl.healthjournal.data.fhir

import nl.healthjournal.domain.metrics.port.FhirExportSourcePort
import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.port.ProfileRepositoryPort
import nl.healthjournal.domain.shared.model.ProfileId

class RoomFhirExportSourceAdapter(
    private val weightRepository: WeightRepositoryPort,
    private val bloodPressureRepository: BloodPressureRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) : FhirExportSourcePort {

    override suspend fun getWeights(profileId: ProfileId): List<WeightEntry> =
        weightRepository.getWeightHistory(profileId)

    override suspend fun getBloodPressures(profileId: ProfileId): List<BloodPressureEntry> =
        bloodPressureRepository.getBloodPressureHistory(profileId)

    override suspend fun getProfile(profileId: ProfileId): Profile? =
        profileRepository.getById(profileId)
}
