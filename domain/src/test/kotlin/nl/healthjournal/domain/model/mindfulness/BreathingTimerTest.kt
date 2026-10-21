package nl.healthjournal.domain.model.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingTimerTest {
    private val ready = BreathingTimer.READY_MILLIS

    private fun box(duration: BreathingDuration = BreathingDuration.THREE_MINUTES) =
        BreathingTimer(BreathingPatterns.box, duration)

    private fun BreathingTimer.running(afterReadyMillis: Long): BreathingState.Running {
        val state = stateAt(readyMillis + afterReadyMillis)
        assertTrue("expected a running state but got $state", state is BreathingState.Running)
        return state as BreathingState.Running
    }

    @Test
    fun `the exercise starts with the ready moment`() {
        val timer = box()
        assertEquals(BreathingState.Ready(3), timer.stateAt(0))
        assertEquals(BreathingState.Ready(3), timer.stateAt(1))
        assertEquals(BreathingState.Ready(1), timer.stateAt(ready - 1))
    }

    @Test
    fun `the first phase starts exactly when the ready moment ends`() {
        val s = box().running(0)
        assertEquals(0, s.phaseIndex)
        assertEquals(BreathingPhaseKind.INHALE, s.phase.kind)
        assertEquals(4, s.secondsLeft)
        assertEquals(0f, s.phaseProgress, 0.0001f)
        assertEquals(0f, s.fill, 0.0001f)
    }

    @Test
    fun `negative elapsed time counts as the start`() {
        assertEquals(BreathingState.Ready(3), box().stateAt(-500))
    }

    @Test
    fun `seconds left count down within a phase`() {
        val timer = box()
        assertEquals(4, timer.running(0).secondsLeft)
        assertEquals(4, timer.running(999).secondsLeft)
        assertEquals(3, timer.running(1000).secondsLeft)
        assertEquals(1, timer.running(3999).secondsLeft)
    }

    @Test
    fun `box breathing runs its four phases in order and repeats`() {
        val timer = box()
        assertEquals(BreathingPhaseKind.INHALE, timer.running(0).phase.kind)
        assertEquals(BreathingPhaseKind.INHALE, timer.running(3_999).phase.kind)
        assertEquals(BreathingPhaseKind.HOLD, timer.running(4_000).phase.kind)
        assertEquals(BreathingPhaseKind.EXHALE, timer.running(8_000).phase.kind)
        assertEquals(BreathingPhaseKind.HOLD_EMPTY, timer.running(12_000).phase.kind)
        val again = timer.running(16_000)
        assertEquals(BreathingPhaseKind.INHALE, again.phase.kind)
        assertEquals(1, again.cycleIndex)
        assertEquals(0, again.phaseIndex)
    }

    @Test
    fun `the fill grows on the inhale, stays on the hold, shrinks on the exhale and stays small`() {
        val timer = box()
        assertEquals(0.5f, timer.running(2_000).fill, 0.0001f)
        assertEquals(1f, timer.running(4_000).fill, 0.0001f)
        assertEquals(1f, timer.running(6_000).fill, 0.0001f)
        assertEquals(1f, timer.running(8_000).fill, 0.0001f)
        assertEquals(0.5f, timer.running(10_000).fill, 0.0001f)
        assertEquals(0f, timer.running(12_000).fill, 0.0001f)
        assertEquals(0f, timer.running(14_000).fill, 0.0001f)
    }

    @Test
    fun `the physiological sigh fills in two steps`() {
        val timer = BreathingTimer(BreathingPatterns.physiologicalSigh, BreathingDuration.ONE_MINUTE)
        assertEquals(0f, timer.running(0).fill, 0.0001f)
        assertEquals(BreathingTimer.SIGH_FIRST_FILL / 2, timer.running(1_000).fill, 0.0001f)
        val second = timer.running(2_000)
        assertEquals(BreathingPhaseKind.INHALE_AGAIN, second.phase.kind)
        assertEquals(BreathingTimer.SIGH_FIRST_FILL, second.fill, 0.0001f)
        assertEquals(1f, timer.running(3_000).fill, 0.0001f)
        assertEquals(BreathingPhaseKind.EXHALE, timer.running(3_000).phase.kind)
        assertEquals(0.5f, timer.running(6_000).fill, 0.0001f)
    }

    @Test
    fun `a pattern without hold goes from inhale straight to exhale`() {
        val timer = BreathingTimer(BreathingPatterns.belly, BreathingDuration.ONE_MINUTE)
        assertEquals(BreathingPhaseKind.INHALE, timer.running(3_999).phase.kind)
        assertEquals(BreathingPhaseKind.EXHALE, timer.running(4_000).phase.kind)
        assertEquals(BreathingPhaseKind.INHALE, timer.running(10_000).phase.kind)
    }

    @Test
    fun `the exercise ends exactly after the chosen duration`() {
        val timer = box(BreathingDuration.THREE_MINUTES)
        assertEquals(ready + 180_000L, timer.totalMillis)
        assertTrue(timer.stateAt(timer.totalMillis - 1) is BreathingState.Running)
        assertEquals(BreathingState.Done, timer.stateAt(timer.totalMillis))
        assertEquals(BreathingState.Done, timer.stateAt(timer.totalMillis + 60_000))
    }

    @Test
    fun `the exercise ends in the middle of a phase when the duration does not fit the cycle`() {
        val timer = BreathingTimer(BreathingPatterns.fourSevenEight, BreathingDuration.ONE_MINUTE)
        // 60 s is three cycles of 19 s plus 3 s: still inside the first inhale of a fourth cycle
        val last = timer.running(59_999)
        assertEquals(3, last.cycleIndex)
        assertEquals(BreathingPhaseKind.INHALE, last.phase.kind)
        assertEquals(BreathingState.Done, timer.stateAt(timer.totalMillis))
    }

    @Test
    fun `a custom pattern follows its own counts`() {
        val timer = BreathingTimer(
            BreathingPatterns.custom(CustomBreathingCounts(2, 1, 3, 0)),
            BreathingDuration.ONE_MINUTE,
        )
        assertEquals(BreathingPhaseKind.INHALE, timer.running(1_999).phase.kind)
        assertEquals(BreathingPhaseKind.HOLD, timer.running(2_000).phase.kind)
        assertEquals(BreathingPhaseKind.EXHALE, timer.running(3_000).phase.kind)
        assertEquals(BreathingPhaseKind.INHALE, timer.running(6_000).phase.kind)
    }

    @Test
    fun `the phase progress runs from 0 to 1 within a phase`() {
        val s = box().running(1_000)
        assertEquals(0.25f, s.phaseProgress, 0.0001f)
    }

    @Test
    fun `a custom ready time is respected`() {
        val timer = BreathingTimer(BreathingPatterns.belly, BreathingDuration.ONE_MINUTE, readyMillis = 0)
        assertTrue(timer.stateAt(0) is BreathingState.Running)
    }
}
