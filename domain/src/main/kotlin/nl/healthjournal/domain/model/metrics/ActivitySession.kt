package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import java.time.Duration
import java.time.Instant

data class ActivitySession(
    val id: MeasurementId,
    val profileId: ProfileId,
    val startTime: Instant,
    val endTime: Instant,
    val distanceInMeters: Double
) {
    init {
        require(endTime.isAfter(startTime)) {
            "End time ($endTime) must be strictly after start time ($startTime)"
        }
        require(distanceInMeters >= 0.0 && distanceInMeters <= MAX_DISTANCE_METERS) {
            "Distance in meters must be between 0.0 and $MAX_DISTANCE_METERS, got: $distanceInMeters"
        }
    }

    val duration: Duration
        get() = Duration.between(startTime, endTime)

    val durationInSeconds: Long
        get() = duration.seconds

    companion object {
        const val MAX_DISTANCE_METERS = 1_000_000.0
    }
}
