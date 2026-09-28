package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.time.Instant

class RecordActivityUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        startTime: Instant,
        endTime: Instant,
        distanceInMeters: Double,
        measurementId: MeasurementId = MeasurementId.generate()
    ): ActivitySession {
        val session = ActivitySession(
            id = measurementId,
            profileId = profileId,
            startTime = startTime,
            endTime = endTime,
            distanceInMeters = distanceInMeters
        )

        healthLogRepository.saveActivity(session)
        return session
    }
}
