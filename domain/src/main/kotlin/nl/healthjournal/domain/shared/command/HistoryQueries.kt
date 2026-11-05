package nl.healthjournal.domain.shared.command

import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.migraine.model.MigraineEntry
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort
import nl.healthjournal.domain.metrics.usecase.GetHealthHistoryUseCase
import nl.healthjournal.domain.metrics.usecase.HealthHistory

/** Everything the History screen lists for one profile: the six metrics and the migraines. */
data class HistorySnapshot(val health: HealthHistory, val migraines: List<MigraineEntry>)

data class GetHistoryQuery(val profileId: ProfileId) : Query<HistorySnapshot>

class GetHistoryHandler(
    private val health: GetHealthHistoryUseCase,
    private val migraines: MigraineRepositoryPort
) : QueryHandler<GetHistoryQuery, HistorySnapshot> {
    override suspend fun handle(query: GetHistoryQuery): DomainResult<HistorySnapshot> = domainCatching {
        HistorySnapshot(health(query.profileId), migraines.getMigraineHistory(query.profileId))
    }
}
