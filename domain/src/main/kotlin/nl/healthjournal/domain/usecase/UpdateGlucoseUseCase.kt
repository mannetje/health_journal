package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.time.Instant
import java.math.BigDecimal

/** Replaces an existing glucose entry (same id and profile). Returns null when the id is unknown. */
class UpdateGlucoseUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        id: MeasurementId,
        profileId: ProfileId,
        context: GlucoseContext,
        valueInMmolL: BigDecimal,
        timestamp: Instant
    ): GlucoseEntry? {
        val level = GlucoseLevel(valueInMmolL)
        val entry = GlucoseEntry(
            id = id,
            profileId = profileId,
            timestamp = timestamp,
            glucose = level,
            context = context,
            category = NhgGlucoseCategory.classify(level, context)
        )
        return entry.takeIf { healthLogRepository.updateGlucose(it) }
    }
}
