package nl.healthjournal.app.reminder

/** Intent actions and extras shared by the alarm, the notification and the receivers. */
object ReminderIntents {
    const val ACTION_ALARM = "nl.healthjournal.app.reminder.ALARM"
    const val ACTION_TAKE_ALL = "nl.healthjournal.app.reminder.TAKE_ALL"
    const val ACTION_SNOOZE = "nl.healthjournal.app.reminder.SNOOZE"

    /** The planned local time of the slot, as an ISO date-time string. */
    const val EXTRA_SLOT = "nl.healthjournal.app.reminder.SLOT"

    /** Set on the Activity intent when the notification is tapped. */
    const val EXTRA_OPEN_PILLBOX = "nl.healthjournal.app.reminder.OPEN_PILLBOX"
}
