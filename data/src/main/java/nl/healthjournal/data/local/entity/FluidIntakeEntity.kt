package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** The drink is a stable code; a missing or unknown code reads as Other (see `HealthLogMapper`). */
@Entity(
    tableName = "fluid_intakes",
    indices = [Index(value = ["profileId", "timestamp"])]
)
data class FluidIntakeEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val timestamp: Long,
    val ml: Int,
    val drink: String? = null,
    val comment: String? = null
)
