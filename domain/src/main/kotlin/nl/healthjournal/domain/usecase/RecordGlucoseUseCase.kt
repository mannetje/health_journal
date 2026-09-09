package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.math.BigDecimal
import java.time.Instant

class RecordGlucoseUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        context: GlucoseContext,
        valueInMmolL: BigDecimal? = null,
        valueInMgDl: BigDecimal? = null,
        timestamp: Instant = Instant.now(),
        measurementId: MeasurementId = MeasurementId.generate()
    ): GlucoseEntry {
        require(valueInMmolL != null || valueInMgDl != null) {
            "Either valueInMmolL or valueInMgDl must be provided"
        }

        val level = if (valueInMmolL != null) {
            GlucoseLevel(valueInMmolL)
        } else {
            GlucoseLevel.fromMgDl(valueInMgDl!!)
        }

        val category = NhgGlucoseCategory.classify(level, context)

        val entry = GlucoseEntry(
            id = measurementId,
            profileId = profileId,
            timestamp = timestamp,
            glucose = level,
            context = context,
            category = category
        )

        healthLogRepository.saveGlucose(entry)
        return entry
    }
}
