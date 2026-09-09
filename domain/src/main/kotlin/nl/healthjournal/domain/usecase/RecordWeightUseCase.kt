package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.metrics.WeightKg
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import java.math.BigDecimal
import java.time.Instant

class RecordWeightUseCase(
    private val healthLogRepository: HealthLogRepositoryPort,
    private val profileRepository: ProfileRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        weightKg: BigDecimal,
        timestamp: Instant = Instant.now(),
        measurementId: MeasurementId = MeasurementId.generate()
    ): WeightEntry {
        val weight = WeightKg(weightKg)
        val profile = profileRepository.getById(profileId)

        val bmi = profile?.calculateBmi(weight)?.bmi

        val entry = WeightEntry(
            id = measurementId,
            profileId = profileId,
            timestamp = timestamp,
            weight = weight,
            bmi = bmi
        )

        healthLogRepository.saveWeight(entry)
        return entry
    }
}
