package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Pillbox
import nl.healthjournal.domain.model.medication.PillboxDay
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** One day of planned and recorded intakes. Pending and missed are derived from [clock], never stored. */
class GetPillboxDayUseCase(
    private val medicationRepository: MedicationRepositoryPort,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend operator fun invoke(profileId: ProfileId, date: LocalDate): PillboxDay {
        val medications = medicationRepository.getMedications(profileId, includeArchived = true)
        val intakes = medicationRepository.getIntakesForDay(profileId, date, clock.zone)
        return Pillbox.dayOf(date, medications, intakes, LocalDateTime.now(clock), clock.zone)
    }
}
