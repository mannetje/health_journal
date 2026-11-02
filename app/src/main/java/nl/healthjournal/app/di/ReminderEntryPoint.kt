package nl.healthjournal.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.healthjournal.app.reminder.ReminderCoordinator

/** What the broadcast receivers take from the graph: the system creates them, so they cannot be injected by constructor. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun reminders(): ReminderCoordinator
}
