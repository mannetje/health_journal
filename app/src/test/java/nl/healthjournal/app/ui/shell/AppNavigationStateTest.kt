package nl.healthjournal.app.ui.shell

import androidx.compose.runtime.saveable.SaverScope
import nl.healthjournal.app.AppNavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationStateTest {

    @Test
    fun startsOnTheLogTabWithNothingOpen() {
        val state = AppNavigationState()
        assertEquals(AppNavDestination.LOG, state.currentTab)
        assertFalse(state.showPillbox)
        assertFalse(state.showMindfulness)
        assertEquals(AppNavDestination.LOG, state.selectedTab)
    }

    @Test
    fun openingThePillboxClosesMindfulnessAndTheOtherWayAround() {
        val state = AppNavigationState()
        state.openMindfulness()
        state.openPillbox()
        assertTrue(state.showPillbox)
        assertFalse(state.showMindfulness)
        state.openMindfulness()
        assertTrue(state.showMindfulness)
        assertFalse(state.showPillbox)
    }

    @Test
    fun noTabIsSelectedWhileAScreenIsOpenOverTheTabs() {
        val state = AppNavigationState(tab = AppNavDestination.HISTORY)
        state.openPillbox()
        assertNull(state.selectedTab)
        state.closePillbox()
        assertEquals(AppNavDestination.HISTORY, state.selectedTab)
    }

    @Test
    fun selectingATabClosesBothScreensOverTheTabs() {
        val state = AppNavigationState()
        state.openPillbox()
        state.openTab(AppNavDestination.PROFILE)
        assertFalse(state.showPillbox)
        assertFalse(state.showMindfulness)
        assertEquals(AppNavDestination.PROFILE, state.currentTab)
    }

    @Test
    fun aNotificationTapWhileMindfulnessIsOpenShowsThePillboxOnToday() {
        val state = AppNavigationState(tab = AppNavDestination.HISTORY, mindfulness = true)
        state.openPillboxOnToday()
        assertTrue(state.showPillbox)
        assertFalse(state.showMindfulness)
        assertTrue(state.openPillboxOnToday)
        assertEquals(AppNavDestination.HISTORY, state.currentTab)
        state.onOpenedOnToday()
        assertFalse(state.openPillboxOnToday)
    }

    @Test
    fun backFromThePillboxReturnsToTheTabItWasOpenedFrom() {
        val state = AppNavigationState(tab = AppNavDestination.HISTORY)
        state.openPillbox()
        state.closePillbox()
        assertEquals(AppNavDestination.HISTORY, state.currentTab)
        assertFalse(state.showPillbox)
    }

    @Test
    fun migraineBackOnlyAppliesOnTheLogAndHistoryTabs() {
        val state = AppNavigationState(tab = AppNavDestination.LOG)
        assertTrue(state.migraineBackEnabled(migraineOpen = true))
        assertFalse(state.migraineBackEnabled(migraineOpen = false))
        state.openTab(AppNavDestination.HISTORY)
        assertTrue(state.migraineBackEnabled(migraineOpen = true))
        state.openTab(AppNavDestination.PROFILE)
        assertFalse(state.migraineBackEnabled(migraineOpen = true))
    }

    @Test
    fun savedStateRestoresTheTabAndScreensButNotThePendingNotificationTap() {
        val state = AppNavigationState(tab = AppNavDestination.HISTORY, logMigraineTab = true, encryptionBannerDismissed = true)
        state.openPillboxOnToday()

        val saved = with(AppNavigationState.Saver) { SaverScope { true }.save(state) }!!
        val restored = AppNavigationState.Saver.restore(saved)!!

        assertEquals(AppNavDestination.HISTORY, restored.currentTab)
        assertTrue(restored.showPillbox)
        assertFalse(restored.showMindfulness)
        assertTrue(restored.logMigraineTab)
        assertTrue(restored.encryptionBannerDismissed)
        assertFalse(restored.openPillboxOnToday)
    }
}
