package nl.healthjournal.domain.profile.command

import nl.healthjournal.domain.command.Command
import nl.healthjournal.domain.command.CommandHandler
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.DomainResult
import nl.healthjournal.domain.command.Query
import nl.healthjournal.domain.command.QueryHandler
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.command.ValidationRule
import nl.healthjournal.domain.command.domainCatching
import nl.healthjournal.domain.model.common.HeightCm
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import java.time.Clock
import java.time.LocalDate

/** The profile the app is showing, or null before one exists. */
data object GetActiveProfileQuery : Query<Profile?>

/**
 * Saves the profile form: edits the active profile (same id) or, when there is none, creates one and makes it
 * active. Returns the stored profile.
 */
data class SaveProfileCommand(
    val name: String,
    val dateOfBirth: LocalDate,
    val heightCm: Int? = null,
    val sex: Sex? = null
) : Command<Profile>

class GetActiveProfileHandler(
    private val profiles: ProfileRepositoryPort
) : QueryHandler<GetActiveProfileQuery, Profile?> {
    override suspend fun handle(query: GetActiveProfileQuery): DomainResult<Profile?> =
        domainCatching { profiles.getActiveProfile() }
}

class SaveProfileHandler(
    private val profiles: ProfileRepositoryPort,
    private val clock: Clock
) : CommandHandler<SaveProfileCommand, Profile> {
    override suspend fun handle(command: SaveProfileCommand): DomainResult<Profile> {
        invalid(command)?.let { return DomainResult.failure(it) }
        return domainCatching {
            val height = command.heightCm?.let { HeightCm(it) }
            val existing = profiles.getActiveProfile()
            if (existing != null) {
                // Editing: keep the id so the save replaces the profile instead of adding one.
                Profile.reconstruct(existing.id, command.name, command.dateOfBirth, height, command.sex)
                    .also { profiles.save(it) }
            } else {
                Profile.create(command.name, command.dateOfBirth, height, command.sex).also {
                    profiles.save(it)
                    profiles.setActiveProfile(it.id)
                }
            }
        }
    }

    private fun invalid(command: SaveProfileCommand): DomainError.Invalid? {
        val today = LocalDate.now(clock)
        return when {
            command.name.isBlank() -> DomainError.Invalid(ValidationField.NAME, ValidationRule.REQUIRED)
            command.heightCm != null && command.heightCm !in HeightCm.MIN_HEIGHT_CM..HeightCm.MAX_HEIGHT_CM ->
                DomainError.Invalid(
                    ValidationField.HEIGHT, ValidationRule.OUT_OF_RANGE,
                    listOf(HeightCm.MIN_HEIGHT_CM, HeightCm.MAX_HEIGHT_CM)
                )
            !command.dateOfBirth.isBefore(today) ||
                !command.dateOfBirth.isAfter(today.minusYears(Profile.MAX_AGE_YEARS)) ->
                DomainError.Invalid(ValidationField.DATE_OF_BIRTH, ValidationRule.OUT_OF_RANGE)
            else -> null
        }
    }
}
