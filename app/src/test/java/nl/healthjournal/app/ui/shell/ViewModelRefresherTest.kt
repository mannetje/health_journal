package nl.healthjournal.app.ui.shell

import kotlinx.coroutines.runBlocking
import nl.healthjournal.app.AppNavDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewModelRefresherTest {

    private val calls = mutableListOf<String>()

    private val refresher = ViewModelRefresher(
        clearMessages = { calls += "clear" },
        clearMedicationMessages = { calls += "clearMedication" },
        loadMigraineProfile = { calls += "loadMigraine" },
        reloadData = { calls += "reload" },
        rescheduleReminders = { calls += "reminders" }
    )

    @Test
    fun theFirstTabShownKeepsBannersSoALanguageChangeDoesNotLoseThem() {
        refresher.onTabShown(AppNavDestination.PROFILE)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun theFirstLogTabReadsTheMigraineProfileWithoutClearing() {
        refresher.onTabShown(AppNavDestination.LOG)
        assertEquals(listOf("loadMigraine"), calls)
    }

    @Test
    fun changingTabClearsTheMessages() {
        refresher.onTabShown(AppNavDestination.PROFILE)
        refresher.onTabShown(AppNavDestination.HISTORY)
        assertEquals(listOf("clear"), calls)
    }

    @Test
    fun showingTheSameTabAgainClearsNothing() {
        refresher.onTabShown(AppNavDestination.HISTORY)
        refresher.onTabShown(AppNavDestination.HISTORY)
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun openingTheLogTabClearsAndThenReadsTheMigraineProfile() {
        refresher.onTabShown(AppNavDestination.PROFILE)
        refresher.onTabShown(AppNavDestination.LOG)
        assertEquals(listOf("clear", "loadMigraine"), calls)
    }

    @Test
    fun closingThePillboxClearsOnlyTheMedicationMessages() {
        refresher.onPillboxClosed()
        assertEquals(listOf("clearMedication"), calls)
    }

    @Test
    fun aRestoreReloadsTheDataAndThenRearmsTheReminders() = runBlocking {
        refresher.onRestored()
        assertEquals(listOf("reload", "reminders"), calls)
    }
}
