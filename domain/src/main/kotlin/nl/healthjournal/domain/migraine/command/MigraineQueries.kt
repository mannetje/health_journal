package nl.healthjournal.domain.migraine.command

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.ObserveQuery
import nl.healthjournal.domain.shared.command.ObserveQueryHandler
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.migraine.model.MigraineEntry
import nl.healthjournal.domain.migraine.model.MigraineTag
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort

/** The migraines of a profile, newest changes included; emits again when one is saved, changed or deleted. */
data class ObserveMigraineHistoryQuery(val profileId: ProfileId) : ObserveQuery<List<MigraineEntry>>

/** The migraine tags of a profile (archived ones too); emits again when a tag changes. */
data class ObserveMigraineTagsQuery(val profileId: ProfileId) : ObserveQuery<List<MigraineTag>>

class ObserveMigraineHistoryHandler(
    private val repository: MigraineRepositoryPort
) : ObserveQueryHandler<ObserveMigraineHistoryQuery, List<MigraineEntry>> {
    override fun observe(query: ObserveMigraineHistoryQuery): Flow<DomainResult<List<MigraineEntry>>> =
        repository.observeMigraineHistory(query.profileId).map { DomainResult.success(it) }
}

class ObserveMigraineTagsHandler(
    private val repository: MigraineRepositoryPort
) : ObserveQueryHandler<ObserveMigraineTagsQuery, List<MigraineTag>> {
    override fun observe(query: ObserveMigraineTagsQuery): Flow<DomainResult<List<MigraineTag>>> =
        repository.observeMigraineTags(query.profileId).map { DomainResult.success(it) }
}
