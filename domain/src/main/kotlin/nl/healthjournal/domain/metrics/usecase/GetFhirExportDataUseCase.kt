package nl.healthjournal.domain.metrics.usecase

import nl.healthjournal.domain.metrics.model.FhirExportData
import nl.healthjournal.domain.metrics.model.FhirExportMetric
import nl.healthjournal.domain.metrics.port.FhirExportSourcePort
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Instant

/**
 * Use case that reads entries from the port, selects the requested range and metrics,
 * and sorts entries oldest first.
 */
class GetFhirExportDataUseCase(
    private val port: FhirExportSourcePort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        from: Instant? = null,
        to: Instant? = null,
        metrics: Set<FhirExportMetric> = setOf(FhirExportMetric.WEIGHT, FhirExportMetric.BLOOD_PRESSURE)
    ): FhirExportData {
        val profile = port.getProfile(profileId)

        val weights = if (FhirExportMetric.WEIGHT in metrics) {
            port.getWeights(profileId)
                .filter { entry ->
                    (from == null || entry.timestamp >= from) &&
                    (to == null || entry.timestamp <= to)
                }
                .sortedBy { it.timestamp }
        } else {
            emptyList()
        }

        val bloodPressures = if (FhirExportMetric.BLOOD_PRESSURE in metrics) {
            port.getBloodPressures(profileId)
                .filter { entry ->
                    (from == null || entry.timestamp >= from) &&
                    (to == null || entry.timestamp <= to)
                }
                .sortedBy { it.timestamp }
        } else {
            emptyList()
        }

        return FhirExportData(
            profile = profile,
            weights = weights,
            bloodPressures = bloodPressures
        )
    }
}
