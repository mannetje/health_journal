package nl.healthjournal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The person's own minimum and maximum, one row per profile; null means not set. Never holds an app-chosen value. */
@Entity(tableName = "fluid_settings")
data class FluidSettingsEntity(
    @PrimaryKey
    val profileId: String,
    val minMl: Int? = null,
    val maxMl: Int? = null
)
