package nl.healthjournal.app.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import nl.healthjournal.app.MainActivity
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.medication.doseString
import nl.healthjournal.domain.model.medication.Slot
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Builds, posts and cancels the one notification of a slot. Texts come from the context it is given. */
class ReminderNotifier(private val appContext: Context) {

    private val manager: NotificationManager = appContext.getSystemService(NotificationManager::class.java)

    /** Derived from the planned time, so posting the same slot again updates the notification instead of adding one. */
    fun idFor(slot: LocalDateTime): Int =
        (slot.toLocalDate().toEpochDay() * MINUTES_PER_DAY + slot.hour * 60 + slot.minute).toInt()

    /** Slots that currently have a reminder notification on screen. */
    fun shownSlots(): List<LocalDateTime> = manager.activeNotifications
        .filter { it.notification.channelId == CHANNEL_ID }
        .mapNotNull { it.notification.extras.getString(ReminderIntents.EXTRA_SLOT)?.let(LocalDateTime::parse) }

    fun cancel(slot: LocalDateTime) = manager.cancel(idFor(slot))

    /**
     * Posts the notification for [slot] listing its open items. With [showDetails] off, the lock screen shows
     * only a generic text. [ctx] carries the app language.
     */
    fun show(ctx: Context, slot: Slot, showDetails: Boolean) {
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(ctx)
        val id = idFor(slot.time)
        val lines = slot.openItems.map {
            val dosage = it.medication.dosage
            ctx.getString(
                R.string.reminder_line,
                it.medication.name.value,
                ctx.doseString(dosage.amountPerIntake, dosage.doseUnit)
            )
        }
        val generic = ctx.getString(R.string.reminder_title_generic)
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(slot.time)

        val publicVersion = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle(generic)
            .build()

        val inbox = Notification.InboxStyle().also { style -> lines.forEach { style.addLine(it) } }
        val notification = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle(time)
            .setContentText(lines.joinToString("; "))
            .setStyle(inbox)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(if (showDetails) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openPillbox(id))
            .addAction(action(ctx, R.string.medication_action_taken_all, ReminderIntents.ACTION_TAKE_ALL, slot.time, id * 3 + 1))
            .addAction(action(ctx, R.string.reminder_action_snooze, ReminderIntents.ACTION_SNOOZE, slot.time, id * 3 + 2))
            .addExtras(Bundle().apply { putString(ReminderIntents.EXTRA_SLOT, slot.time.toString()) })
            .build()
        manager.notify(id, notification)
    }

    private fun action(ctx: Context, label: Int, action: String, slot: LocalDateTime, request: Int): Notification.Action {
        val intent = Intent(appContext, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderIntents.EXTRA_SLOT, slot.toString())
        val pending = PendingIntent.getBroadcast(
            appContext, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(
            Icon.createWithResource(ctx, R.drawable.ic_notification_pill), ctx.getString(label), pending
        ).build()
    }

    private fun openPillbox(id: Int): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java)
            .putExtra(ReminderIntents.EXTRA_OPEN_PILLBOX, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            appContext, id * 3, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel(ctx: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, ctx.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH
        ).apply { description = ctx.getString(R.string.reminder_channel_description) }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val MINUTES_PER_DAY = 1440
    }
}
