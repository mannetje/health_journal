package nl.healthjournal.domain.testing

import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.port.ProfileRepositoryPort

/** In-memory profiles for the domain and app tests. */
class FakeProfileRepository : ProfileRepositoryPort {
    val profiles = mutableMapOf<ProfileId, Profile>()
    var activeId: ProfileId? = null

    override suspend fun save(profile: Profile) {
        profiles[profile.id] = profile
    }

    override suspend fun getById(id: ProfileId): Profile? = profiles[id]

    override suspend fun getAll(): List<Profile> = profiles.values.toList()

    override suspend fun getActiveProfile(): Profile? = activeId?.let { profiles[it] }

    override suspend fun setActiveProfile(id: ProfileId) {
        activeId = id
    }
}
