package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Adherence
import nl.healthjournal.domain.model.medication.AdherenceReport
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import java.time.Clock
import java.time.LocalDateTime

/** Adherence over the last [days] days (7, 30 or 90), derived from schedule versions and recorded outcomes. */
class GetAdherenceUseCase(
    private val medicationRepository: MedicationRepositoryPort,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend operator fun invoke(profileId: ProfileId, days: Int): AdherenceReport {
        require(days in Adherence.RANGES_IN_DAYS) { "The range must be one of ${Adherence.RANGES_IN_DAYS}, got: $days" }
        val medications = medicationRepository.getMedications(profileId, includeArchived = true)
        val intakes = medicationRepository.getAllIntakes(profileId)
        return Adherence.report(medications, intakes, days, LocalDateTime.now(clock), clock.zone)
    }
}
