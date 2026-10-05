package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort

/** Permanently deletes one waist circumference entry by id. Returns false when the id is unknown. */
class DeleteWaistCircumferenceUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(id: MeasurementId): Boolean = healthLogRepository.deleteWaistCircumference(id)
}
