package nl.healthjournal.app.reminder

/** What the pillbox tells the reminders when data changes. A no-op in tests and previews. */
interface ReminderHooks {
    /** A schedule, archive, delete or outcome changed: re-arm the alarm and refresh shown notifications. */
    suspend fun changed()

    companion object {
        val None: ReminderHooks = object : ReminderHooks {
            override suspend fun changed() = Unit
        }
    }
}
