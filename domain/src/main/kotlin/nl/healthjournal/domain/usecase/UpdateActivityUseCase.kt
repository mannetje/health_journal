package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.time.Instant

/** Replaces an existing activity session (same id and profile). Returns null when the id is unknown. */
class UpdateActivityUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        id: MeasurementId,
        profileId: ProfileId,
        startTime: Instant,
        endTime: Instant,
        distanceInMeters: Double
    ): ActivitySession? {
        val session = ActivitySession(
            id = id,
            profileId = profileId,
            startTime = startTime,
            endTime = endTime,
            distanceInMeters = distanceInMeters
        )
        return session.takeIf { healthLogRepository.updateActivity(it) }
    }
}
