package nl.healthjournal.data.local.mapper

import nl.healthjournal.data.local.entity.ProfileEntity
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.profile.Profile
import java.time.LocalDate

object ProfileMapper {
    fun toEntity(domain: Profile, isActive: Boolean = false): ProfileEntity {
        return ProfileEntity(
            id = domain.id.value.toString(),
            name = domain.name,
            dateOfBirth = domain.dateOfBirth.toString(),
            heightCm = domain.height?.value,
            isActive = isActive
        )
    }

    fun toDomain(entity: ProfileEntity): Profile {
        return Profile.reconstruct(
            id = ProfileId.fromString(entity.id),
            name = entity.name,
            dateOfBirth = LocalDate.parse(entity.dateOfBirth),
            height = entity.heightCm?.let { HeightCm(it) }
        )
    }
}
