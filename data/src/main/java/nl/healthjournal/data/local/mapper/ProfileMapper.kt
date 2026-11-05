package nl.healthjournal.data.local.mapper

import nl.healthjournal.data.local.entity.ProfileEntity
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.shared.model.HeightCm
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.model.Sex
import java.time.LocalDate

object ProfileMapper {
    fun toEntity(domain: Profile, isActive: Boolean = false): ProfileEntity {
        return ProfileEntity(
            id = domain.id.value.toString(),
            name = domain.name,
            dateOfBirth = domain.dateOfBirth.toString(),
            heightCm = domain.height?.value,
            sex = domain.sex?.name,
            isActive = isActive
        )
    }

    fun toDomain(entity: ProfileEntity): Profile {
        return Profile.reconstruct(
            id = ProfileId.fromString(entity.id),
            name = entity.name,
            dateOfBirth = LocalDate.parse(entity.dateOfBirth),
            height = entity.heightCm?.let { HeightCm(it) },
            sex = entity.sex?.let { Sex.valueOf(it) }
        )
    }
}
