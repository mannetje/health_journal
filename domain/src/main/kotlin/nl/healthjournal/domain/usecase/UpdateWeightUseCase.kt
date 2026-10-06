package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.metrics.WeightKg
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.time.Instant
import java.math.BigDecimal

/** Replaces an existing weight entry (same id and profile); recomputes the BMI. Returns null when the id is unknown. */
class UpdateWeightUseCase(
    private val healthLogRepository: HealthLogRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) {
    suspend operator fun invoke(
        id: MeasurementId,
        profileId: ProfileId,
        weightKg: BigDecimal,
        timestamp: Instant,
        comment: String? = null
    ): WeightEntry? {
        val weight = WeightKg(weightKg)
        val bmi = profileRepository.getById(profileId)?.calculateBmi(weight)?.bmi
        val entry = WeightEntry(id = id, profileId = profileId, timestamp = timestamp, weight = weight, bmi = bmi, comment = EntryComment.ofOrNull(comment))
        return entry.takeIf { healthLogRepository.updateWeight(it) }
    }
}
