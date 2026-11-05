package nl.healthjournal.domain.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.migraine.model.MigraineEntry
import nl.healthjournal.domain.migraine.model.MigraineTag
import nl.healthjournal.domain.migraine.model.MigraineTagId
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort

/** In-memory migraines and tags; the observers emit once. */
class FakeMigraineRepository : MigraineRepositoryPort {
    val migraines = mutableListOf<MigraineEntry>()
    val tags = mutableListOf<MigraineTag>()

    override suspend fun saveMigraine(entry: MigraineEntry) { migraines.add(entry) }
    override suspend fun getMigraine(id: MeasurementId): MigraineEntry? = migraines.firstOrNull { it.id == id }
    override suspend fun getMigraineHistory(profileId: ProfileId): List<MigraineEntry> =
        migraines.filter { it.profileId == profileId }
    override fun observeMigraineHistory(profileId: ProfileId): Flow<List<MigraineEntry>> =
        flow { emit(getMigraineHistory(profileId)) }
    override suspend fun updateMigraine(entry: MigraineEntry): Boolean {
        val i = migraines.indexOfFirst { it.id == entry.id }
        if (i < 0) return false
        migraines[i] = entry
        return true
    }
    override suspend fun deleteMigraine(id: MeasurementId): Boolean = migraines.removeAll { it.id == id }

    override suspend fun saveMigraineTag(tag: MigraineTag) { tags.add(tag) }
    override suspend fun getMigraineTag(id: MigraineTagId): MigraineTag? = tags.firstOrNull { it.id == id }
    override suspend fun getMigraineTags(profileId: ProfileId): List<MigraineTag> = tags.filter { it.profileId == profileId }
    override fun observeMigraineTags(profileId: ProfileId): Flow<List<MigraineTag>> =
        flow { emit(getMigraineTags(profileId)) }
    override suspend fun updateMigraineTag(tag: MigraineTag): Boolean {
        val i = tags.indexOfFirst { it.id == tag.id }
        if (i < 0) return false
        tags[i] = tag
        return true
    }
}
