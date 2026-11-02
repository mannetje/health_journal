package nl.healthjournal.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.healthjournal.app.lock.AppLockManager
import nl.healthjournal.app.reminder.AlarmReminderScheduler
import nl.healthjournal.app.reminder.ReminderCoordinator
import nl.healthjournal.app.reminder.ReminderNotifier
import nl.healthjournal.app.settings.AppLockPreference
import nl.healthjournal.app.settings.BackupPreferenceBridge
import nl.healthjournal.app.settings.MetricDisplayOrderPreference
import nl.healthjournal.app.settings.ReminderPreference
import nl.healthjournal.app.settings.UnitPreference
import nl.healthjournal.app.ui.medication.MedicationUseCases
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.RearmRemindersUseCase
import nl.healthjournal.domain.usecase.TakeAllForSlotUseCase
import javax.inject.Singleton

/** Preferences and process-owned objects: each is a singleton because it holds state or is shared with the receivers. */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun metricDisplayOrder(@ApplicationContext context: Context) = MetricDisplayOrderPreference(context)

    @Provides
    @Singleton
    fun reminderPreference(@ApplicationContext context: Context) = ReminderPreference(context)

    @Provides
    @Singleton
    fun unitPreference(@ApplicationContext context: Context) = UnitPreference(context)

    @Provides
    @Singleton
    fun backupPreferences(@ApplicationContext context: Context) = BackupPreferenceBridge(context)

    /** Owned by the process, not the Activity: the lock must survive an Activity being recreated. */
    @Provides
    @Singleton
    fun appLock(@ApplicationContext context: Context) = AppLockManager(context, AppLockPreference(context))

    @Provides
    @Singleton
    fun reminderScheduler(@ApplicationContext context: Context) = AlarmReminderScheduler(context)

    @Provides
    @Singleton
    fun reminders(
        @ApplicationContext context: Context,
        profile: ProfileRepositoryPort,
        medication: MedicationRepositoryPort,
        medicationUseCases: MedicationUseCases,
        scheduler: AlarmReminderScheduler,
        preference: ReminderPreference
    ) = ReminderCoordinator(
        context = context,
        profileRepository = profile,
        rearmReminders = RearmRemindersUseCase(medication, scheduler),
        takeAllForSlot = TakeAllForSlotUseCase(medicationUseCases.getDay, medicationUseCases.recordSlot),
        scheduler = scheduler,
        notifier = ReminderNotifier(context),
        preference = preference
    )
}
