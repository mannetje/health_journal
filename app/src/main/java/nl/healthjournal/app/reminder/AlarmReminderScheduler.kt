package nl.healthjournal.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import nl.healthjournal.domain.model.medication.resolveIn
import nl.healthjournal.domain.port.secondary.ReminderSchedulerPort
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Sets the one alarm for the next slot on [AlarmManager]. Exact when the user allowed exact alarms,
 * otherwise inexact (it can be a few minutes late); the feature works either way.
 */
class AlarmReminderScheduler(
    private val context: Context,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) : ReminderSchedulerPort {

    private val alarms: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    override fun arm(slot: LocalDateTime) {
        setAlarm(slot.resolveIn(zone()).toEpochMilli(), slot, REQUEST_NEXT)
    }

    override fun cancel() {
        alarms.cancel(pendingIntent(REQUEST_NEXT, null))
    }

    /** A one-shot alarm that posts the slot's notification again, without changing the planned time. */
    fun snooze(slot: LocalDateTime, minutes: Int) {
        setAlarm(System.currentTimeMillis() + minutes * MILLIS_PER_MINUTE, slot, REQUEST_SNOOZE)
    }

    private fun setAlarm(atMillis: Long, slot: LocalDateTime, request: Int) {
        val intent = pendingIntent(request, slot)
        if (canScheduleExact()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
                return
            } catch (_: SecurityException) {
                // The permission was withdrawn after the check; fall back to an inexact alarm.
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
    }

    private fun pendingIntent(request: Int, slot: LocalDateTime?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderIntents.ACTION_ALARM)
        slot?.let { intent.putExtra(ReminderIntents.EXTRA_SLOT, it.toString()) }
        return PendingIntent.getBroadcast(
            context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_NEXT = 1
        const val REQUEST_SNOOZE = 2
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
