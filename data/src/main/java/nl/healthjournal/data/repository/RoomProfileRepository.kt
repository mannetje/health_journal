package nl.healthjournal.data.repository

import nl.healthjournal.data.local.dao.ProfileDao
import nl.healthjournal.data.local.mapper.ProfileMapper
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort

class RoomProfileRepository(
    private val profileDao: ProfileDao
) : ProfileRepositoryPort {

    override suspend fun save(profile: Profile) {
        val existing = profileDao.getById(profile.id.value.toString())
        val isActive = existing?.isActive ?: (profileDao.getActive() == null)
        profileDao.insert(ProfileMapper.toEntity(profile, isActive = isActive))
    }

    override suspend fun getById(id: ProfileId): Profile? {
        return profileDao.getById(id.value.toString())?.let { ProfileMapper.toDomain(it) }
    }

    override suspend fun getAll(): List<Profile> {
        return profileDao.getAll().map { ProfileMapper.toDomain(it) }
    }

    override suspend fun getActiveProfile(): Profile? {
        return profileDao.getActive()?.let { ProfileMapper.toDomain(it) }
    }

    override suspend fun setActiveProfile(id: ProfileId) {
        profileDao.setActiveProfile(id.value.toString())
    }
}
