package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Amounts are stored as plain decimal text so 2.5 stays 2.5 (no floating point), dates as ISO-8601 text
 * (`2026-05-04`), planned times as local ISO date-times without zone (`2026-05-04T08:00`).
 */
@Entity(
    tableName = "medications",
    indices = [Index("profileId")]
)
data class MedicationEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val form: String?,
    val strengthAmount: String?,
    val strengthUnit: String?,
    val doseAmount: String,
    val doseUnit: String,
    val color: String?,
    val shape: String?,
    val comment: String?,
    val archivedFrom: String?
)

/** One row per schedule version of a medication. */
@Entity(
    tableName = "medication_schedules",
    primaryKeys = ["medicationId", "effectiveFrom"],
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicationScheduleEntity(
    val medicationId: String,
    val effectiveFrom: String,
    val asNeeded: Boolean,
    /** Pipe-separated MON..SUN, null when an interval is used or the schedule is as needed. */
    val daysOfWeek: String?,
    val intervalDays: Int?,
    val startDate: String?,
    val endDate: String?
)

/** One row per daily time of a schedule version. */
@Entity(
    tableName = "medication_times",
    primaryKeys = ["medicationId", "effectiveFrom", "localTime"],
    foreignKeys = [
        ForeignKey(
            entity = MedicationScheduleEntity::class,
            parentColumns = ["medicationId", "effectiveFrom"],
            childColumns = ["medicationId", "effectiveFrom"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicationTimeEntity(
    val medicationId: String,
    val effectiveFrom: String,
    /** `HH:mm` */
    val localTime: String
)

/** A recorded outcome. Unique per (medication, planned time); as-needed doses have a null planned time. */
@Entity(
    tableName = "intakes",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicationId", "planned"], unique = true),
        Index(value = ["medicationId", "takenAt"])
    ]
)
data class IntakeEntity(
    @PrimaryKey
    val id: String,
    val medicationId: String,
    val planned: String?,
    val status: String,
    /** Epoch milliseconds, null for a skipped intake. */
    val takenAt: Long?,
    val actualAmount: String?,
    val comment: String?
)
