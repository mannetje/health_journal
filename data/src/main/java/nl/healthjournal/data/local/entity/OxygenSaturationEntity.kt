package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Context and site are stored as stable codes; an unknown code reads as none (see `HealthLogMapper`). */
@Entity(
    tableName = "oxygen_saturations",
    indices = [Index(value = ["profileId", "timestamp"])]
)
data class OxygenSaturationEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val timestamp: Long,
    val percent: Int,
    val pulse: Int? = null,
    val context: String? = null,
    val site: String? = null,
    val oxygenGiven: Boolean = false,
    val comment: String? = null
)
