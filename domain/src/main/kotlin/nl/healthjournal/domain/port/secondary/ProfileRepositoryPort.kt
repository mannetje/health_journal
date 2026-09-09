package nl.healthjournal.domain.port.secondary

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.profile.Profile

interface ProfileRepositoryPort {
    suspend fun save(profile: Profile)
    suspend fun getById(id: ProfileId): Profile?
    suspend fun getAll(): List<Profile>
    suspend fun getActiveProfile(): Profile?
    suspend fun setActiveProfile(id: ProfileId)
}
