package nl.healthjournal.domain.metrics.usecase

import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.ActivitySession
import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.GlucoseEntry
import nl.healthjournal.domain.metrics.model.OxygenSaturationEntry
import nl.healthjournal.domain.metrics.model.WaistCircumferenceEntry
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import nl.healthjournal.domain.metrics.port.GlucoseRepositoryPort
import nl.healthjournal.domain.metrics.port.ActivityRepositoryPort
import nl.healthjournal.domain.metrics.port.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.metrics.port.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.metrics.port.FluidRepositoryPort
import nl.healthjournal.domain.metrics.model.FluidIntakeEntry

data class HealthHistory(
    val weights: List<WeightEntry>,
    val bloodPressures: List<BloodPressureEntry>,
    val glucoses: List<GlucoseEntry>,
    val activities: List<ActivitySession>,
    val waistCircumferences: List<WaistCircumferenceEntry> = emptyList(),
    val oxygenSaturations: List<OxygenSaturationEntry> = emptyList(),
    val fluidIntakes: List<FluidIntakeEntry> = emptyList()
)

class GetHealthHistoryUseCase(
    private val weightRepository: WeightRepositoryPort,
    private val bloodPressureRepository: BloodPressureRepositoryPort,
    private val glucoseRepository: GlucoseRepositoryPort,
    private val activityRepository: ActivityRepositoryPort,
    private val waistCircumferenceRepository: WaistCircumferenceRepositoryPort,
    private val oxygenSaturationRepository: OxygenSaturationRepositoryPort,
    private val fluidRepository: FluidRepositoryPort
) {
    suspend operator fun invoke(profileId: ProfileId): HealthHistory {
        return HealthHistory(
            weights = weightRepository.getWeightHistory(profileId),
            bloodPressures = bloodPressureRepository.getBloodPressureHistory(profileId),
            glucoses = glucoseRepository.getGlucoseHistory(profileId),
            activities = activityRepository.getActivityHistory(profileId),
            waistCircumferences = waistCircumferenceRepository.getWaistCircumferenceHistory(profileId),
            oxygenSaturations = oxygenSaturationRepository.getOxygenSaturationHistory(profileId),
            fluidIntakes = fluidRepository.getFluidHistory(profileId)
        )
    }
}
