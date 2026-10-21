package nl.healthjournal.domain.model.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeditationTimerTest {
    private fun minutes(n: Int) = n * 60_000L

    @Test
    fun `a timer without interval has a start bell and an end bell`() {
        val timer = MeditationTimer(10)
        assertEquals(
            listOf(Bell(0, BellKind.START), Bell(minutes(10), BellKind.END)),
            timer.bells,
        )
    }

    @Test
    fun `a 10 minute timer with an interval of 2 minutes rings at the start, every 2 minutes and at the end`() {
        val timer = MeditationTimer(10, intervalMinutes = 2)
        assertEquals(
            listOf(
                Bell(0, BellKind.START),
                Bell(minutes(2), BellKind.INTERVAL),
                Bell(minutes(4), BellKind.INTERVAL),
                Bell(minutes(6), BellKind.INTERVAL),
                Bell(minutes(8), BellKind.INTERVAL),
                Bell(minutes(10), BellKind.END),
            ),
            timer.bells,
        )
    }

    @Test
    fun `an interval that does not divide the duration leaves a shorter last stretch`() {
        val timer = MeditationTimer(10, intervalMinutes = 4)
        assertEquals(listOf(0L, minutes(4), minutes(8), minutes(10)), timer.bells.map { it.atMillis })
        assertEquals(
            listOf(BellKind.START, BellKind.INTERVAL, BellKind.INTERVAL, BellKind.END),
            timer.bells.map { it.kind },
        )
    }

    @Test
    fun `no interval bell rings together with the end bell`() {
        val timer = MeditationTimer(5, intervalMinutes = 1)
        assertEquals(1, timer.bells.count { it.atMillis == minutes(5) })
        assertEquals(BellKind.END, timer.bells.last().kind)
    }

    @Test
    fun `remaining time counts down and stops at zero`() {
        val timer = MeditationTimer(5)
        assertEquals(minutes(5), timer.remainingMillis(0))
        assertEquals(minutes(5), timer.remainingMillis(-10))
        assertEquals(minutes(4), timer.remainingMillis(minutes(1)))
        assertEquals(0L, timer.remainingMillis(minutes(5)))
        assertEquals(0L, timer.remainingMillis(minutes(6)))
    }

    @Test
    fun `the timer is finished exactly at the duration`() {
        val timer = MeditationTimer(5)
        assertFalse(timer.isFinished(minutes(5) - 1))
        assertTrue(timer.isFinished(minutes(5)))
    }

    @Test
    fun `bells due are returned once from the next index`() {
        val timer = MeditationTimer(10, intervalMinutes = 2)
        assertEquals(listOf(Bell(0, BellKind.START)), timer.bellsDue(0, 0))
        assertEquals(emptyList<Bell>(), timer.bellsDue(1, minutes(2) - 1))
        assertEquals(listOf(Bell(minutes(2), BellKind.INTERVAL)), timer.bellsDue(1, minutes(2)))
        assertEquals(emptyList<Bell>(), timer.bellsDue(2, minutes(2)))
    }

    @Test
    fun `a late ticker still gets every missed bell in order`() {
        val timer = MeditationTimer(10, intervalMinutes = 2)
        val due = timer.bellsDue(1, minutes(5))
        assertEquals(listOf(minutes(2), minutes(4)), due.map { it.atMillis })
    }

    @Test
    fun `all bells are due at the end`() {
        val timer = MeditationTimer(10, intervalMinutes = 5)
        assertEquals(timer.bells, timer.bellsDue(0, minutes(10)))
        assertEquals(emptyList<Bell>(), timer.bellsDue(timer.bells.size, minutes(10)))
    }

    @Test
    fun `the limits of the duration are 1 and 120 minutes`() {
        assertEquals(minutes(1), MeditationTimer(1).durationMillis)
        assertEquals(minutes(120), MeditationTimer(120).durationMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a duration of 0 is rejected`() {
        MeditationTimer(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a duration above the limit is rejected`() {
        MeditationTimer(121)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an interval equal to the duration is rejected`() {
        MeditationTimer(5, intervalMinutes = 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an interval of 0 is rejected`() {
        MeditationTimer(5, intervalMinutes = 0)
    }

    @Test
    fun `every preset duration and interval is valid`() {
        for (d in MeditationTimer.PRESET_MINUTES) {
            MeditationTimer(d)
            for (i in MeditationTimer.INTERVAL_PRESET_MINUTES.filter { it < d }) MeditationTimer(d, i)
        }
    }
}
