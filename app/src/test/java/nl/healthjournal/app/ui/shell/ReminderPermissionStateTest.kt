package nl.healthjournal.app.ui.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPermissionStateTest {

    private var notifications = false
    private var exact = true
    private var asked = false

    private fun state(needsRuntimePermission: Boolean = true) = ReminderPermissionState(
        readNotificationsAllowed = { notifications },
        readExactAlarmsAllowed = { exact },
        askedBefore = { asked },
        markAsked = { asked = true },
        needsRuntimePermission = needsRuntimePermission
    ).also { it.refresh() }

    @Test
    fun refreshReadsBothPermissions() {
        notifications = true
        exact = false
        val state = state()
        assertTrue(state.notificationsAllowed)
        assertFalse(state.exactAlarmsAllowed)
        notifications = false
        exact = true
        state.refresh()
        assertFalse(state.notificationsAllowed)
        assertTrue(state.exactAlarmsAllowed)
    }

    @Test
    fun asksOnceMedicationExistsAndThePillboxIsOpen() {
        val state = state()
        state.onPillboxState(hasMedication = true, pillboxOpen = true)
        assertTrue(state.showDialog)
    }

    @Test
    fun doesNotAskWithoutMedicationOrOutsideThePillbox() {
        val state = state()
        state.onPillboxState(hasMedication = false, pillboxOpen = true)
        state.onPillboxState(hasMedication = true, pillboxOpen = false)
        assertFalse(state.showDialog)
    }

    @Test
    fun doesNotAskWhenNotificationsAreAlreadyAllowed() {
        notifications = true
        val state = state()
        state.onPillboxState(hasMedication = true, pillboxOpen = true)
        assertFalse(state.showDialog)
    }

    @Test
    fun doesNotAskBeforeAndroid13() {
        val state = state(needsRuntimePermission = false)
        state.onPillboxState(hasMedication = true, pillboxOpen = true)
        assertFalse(state.showDialog)
    }

    @Test
    fun answeringClosesTheDialogAndAsksNeverAgain() {
        val state = state()
        state.onPillboxState(hasMedication = true, pillboxOpen = true)
        state.answered()
        assertFalse(state.showDialog)
        assertTrue(asked)
        state.onPillboxState(hasMedication = true, pillboxOpen = true)
        assertFalse(state.showDialog)
    }
}
