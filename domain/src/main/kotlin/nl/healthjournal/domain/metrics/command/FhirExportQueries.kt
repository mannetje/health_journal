package nl.healthjournal.domain.metrics.command

import nl.healthjournal.domain.metrics.model.FhirExportData
import nl.healthjournal.domain.metrics.model.FhirExportMetric
import nl.healthjournal.domain.metrics.usecase.GetFhirExportDataUseCase
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.Query
import nl.healthjournal.domain.shared.command.QueryHandler
import nl.healthjournal.domain.shared.command.domainCatching
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Instant

data class GetFhirExportDataQuery(
    val profileId: ProfileId,
    val from: Instant? = null,
    val to: Instant? = null,
    val metrics: Set<FhirExportMetric> = setOf(FhirExportMetric.WEIGHT, FhirExportMetric.BLOOD_PRESSURE)
) : Query<FhirExportData>

class GetFhirExportDataHandler(
    private val useCase: GetFhirExportDataUseCase
) : QueryHandler<GetFhirExportDataQuery, FhirExportData> {
    override suspend fun handle(query: GetFhirExportDataQuery): DomainResult<FhirExportData> = domainCatching {
        useCase(
            profileId = query.profileId,
            from = query.from,
            to = query.to,
            metrics = query.metrics
        )
    }
}
