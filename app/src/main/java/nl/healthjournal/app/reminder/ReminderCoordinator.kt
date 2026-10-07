package nl.healthjournal.app.reminder

import android.content.Context
import kotlinx.coroutines.CancellationException
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.ReminderPreference
import nl.healthjournal.app.settings.withAppLocale
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.RearmRemindersUseCase
import nl.healthjournal.domain.usecase.TakeAllForSlotUseCase
import java.time.LocalDateTime

/**
 * Connects the domain use cases to the alarm and the notification. Used by the receivers (outside the Activity)
 * and by the pillbox (through [ReminderHooks]). A failure here is logged and never breaks the caller.
 */
class ReminderCoordinator(
    private val context: Context,
    private val profileRepository: ProfileRepositoryPort,
    private val rearmReminders: RearmRemindersUseCase,
    private val takeAllForSlot: TakeAllForSlotUseCase,
    private val scheduler: AlarmReminderScheduler,
    private val notifier: ReminderNotifier,
    private val preference: ReminderPreference
) : ReminderHooks {

    fun canScheduleExact(): Boolean = scheduler.canScheduleExact()

    override suspend fun changed() = guarded {
        val profile = activeProfile()
        rearmReminders(profile)
        if (profile != null) notifier.shownSlots().forEach { show(profile, it) }
    }

    /** The alarm fired for [slot]: post its notification when something is still open, then set the next alarm. */
    suspend fun onAlarm(slot: LocalDateTime) = guarded {
        val profile = activeProfile()
        if (profile != null) show(profile, slot)
        rearmReminders(profile)
    }

    /** The boot, update or clock receivers: only the alarm needs to be set again. */
    suspend fun rearm() = guarded { rearmReminders(activeProfile()) }

    suspend fun takeAll(slot: LocalDateTime) = guarded {
        activeProfile()?.let { takeAllForSlot(it, slot) }
        notifier.cancel(slot)
    }

    suspend fun snooze(slot: LocalDateTime) = guarded {
        notifier.cancel(slot)
        scheduler.snooze(slot, preference.snoozeMinutes)
    }

    private suspend fun show(profile: ProfileId, slotTime: LocalDateTime) {
        val slot = takeAllForSlot.slotAt(profile, slotTime)
        if (slot == null || slot.openItems.isEmpty()) {
            notifier.cancel(slotTime)
            return
        }
        val language = LanguagePreference(context)
        notifier.show(context.withAppLocale(language.language, language.region), slot, preference.showDetailsOnLockScreen)
    }

    private suspend fun activeProfile(): ProfileId? = profileRepository.getActiveProfile()?.id

    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Nothing is logged (it could leak names and doses); the next change or resume sets things right.
        }
    }
}
