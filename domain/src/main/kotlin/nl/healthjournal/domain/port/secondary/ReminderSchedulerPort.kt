package nl.healthjournal.domain.port.secondary

import java.time.LocalDateTime

/** Sets the one alarm for the next reminder. Implemented by the platform (AlarmManager on Android). */
interface ReminderSchedulerPort {
    /** Replaces any earlier alarm with one for the slot at [slot] (local wall-clock time). */
    fun arm(slot: LocalDateTime)

    /** Removes the alarm; nothing is planned. */
    fun cancel()
}
