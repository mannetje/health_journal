package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.Slot
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/**
 * "Taken all" for the slot at a given time: records taken for the intakes that still have no outcome. The slot is
 * rebuilt from the current data, so a medication archived or already marked since the alarm was set is left out,
 * and an outcome that exists (for example skipped) is never overwritten.
 */
class TakeAllForSlotUseCase(
    private val getPillboxDay: GetPillboxDayUseCase,
    private val recordSlotIntakes: RecordSlotIntakesUseCase,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    /** The outcomes that were recorded; empty when nothing was open. */
    suspend operator fun invoke(profileId: ProfileId, slotTime: LocalDateTime): List<Intake> {
        val open = slotAt(profileId, slotTime)?.openItems.orEmpty()
        if (open.isEmpty()) return emptyList()
        return recordSlotIntakes(open.map { it.medication.id }, slotTime, Instant.now(clock))
    }

    /** The slot at [slotTime] as it is now, or null when no medication is planned then. */
    suspend fun slotAt(profileId: ProfileId, slotTime: LocalDateTime): Slot? =
        getPillboxDay(profileId, slotTime.toLocalDate()).slots.firstOrNull { it.time == slotTime }
}
