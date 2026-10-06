package nl.healthjournal.app.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeChoiceTest {

    @Test
    fun `system follows the device setting`() {
        assertTrue(ThemeChoice.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeChoice.SYSTEM.isDark(systemDark = false))
    }

    @Test
    fun `light and dark ignore the device setting`() {
        assertFalse(ThemeChoice.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeChoice.DARK.isDark(systemDark = false))
    }

    @Test
    fun `unknown or missing tag falls back to system`() {
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.fromTag(null))
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.fromTag("sepia"))
        assertEquals(ThemeChoice.DARK, ThemeChoice.fromTag("dark"))
    }
}
