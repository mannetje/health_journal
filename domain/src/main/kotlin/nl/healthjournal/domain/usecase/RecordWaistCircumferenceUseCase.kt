package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.WaistCircumferenceCm
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import java.time.Instant

class RecordWaistCircumferenceUseCase(
    private val healthLogRepository: HealthLogRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        waistCm: Double,
        timestamp: Instant = Instant.now(),
        measurementId: MeasurementId = MeasurementId.generate(),
        comment: String? = null
    ): WaistCircumferenceEntry {
        val waist = WaistCircumferenceCm(waistCm)
        val profile = profileRepository.getById(profileId)

        val category = profile?.classifyWaistCircumference(waist)

        val entry = WaistCircumferenceEntry(
            id = measurementId,
            profileId = profileId,
            timestamp = timestamp,
            waist = waist,
            category = category,
            comment = EntryComment.ofOrNull(comment)
        )

        healthLogRepository.saveWaistCircumference(entry)
        return entry
    }
}
