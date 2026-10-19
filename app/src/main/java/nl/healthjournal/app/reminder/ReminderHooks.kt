package nl.healthjournal.app.reminder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** What the pillbox tells the reminders when data changes. A no-op in tests and previews. */
interface ReminderHooks {
    /** A schedule, archive, delete or outcome changed: re-arm the alarm and refresh shown notifications. */
    suspend fun changed()

    /** Emits when outcomes were recorded outside the pillbox (Taken all in a notification), so an open pillbox can reload. */
    val outcomesRecorded: Flow<Unit> get() = emptyFlow()

    companion object {
        val None: ReminderHooks = object : ReminderHooks {
            override suspend fun changed() = Unit
        }
    }
}
