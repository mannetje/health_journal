package nl.healthjournal.app.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What the app knows about the permissions reminders need, and the rule for asking for notifications once. Holds no
 * Activity: the system is read through small functions, and launching the permission request stays with the caller.
 */
class ReminderPermissionState(
    private val readNotificationsAllowed: () -> Boolean,
    private val readExactAlarmsAllowed: () -> Boolean,
    private val askedBefore: () -> Boolean,
    private val markAsked: () -> Unit,
    /** Notifications need a runtime permission from Android 13; before that there is nothing to ask. */
    private val needsRuntimePermission: Boolean
) {
    var notificationsAllowed by mutableStateOf(true)
        private set
    var exactAlarmsAllowed by mutableStateOf(true)
        private set
    var showDialog by mutableStateOf(false)
        private set

    /** Notifications may have been switched elsewhere, so this runs whenever the app returns to the foreground. */
    fun refresh() {
        notificationsAllowed = readNotificationsAllowed()
        exactAlarmsAllowed = readExactAlarmsAllowed()
    }

    /** Ask once, with an explanation, when the first schedule exists and the pillbox is in view. */
    fun onPillboxState(hasMedication: Boolean, pillboxOpen: Boolean) {
        if (hasMedication && pillboxOpen && !askedBefore() && needsRuntimePermission && !notificationsAllowed) {
            showDialog = true
        }
    }

    /** The person chose Allow or Later; either way the question is not asked again. */
    fun answered() {
        showDialog = false
        markAsked()
    }
}
