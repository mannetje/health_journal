package nl.healthjournal.app.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort

/** In-memory [MigraineRepositoryPort] shared by the ViewModel tests. */
class FakeMigraines : MigraineRepositoryPort {
    val entries = MutableStateFlow<List<MigraineEntry>>(emptyList())
    val tags = MutableStateFlow<List<MigraineTag>>(emptyList())

    override suspend fun saveMigraine(entry: MigraineEntry) { entries.value = entries.value + entry }
    override suspend fun getMigraine(id: MeasurementId) = entries.value.firstOrNull { it.id == id }
    override suspend fun getMigraineHistory(profileId: ProfileId) = entries.value.filter { it.profileId == profileId }
    override fun observeMigraineHistory(profileId: ProfileId): Flow<List<MigraineEntry>> =
        entries.map { list -> list.filter { it.profileId == profileId } }
    override suspend fun updateMigraine(entry: MigraineEntry): Boolean {
        if (entries.value.none { it.id == entry.id }) return false
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
        return true
    }
    override suspend fun deleteMigraine(id: MeasurementId) = entries.value.any { it.id == id }.also {
        entries.value = entries.value.filter { e -> e.id != id }
    }
    override suspend fun saveMigraineTag(tag: MigraineTag) { tags.value = tags.value + tag }
    override suspend fun getMigraineTag(id: MigraineTagId) = tags.value.firstOrNull { it.id == id }
    override suspend fun getMigraineTags(profileId: ProfileId) = tags.value.filter { it.profileId == profileId }
    override fun observeMigraineTags(profileId: ProfileId): Flow<List<MigraineTag>> =
        tags.map { list -> list.filter { it.profileId == profileId } }
    override suspend fun updateMigraineTag(tag: MigraineTag): Boolean {
        if (tags.value.none { it.id == tag.id }) return false
        tags.value = tags.value.map { if (it.id == tag.id) tag else it }
        return true
    }
}
