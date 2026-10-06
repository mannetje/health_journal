package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.BloodPressureReading
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.time.Instant

class RecordBloodPressureUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        systolic: Int,
        diastolic: Int,
        pulse: Int? = null,
        timestamp: Instant = Instant.now(),
        measurementId: MeasurementId = MeasurementId.generate(),
        comment: String? = null
    ): BloodPressureEntry {
        val reading = BloodPressureReading(systolic = systolic, diastolic = diastolic, pulse = pulse)
        val category = NhgBloodPressureCategory.classify(reading)

        val entry = BloodPressureEntry(
            id = measurementId,
            profileId = profileId,
            timestamp = timestamp,
            reading = reading,
            category = category,
            comment = EntryComment.ofOrNull(comment)
        )

        healthLogRepository.saveBloodPressure(entry)
        return entry
    }
}
