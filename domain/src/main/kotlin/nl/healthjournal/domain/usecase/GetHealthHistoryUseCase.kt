package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort

data class HealthHistory(
    val weights: List<WeightEntry>,
    val bloodPressures: List<BloodPressureEntry>,
    val glucoses: List<GlucoseEntry>,
    val activities: List<ActivitySession>
)

class GetHealthHistoryUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(profileId: ProfileId): HealthHistory {
        return HealthHistory(
            weights = healthLogRepository.getWeightHistory(profileId),
            bloodPressures = healthLogRepository.getBloodPressureHistory(profileId),
            glucoses = healthLogRepository.getGlucoseHistory(profileId),
            activities = healthLogRepository.getActivityHistory(profileId)
        )
    }
}
