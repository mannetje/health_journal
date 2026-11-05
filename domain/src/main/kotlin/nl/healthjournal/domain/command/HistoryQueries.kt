package nl.healthjournal.domain.command

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.usecase.GetHealthHistoryUseCase
import nl.healthjournal.domain.usecase.HealthHistory

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
