package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.BloodPressureReading
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.time.Instant

/** Replaces an existing blood pressure entry (same id and profile). Returns null when the id is unknown. */
class UpdateBloodPressureUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        id: MeasurementId,
        profileId: ProfileId,
        systolic: Int,
        diastolic: Int,
        timestamp: Instant,
        pulse: Int? = null,
        comment: String? = null
    ): BloodPressureEntry? {
        val reading = BloodPressureReading(systolic = systolic, diastolic = diastolic, pulse = pulse)
        val entry = BloodPressureEntry(
            id = id,
            profileId = profileId,
            timestamp = timestamp,
            reading = reading,
            category = NhgBloodPressureCategory.classify(reading),
            comment = EntryComment.ofOrNull(comment)
        )
        return entry.takeIf { healthLogRepository.updateBloodPressure(it) }
    }
}
