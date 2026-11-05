package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.OxygenSaturationEntry
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.port.secondary.WeightRepositoryPort
import nl.healthjournal.domain.port.secondary.BloodPressureRepositoryPort
import nl.healthjournal.domain.port.secondary.GlucoseRepositoryPort
import nl.healthjournal.domain.port.secondary.ActivityRepositoryPort
import nl.healthjournal.domain.port.secondary.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.port.secondary.OxygenSaturationRepositoryPort

data class HealthHistory(
    val weights: List<WeightEntry>,
    val bloodPressures: List<BloodPressureEntry>,
    val glucoses: List<GlucoseEntry>,
    val activities: List<ActivitySession>,
    val waistCircumferences: List<WaistCircumferenceEntry> = emptyList(),
    val oxygenSaturations: List<OxygenSaturationEntry> = emptyList()
)

class GetHealthHistoryUseCase(
    private val weightRepository: WeightRepositoryPort,
    private val bloodPressureRepository: BloodPressureRepositoryPort,
    private val glucoseRepository: GlucoseRepositoryPort,
    private val activityRepository: ActivityRepositoryPort,
    private val waistCircumferenceRepository: WaistCircumferenceRepositoryPort,
    private val oxygenSaturationRepository: OxygenSaturationRepositoryPort
) {
    suspend operator fun invoke(profileId: ProfileId): HealthHistory {
        return HealthHistory(
            weights = weightRepository.getWeightHistory(profileId),
            bloodPressures = bloodPressureRepository.getBloodPressureHistory(profileId),
            glucoses = glucoseRepository.getGlucoseHistory(profileId),
            activities = activityRepository.getActivityHistory(profileId),
            waistCircumferences = waistCircumferenceRepository.getWaistCircumferenceHistory(profileId),
            oxygenSaturations = oxygenSaturationRepository.getOxygenSaturationHistory(profileId)
        )
    }
}
