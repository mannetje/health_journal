package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import java.time.Instant

class RecordMigraineUseCase(private val repository: MigraineRepositoryPort) {
    suspend operator fun invoke(entry: MigraineEntry): MigraineEntry {
        repository.saveMigraine(entry)
        return entry
    }
}

/** Replaces an existing migraine (same id and profile). Returns null when the id is unknown. */
class UpdateMigraineUseCase(private val repository: MigraineRepositoryPort) {
    suspend operator fun invoke(entry: MigraineEntry): MigraineEntry? =
        entry.takeIf { repository.updateMigraine(it) }
}

class DeleteMigraineUseCase(private val repository: MigraineRepositoryPort) {
    suspend operator fun invoke(id: MeasurementId): Boolean = repository.deleteMigraine(id)
}

/** Sets the end time of an ongoing migraine. Null when the id is unknown, it already has an end, or [end] is before the start. */
class EndMigraineUseCase(private val repository: MigraineRepositoryPort) {
    suspend operator fun invoke(id: MeasurementId, end: Instant = Instant.now()): MigraineEntry? {
        val entry = repository.getMigraine(id) ?: return null
        if (!entry.ongoing || end.isBefore(entry.start)) return null
        val ended = entry.copy(end = end)
        return ended.takeIf { repository.updateMigraine(it) }
    }
}

sealed interface AddTagResult {
    /** The tag now exists: new, or the existing tag with the same label (ignoring case). */
    data class Added(val tag: MigraineTag, val alreadyExisted: Boolean) : AddTagResult
    /** The person already has [MigraineTag.MAX_ACTIVE_PER_KIND] active tags of this kind. */
    data object LimitReached : AddTagResult
}

class AddMigraineTagUseCase(private val repository: MigraineRepositoryPort) {
    suspend operator fun invoke(profileId: ProfileId, kind: MigraineTagKind, rawLabel: String): AddTagResult {
        val label = MigraineTag.normalizeLabel(rawLabel)
        val ofKind = repository.getMigraineTags(profileId).filter { it.kind == kind }
        val existing = ofKind.firstOrNull { MigraineTag.sameLabel(it.label, label) }
        if (existing != null) {
            // A duplicate returns the existing tag; an archived one is offered again.
            if (!existing.archived) return AddTagResult.Added(existing, alreadyExisted = true)
            if (ofKind.count { !it.archived } >= MigraineTag.MAX_ACTIVE_PER_KIND) return AddTagResult.LimitReached
            val restored = existing.copy(archived = false)
            repository.updateMigraineTag(restored)
            return AddTagResult.Added(restored, alreadyExisted = true)
        }
        if (ofKind.count { !it.archived } >= MigraineTag.MAX_ACTIVE_PER_KIND) return AddTagResult.LimitReached
        val tag = MigraineTag(MigraineTagId.generate(), profileId, kind, label)
        repository.saveMigraineTag(tag)
        return AddTagResult.Added(tag, alreadyExisted = false)
    }
}

sealed interface RenameTagResult {
    data class Renamed(val tag: MigraineTag) : RenameTagResult
    data object UnknownTag : RenameTagResult
    /** Another tag of the same kind already has this label. */
    data object LabelTaken : RenameTagResult
}

class RenameMigraineTagUseCase(private val repository: MigraineRepositoryPort) {
    suspend operator fun invoke(id: MigraineTagId, rawLabel: String): RenameTagResult {
        val tag = repository.getMigraineTag(id) ?: return RenameTagResult.UnknownTag
        val label = MigraineTag.normalizeLabel(rawLabel)
        val taken = repository.getMigraineTags(tag.profileId)
            .any { it.id != id && it.kind == tag.kind && MigraineTag.sameLabel(it.label, label) }
        if (taken) return RenameTagResult.LabelTaken
        val renamed = tag.copy(label = label)
        return if (repository.updateMigraineTag(renamed)) RenameTagResult.Renamed(renamed) else RenameTagResult.UnknownTag
    }
}

/** Archiving hides a tag from the picker; entries that used it keep it. Returns false when the id is unknown. */
class ArchiveMigraineTagUseCase(private val repository: MigraineRepositoryPort) {
    suspend operator fun invoke(id: MigraineTagId): Boolean {
        val tag = repository.getMigraineTag(id) ?: return false
        return repository.updateMigraineTag(tag.copy(archived = true))
    }
}
