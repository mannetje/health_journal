package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.WaistCircumferenceCm
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import java.time.Instant

/** Replaces an existing waist circumference entry (same id and profile); recomputes the category. Returns null when the id is unknown. */
class UpdateWaistCircumferenceUseCase(
    private val healthLogRepository: HealthLogRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) {
    suspend operator fun invoke(
        id: MeasurementId,
        profileId: ProfileId,
        waistCm: Double,
        timestamp: Instant
    ): WaistCircumferenceEntry? {
        val waist = WaistCircumferenceCm(waistCm)
        val category = profileRepository.getById(profileId)?.classifyWaistCircumference(waist)
        val entry = WaistCircumferenceEntry(id = id, profileId = profileId, timestamp = timestamp, waist = waist, category = category)
        return entry.takeIf { healthLogRepository.updateWaistCircumference(it) }
    }
}
