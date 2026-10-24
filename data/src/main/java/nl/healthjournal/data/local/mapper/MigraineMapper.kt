package nl.healthjournal.data.local.mapper

import nl.healthjournal.data.local.entity.MigraineEntity
import nl.healthjournal.data.local.entity.MigraineTagEntity
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineTrigger
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import java.time.Instant

object MigraineMapper {
    fun toEntity(entry: MigraineEntry) = MigraineEntity(
        id = entry.id.value.toString(),
        profileId = entry.profileId.value.toString(),
        startTime = entry.start.toEpochMilli(),
        wokeUpWithIt = entry.wokeUpWithIt,
        endTime = entry.end?.toEpochMilli(),
        intensity = entry.intensity,
        areas = codes(entry.areas),
        painCharacters = codes(entry.painCharacters),
        type = entry.type?.name,
        symptoms = codes(entry.symptoms),
        triggers = codes(entry.triggers),
        customTriggers = ids(entry.customTriggerIds),
        tookSomething = entry.tookSomething,
        medicationText = entry.medicationText,
        secondDose = entry.secondDose,
        intensityAfter2Hours = entry.intensityAfter2Hours,
        medicationHelpedMe = entry.medicationHelpedMe,
        reliefMethods = codes(entry.reliefMethods),
        customReliefMethods = ids(entry.customReliefIds),
        reliefHelpedMe = entry.reliefHelpedMe,
        activities = entry.activities?.name,
        comment = entry.comment?.text
    )

    fun toDomain(e: MigraineEntity) = MigraineEntry(
        id = MeasurementId.fromString(e.id),
        profileId = ProfileId.fromString(e.profileId),
        start = Instant.ofEpochMilli(e.startTime),
        wokeUpWithIt = e.wokeUpWithIt,
        end = e.endTime?.let(Instant::ofEpochMilli),
        intensity = e.intensity,
        areas = enums<HeadArea>(e.areas),
        painCharacters = enums<PainCharacter>(e.painCharacters),
        type = enumOrNull<MigraineType>(e.type),
        symptoms = enums<MigraineSymptom>(e.symptoms),
        triggers = enums<MigraineTrigger>(e.triggers),
        customTriggerIds = tagIds(e.customTriggers),
        tookSomething = e.tookSomething,
        medicationText = e.medicationText,
        secondDose = e.secondDose,
        intensityAfter2Hours = e.intensityAfter2Hours,
        medicationHelpedMe = e.medicationHelpedMe,
        reliefMethods = enums<ReliefMethod>(e.reliefMethods),
        customReliefIds = tagIds(e.customReliefMethods),
        reliefHelpedMe = e.reliefHelpedMe,
        activities = enumOrNull<DailyActivities>(e.activities),
        comment = EntryComment.ofOrNull(e.comment)
    )

    fun toEntity(tag: MigraineTag, createdAt: Long) = MigraineTagEntity(
        id = tag.id.toString(),
        profileId = tag.profileId.value.toString(),
        kind = tag.kind.name,
        label = tag.label,
        archived = tag.archived,
        createdAt = createdAt
    )

    /** Null when the stored kind is unknown (a row written by a newer version). */
    fun toDomain(e: MigraineTagEntity): MigraineTag? {
        val kind = enumOrNull<MigraineTagKind>(e.kind) ?: return null
        return MigraineTag(MigraineTagId.fromString(e.id), ProfileId.fromString(e.profileId), kind, e.label, e.archived)
    }

    private fun codes(values: Set<Enum<*>>): String = values.map { it.name }.sorted().joinToString(",")

    private fun ids(values: Set<MigraineTagId>): String = values.map { it.toString() }.sorted().joinToString(",")

    private fun parts(raw: String): List<String> = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** Unknown codes (from a newer version) are ignored on read. */
    private inline fun <reified E : Enum<E>> enums(raw: String): Set<E> =
        parts(raw).mapNotNull { name -> enumValues<E>().firstOrNull { it.name == name } }.toSet()

    private inline fun <reified E : Enum<E>> enumOrNull(raw: String?): E? =
        raw?.let { name -> enumValues<E>().firstOrNull { it.name == name } }

    /** An id that is not a UUID is ignored; an id of a tag that no longer resolves is dropped by the readers. */
    private fun tagIds(raw: String): Set<MigraineTagId> =
        parts(raw).mapNotNull { runCatching { MigraineTagId.fromString(it) }.getOrNull() }.toSet()
}
