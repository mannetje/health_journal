package nl.healthjournal.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.healthjournal.data.local.dao.MigraineDao
import nl.healthjournal.data.local.mapper.MigraineMapper
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort

class RoomMigraineRepository(
    private val dao: MigraineDao,
    private val clock: () -> Long = System::currentTimeMillis
) : MigraineRepositoryPort {

    override suspend fun saveMigraine(entry: MigraineEntry) {
        dao.insert(MigraineMapper.toEntity(entry))
    }

    override suspend fun getMigraine(id: MeasurementId): MigraineEntry? =
        dao.getById(id.value.toString())?.let(MigraineMapper::toDomain)

    override suspend fun getMigraineHistory(profileId: ProfileId): List<MigraineEntry> =
        dao.getByProfileId(profileId.value.toString()).map(MigraineMapper::toDomain)

    override fun observeMigraineHistory(profileId: ProfileId): Flow<List<MigraineEntry>> =
        dao.observeByProfileId(profileId.value.toString()).map { list -> list.map(MigraineMapper::toDomain) }

    override suspend fun updateMigraine(entry: MigraineEntry): Boolean =
        dao.update(MigraineMapper.toEntity(entry)) > 0

    override suspend fun deleteMigraine(id: MeasurementId): Boolean =
        dao.deleteById(id.value.toString()) > 0

    override suspend fun saveMigraineTag(tag: MigraineTag) {
        dao.insertTag(MigraineMapper.toEntity(tag, clock()))
    }

    override suspend fun getMigraineTag(id: MigraineTagId): MigraineTag? =
        dao.getTag(id.toString())?.let(MigraineMapper::toDomain)

    override suspend fun getMigraineTags(profileId: ProfileId): List<MigraineTag> =
        dao.getTags(profileId.value.toString()).mapNotNull(MigraineMapper::toDomain)

    override fun observeMigraineTags(profileId: ProfileId): Flow<List<MigraineTag>> =
        dao.observeTags(profileId.value.toString()).map { list -> list.mapNotNull(MigraineMapper::toDomain) }

    override suspend fun updateMigraineTag(tag: MigraineTag): Boolean =
        dao.updateTag(tag.id.toString(), tag.label, tag.archived) > 0
}
