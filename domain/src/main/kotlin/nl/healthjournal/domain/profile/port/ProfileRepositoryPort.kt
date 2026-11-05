package nl.healthjournal.domain.profile.port

import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.profile.model.Profile

interface ProfileRepositoryPort {
    suspend fun save(profile: Profile)
    suspend fun getById(id: ProfileId): Profile?
    suspend fun getAll(): List<Profile>
    suspend fun getActiveProfile(): Profile?
    suspend fun setActiveProfile(id: ProfileId)
}
