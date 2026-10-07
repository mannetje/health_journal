package nl.healthjournal.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.healthjournal.app.HealthJournalApp
import java.time.LocalDateTime

/** Runs the work off the main thread and keeps the broadcast alive until it is done (`goAsync`). */
abstract class AsyncReceiver : BroadcastReceiver() {
    protected abstract suspend fun handle(reminders: ReminderCoordinator, intent: Intent)

    final override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? HealthJournalApp ?: return
        val pending = goAsync()
        scope.launch {
            try {
                handle(app.reminders, intent)
            } catch (_: Exception) {
                // Not logged: a failed reminder must never crash the app, and logs could leak health data.
            } finally {
                pending.finish()
            }
        }
    }

    protected fun slotOf(intent: Intent): LocalDateTime? =
        intent.getStringExtra(ReminderIntents.EXTRA_SLOT)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/** The alarm fired: rebuild the slot from the current data and post its notification. */
class ReminderReceiver : AsyncReceiver() {
    override suspend fun handle(reminders: ReminderCoordinator, intent: Intent) {
        val slot = slotOf(intent)
        if (slot == null) reminders.rearm() else reminders.onAlarm(slot)
    }
}

/** The two notification buttons. Neither opens the app. */
class ReminderActionReceiver : AsyncReceiver() {
    override suspend fun handle(reminders: ReminderCoordinator, intent: Intent) {
        val slot = slotOf(intent) ?: return
        when (intent.action) {
            ReminderIntents.ACTION_TAKE_ALL -> reminders.takeAll(slot)
            ReminderIntents.ACTION_SNOOZE -> reminders.snooze(slot)
        }
    }
}

/** Boot, app update, time or time-zone change, exact-alarm permission change: set the alarm again. */
class RescheduleReceiver : AsyncReceiver() {
    override suspend fun handle(reminders: ReminderCoordinator, intent: Intent) {
        reminders.rearm()
    }
}
