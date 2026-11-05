package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.command.*
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import java.time.Duration
import java.time.Instant
import nl.healthjournal.domain.model.common.EntryComment

data class ActivitySession(
    val id: MeasurementId,
    val profileId: ProfileId,
    val startTime: Instant,
    val endTime: Instant,
    val distanceInMeters: Double,
    val comment: EntryComment? = null
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
        fun of(
            id: MeasurementId,
            profileId: ProfileId,
            startTime: Instant,
            endTime: Instant,
            distanceInMeters: Double,
            comment: EntryComment? = null
        ): DomainResult<ActivitySession> = validated(
            if (endTime.isAfter(startTime)) null
            else DomainError.Invalid(ValidationField.TIME_RANGE, ValidationRule.END_NOT_AFTER_START),
            rangeError(ValidationField.DISTANCE, distanceInMeters, 0.0, MAX_DISTANCE_METERS)
        ) { ActivitySession(id, profileId, startTime, endTime, distanceInMeters, comment) }

        const val MAX_DISTANCE_METERS = 1_000_000.0
    }
}
