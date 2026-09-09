package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import java.time.LocalDate

class CreateProfileUseCase(
    private val profileRepository: ProfileRepositoryPort
) {
    suspend operator fun invoke(
        name: String,
        dateOfBirth: LocalDate,
        heightCm: Int? = null
    ): ProfileId {
        val height = heightCm?.let { HeightCm(it) }
        val profile = Profile.create(
            name = name,
            dateOfBirth = dateOfBirth,
            height = height
        )

        profileRepository.save(profile)

        val active = profileRepository.getActiveProfile()
        if (active == null) {
            profileRepository.setActiveProfile(profile.id)
        }

        return profile.id
    }
}
