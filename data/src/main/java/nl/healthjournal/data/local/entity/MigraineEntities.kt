package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One migraine. Times are epoch millis; `endTime` is null while ongoing. The set columns (`areas`, `painCharacters`,
 * `symptoms`, `triggers`, `reliefMethods`) hold comma-separated stable code names, and
 * `customTriggers` and `customReliefMethods` hold comma-separated tag ids.
 */
@Entity(
    tableName = "migraines",
    indices = [Index("profileId", "startTime")]
)
data class MigraineEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val startTime: Long,
    val wokeUpWithIt: Boolean,
    val endTime: Long?,
    val intensity: Int?,
    val areas: String,
    val painCharacters: String,
    val type: String?,
    val symptoms: String,
    val triggers: String,
    val customTriggers: String,
    val tookSomething: Boolean,
    val medicationText: String?,
    val secondDose: Boolean,
    val intensityAfter2Hours: Int?,
    val medicationHelpedMe: Boolean,
    val reliefMethods: String,
    val customReliefMethods: String,
    val reliefHelpedMe: Boolean,
    val activities: String?,
    val comment: String?
)

/** A personal trigger or relief label. Never hard-deleted: removing sets `archived`. */
@Entity(
    tableName = "migraine_tags",
    indices = [Index("profileId", "kind")]
)
data class MigraineTagEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val kind: String,
    val label: String,
    val archived: Boolean,
    val createdAt: Long
)
