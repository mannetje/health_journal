package nl.healthjournal.app.ui.shell

import nl.healthjournal.app.settings.AppLanguage
import nl.healthjournal.app.settings.AppRegion
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.settings.ThemeChoice
import nl.healthjournal.app.settings.UnitSystemChoice
import nl.healthjournal.domain.model.lock.LockTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsStateTest {

    private class FakeStore : SettingsStore {
        override var language = AppLanguage.SYSTEM
        override var region = AppRegion.SYSTEM
        override var unitSystem = UnitSystemChoice.SYSTEM
        override var glucoseUnit = GlucoseUnitChoice.SYSTEM
        override var theme = ThemeChoice.SYSTEM
        override var medicationNoticeAccepted = false
        override var snoozeMinutes = 10
        override var showDetailsOnLockScreen = false
        override var secureScreen = false
        override var appLockEnabled = false
        override var lockTimeout = LockTimeout.ONE_MINUTE
    }

    private val store = FakeStore()

    @Test
    fun startsFromWhatIsSaved() {
        store.language = AppLanguage.DUTCH
        store.snoozeMinutes = 30
        store.lockTimeout = LockTimeout.FIVE_MINUTES
        val state = AppSettingsState(store)
        assertEquals(AppLanguage.DUTCH, state.language)
        assertEquals(30, state.snoozeMinutes)
        assertEquals(LockTimeout.FIVE_MINUTES, state.lockTimeout)
    }

    @Test
    fun languageAndRegionAreWrittenThrough() {
        val state = AppSettingsState(store)
        state.changeLanguage(AppLanguage.ENGLISH)
        state.changeRegion(AppRegion.NETHERLANDS)
        assertEquals(AppLanguage.ENGLISH, state.language)
        assertEquals(AppLanguage.ENGLISH, store.language)
        assertEquals(AppRegion.NETHERLANDS, state.region)
        assertEquals(AppRegion.NETHERLANDS, store.region)
    }

    @Test
    fun unitsAndThemeAreWrittenThrough() {
        val state = AppSettingsState(store)
        state.changeUnitSystem(UnitSystemChoice.IMPERIAL)
        state.changeGlucoseUnit(GlucoseUnitChoice.MGDL)
        state.changeTheme(ThemeChoice.DARK)
        assertEquals(UnitSystemChoice.IMPERIAL, store.unitSystem)
        assertEquals(GlucoseUnitChoice.MGDL, store.glucoseUnit)
        assertEquals(ThemeChoice.DARK, store.theme)
    }

    @Test
    fun displayUnitsFollowTheChosenUnits() {
        val state = AppSettingsState(store)
        state.changeUnitSystem(UnitSystemChoice.IMPERIAL)
        assertTrue(state.displayUnits.isImperial)
        state.changeUnitSystem(UnitSystemChoice.METRIC)
        assertFalse(state.displayUnits.isImperial)
    }

    @Test
    fun noticeAndReminderSettingsAreWrittenThrough() {
        val state = AppSettingsState(store)
        state.acceptMedicationNotice()
        state.changeSnoozeMinutes(60)
        state.changeShowDetailsOnLockScreen(true)
        assertTrue(store.medicationNoticeAccepted)
        assertEquals(60, store.snoozeMinutes)
        assertTrue(store.showDetailsOnLockScreen)
        assertTrue(state.medicationNoticeAccepted)
    }

    @Test
    fun securitySettingsAreWrittenThrough() {
        val state = AppSettingsState(store)
        state.changeSecureScreen(true)
        state.changeAppLockEnabled(true)
        state.changeLockTimeout(LockTimeout.IMMEDIATELY)
        assertTrue(store.secureScreen)
        assertTrue(store.appLockEnabled)
        assertEquals(LockTimeout.IMMEDIATELY, store.lockTimeout)
        assertEquals(LockTimeout.IMMEDIATELY, state.lockTimeout)
    }
}
