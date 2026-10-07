package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.NextSlot
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.ReminderSchedulerPort
import java.time.Clock
import java.time.LocalDateTime

/**
 * Sets the single alarm for the next slot of the profile, or cancels it when nothing is planned or there is no
 * active profile. Called after boot, app update, a time change and any schedule, archive, delete or profile change.
 */
class RearmRemindersUseCase(
    private val medicationRepository: MedicationRepositoryPort,
    private val scheduler: ReminderSchedulerPort,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    /** Returns the slot that was armed, or null when the alarm was cancelled. */
    suspend operator fun invoke(profileId: ProfileId?): LocalDateTime? {
        // Archived medications are included: they still remind until their archive date, which plannedFor applies.
        val next = profileId?.let {
            NextSlot.after(medicationRepository.getMedications(it, includeArchived = true), LocalDateTime.now(clock))
        }
        if (next == null) scheduler.cancel() else scheduler.arm(next)
        return next
    }
}
