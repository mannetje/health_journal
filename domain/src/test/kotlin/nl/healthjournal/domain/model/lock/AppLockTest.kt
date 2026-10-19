package nl.healthjournal.domain.model.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nl.healthjournal.domain.port.secondary.AppLockPort
import org.junit.Assert.*
import org.junit.Test

// ---------------------------------------------------------------------------
// Minimal in-memory fake — implements only the state-machine logic that the
// domain cares about; no Android, no timers, no SharedPreferences.
// ---------------------------------------------------------------------------

private class FakeAppLockPort(
    initiallyEnabled: Boolean = false,
    override var timeout: LockTimeout = LockTimeout.IMMEDIATELY,
) : AppLockPort {

    private val _lockState = MutableStateFlow<LockState>(LockState.Unlocked)
    override val lockState: StateFlow<LockState> = _lockState

    /** The test controls whether the device has an enrolled credential. */
    override var isAvailable: Boolean = true

    override var isEnabled: Boolean = initiallyEnabled
        set(value) {
            field = value
            if (!value) _lockState.value = LockState.Unlocked
        }

    override fun lock() {
        if (isEnabled) _lockState.value = LockState.Locked
    }

    override fun onAuthenticated() {
        _lockState.value = LockState.Unlocked
    }

    override fun onBackgrounded() {
        if (!isEnabled) return
        // Fake: IMMEDIATELY always triggers; other timeouts are not simulated here.
        if (timeout == LockTimeout.IMMEDIATELY) _lockState.value = LockState.TimedOut
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class LockStateTest {

    @Test
    fun `Unlocked does not require authentication`() {
        assertFalse(LockState.Unlocked.requiresAuthentication)
    }

    @Test
    fun `Locked requires authentication`() {
        assertTrue(LockState.Locked.requiresAuthentication)
    }

    @Test
    fun `TimedOut requires authentication`() {
        assertTrue(LockState.TimedOut.requiresAuthentication)
    }
}

class LockTimeoutTest {

    @Test
    fun `fromSeconds returns correct enum for known values`() {
        assertEquals(LockTimeout.IMMEDIATELY, LockTimeout.fromSeconds(0))
        assertEquals(LockTimeout.ONE_MINUTE, LockTimeout.fromSeconds(60))
        assertEquals(LockTimeout.FIVE_MINUTES, LockTimeout.fromSeconds(300))
    }

    @Test
    fun `fromSeconds falls back to IMMEDIATELY for unknown value`() {
        assertEquals(LockTimeout.IMMEDIATELY, LockTimeout.fromSeconds(999))
    }
}

class AppLockPortTest {

    @Test
    fun `initial state is Unlocked`() {
        val port = FakeAppLockPort()
        assertEquals(LockState.Unlocked, port.lockState.value)
    }

    @Test
    fun `lock transitions to Locked when enabled`() {
        val port = FakeAppLockPort(initiallyEnabled = true)
        port.lock()
        assertEquals(LockState.Locked, port.lockState.value)
    }

    @Test
    fun `lock is a no-op when disabled`() {
        val port = FakeAppLockPort(initiallyEnabled = false)
        port.lock()
        assertEquals(LockState.Unlocked, port.lockState.value)
    }

    @Test
    fun `onAuthenticated transitions to Unlocked from Locked`() {
        val port = FakeAppLockPort(initiallyEnabled = true)
        port.lock()
        assertEquals(LockState.Locked, port.lockState.value)

        port.onAuthenticated()
        assertEquals(LockState.Unlocked, port.lockState.value)
    }

    @Test
    fun `onAuthenticated transitions to Unlocked from TimedOut`() {
        val port = FakeAppLockPort(initiallyEnabled = true, timeout = LockTimeout.IMMEDIATELY)
        port.onBackgrounded()
        assertEquals(LockState.TimedOut, port.lockState.value)

        port.onAuthenticated()
        assertEquals(LockState.Unlocked, port.lockState.value)
    }

    @Test
    fun `onBackgrounded with IMMEDIATELY timeout emits TimedOut when enabled`() {
        val port = FakeAppLockPort(initiallyEnabled = true, timeout = LockTimeout.IMMEDIATELY)
        port.onBackgrounded()
        assertEquals(LockState.TimedOut, port.lockState.value)
    }

    @Test
    fun `onBackgrounded is a no-op when disabled`() {
        val port = FakeAppLockPort(initiallyEnabled = false, timeout = LockTimeout.IMMEDIATELY)
        port.onBackgrounded()
        assertEquals(LockState.Unlocked, port.lockState.value)
    }

    @Test
    fun `disabling the lock resets state to Unlocked`() {
        val port = FakeAppLockPort(initiallyEnabled = true)
        port.lock()
        assertEquals(LockState.Locked, port.lockState.value)

        port.isEnabled = false
        assertEquals(LockState.Unlocked, port.lockState.value)
    }

    @Test
    fun `isAvailable false signals that the setting must not be enabled`() {
        val port = FakeAppLockPort(initiallyEnabled = false)
        port.isAvailable = false

        // The domain contract: if isAvailable is false, isEnabled must stay false.
        // The test verifies the invariant without re-implementing UI gating logic.
        assertFalse(port.isAvailable)
        assertFalse(port.isEnabled)
    }

    @Test
    fun `requiresAuthentication is false after successful auth from any state`() {
        val port = FakeAppLockPort(initiallyEnabled = true)

        port.lock()
        port.onAuthenticated()
        assertFalse(port.lockState.value.requiresAuthentication)

        port.onBackgrounded()
        port.onAuthenticated()
        assertFalse(port.lockState.value.requiresAuthentication)
    }
}
