package nl.healthjournal.domain.metrics.port

import kotlinx.coroutines.flow.Flow
import nl.healthjournal.domain.metrics.model.FluidIntakeEntry
import nl.healthjournal.domain.metrics.model.FluidLimits
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId

/** Storage of fluid intake entries and of the person's own minimum and maximum. One port per metric. */
interface FluidRepositoryPort {
    suspend fun saveFluidIntake(entry: FluidIntakeEntry)
    suspend fun getFluidHistory(profileId: ProfileId): List<FluidIntakeEntry>
    fun observeFluidHistory(profileId: ProfileId): Flow<List<FluidIntakeEntry>>
    /** Replaces an existing entry; returns false (and changes nothing) when the id is unknown. */
    suspend fun updateFluidIntake(entry: FluidIntakeEntry): Boolean
    /** Deletes one entry by id; returns false when the id is unknown. */
    suspend fun deleteFluidIntake(id: MeasurementId): Boolean

    suspend fun getFluidLimits(profileId: ProfileId): FluidLimits
    fun observeFluidLimits(profileId: ProfileId): Flow<FluidLimits>
    /** Stores both numbers for the profile; null clears one. Never touches entries. */
    suspend fun saveFluidLimits(profileId: ProfileId, limits: FluidLimits)
}
