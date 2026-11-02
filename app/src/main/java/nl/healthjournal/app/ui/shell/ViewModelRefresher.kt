package nl.healthjournal.app.ui.shell

import nl.healthjournal.app.AppNavDestination
import nl.healthjournal.app.reminder.ReminderHooks
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.migraine.MigraineLogViewModel
import nl.healthjournal.app.ui.profile.ProfileViewModel

/**
 * Which ViewModels clear or reload when the person moves around or a restore finishes. Holds no Activity; the
 * ViewModels are reached through small functions so the rules run in a plain JVM test.
 */
class ViewModelRefresher(
    private val clearMessages: () -> Unit,
    private val clearMedicationMessages: () -> Unit,
    private val loadMigraineProfile: () -> Unit,
    private val reloadData: () -> Unit,
    private val rescheduleReminders: suspend () -> Unit
) {
    private var lastTab: AppNavDestination? = null

    /**
     * Called whenever a tab is shown. Banners belong to the screen that raised them, so they are cleared when the
     * tab changes, but not on the first call, so a banner survives the Activity recreation caused by a language
     * change (ADR 0015). The migraine tab reads the profile each time the Log tab opens, because a profile created
     * on the Profile tab must reach it.
     */
    fun onTabShown(tab: AppNavDestination) {
        if (lastTab != null && tab != lastTab) clearMessages()
        lastTab = tab
        if (tab == AppNavDestination.LOG) loadMigraineProfile()
    }

    fun onPillboxClosed() = clearMedicationMessages()

    /** A restore finished: everything that shows stored data reads it again, and reminders are re-armed. */
    suspend fun onRestored() {
        reloadData()
        rescheduleReminders()
    }

    companion object {
        fun of(
            profile: ProfileViewModel,
            logging: LoggingViewModel,
            migraine: MigraineLogViewModel,
            history: HistoryViewModel,
            medication: MedicationViewModel,
            backup: BackupViewModel,
            reminders: ReminderHooks
        ) = ViewModelRefresher(
            clearMessages = {
                logging.clearMessages()
                migraine.clearMessages()
                profile.clearMessages()
                history.clearMessages()
                medication.clearMessages()
                backup.clearMessages()
            },
            clearMedicationMessages = medication::clearMessages,
            loadMigraineProfile = migraine::loadProfile,
            reloadData = {
                profile.loadProfile()
                logging.loadProfile()
                migraine.loadProfile()
                history.loadHistory()
                medication.refresh()
            },
            rescheduleReminders = reminders::changed
        )
    }
}
