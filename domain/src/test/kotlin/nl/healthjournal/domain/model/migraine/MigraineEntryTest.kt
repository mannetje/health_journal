package nl.healthjournal.domain.model.migraine

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MigraineEntryTest {
    private val profile = ProfileId.generate()
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val start = Instant.parse("2026-03-09T21:00:00Z") // Monday 22:00 in Amsterdam (CET)

    private fun entry(
        end: Instant? = null,
        intensity: Int? = null,
        tookSomething: Boolean = false,
        after2h: Int? = null,
        medicationHelpedMe: Boolean = false,
        relief: Set<ReliefMethod> = emptySet(),
        customRelief: Set<MigraineTagId> = emptySet(),
        reliefHelpedMe: Boolean = false,
        medicationText: String? = null,
        start: Instant = this.start
    ) = MigraineEntry(
        id = MeasurementId.generate(), profileId = profile, start = start, end = end, intensity = intensity,
        tookSomething = tookSomething, intensityAfter2Hours = after2h, medicationHelpedMe = medicationHelpedMe,
        reliefMethods = relief, customReliefIds = customRelief, reliefHelpedMe = reliefHelpedMe,
        medicationText = medicationText
    )

    @Test fun `minimal entry has only a start and is ongoing`() {
        val e = entry()
        assertTrue(e.ongoing)
        assertNull(e.duration)
        assertNull(e.intensity)
    }

    @Test fun `end before start is refused and end equal to start is allowed`() {
        assertThrows(IllegalArgumentException::class.java) { entry(end = start.minusSeconds(1)) }
        assertEquals(Duration.ZERO, entry(end = start).duration)
    }

    @Test fun `duration crosses midnight and weekday is that of the start`() {
        val e = entry(end = start.plus(Duration.ofHours(5).plusMinutes(30)))
        assertEquals(Duration.ofMinutes(330), e.duration)
        assertFalse(e.ongoing)
        assertEquals(DayOfWeek.MONDAY, e.startWeekday(zone))
        assertEquals(LocalDate.of(2026, 3, 9), e.startDay(zone))
    }

    @Test fun `intensity accepts 0 and 10 and refuses 11 and minus 1`() {
        assertEquals(0, entry(intensity = 0).intensity)
        assertEquals(10, entry(intensity = 10).intensity)
        assertThrows(IllegalArgumentException::class.java) { entry(intensity = 11) }
        assertThrows(IllegalArgumentException::class.java) { entry(intensity = -1) }
    }

    @Test fun `intensity after 2 hours needs something taken and 0 to 10`() {
        assertEquals(3, entry(tookSomething = true, after2h = 3).intensityAfter2Hours)
        assertEquals(0, entry(tookSomething = true, after2h = 0).intensityAfter2Hours)
        assertEquals(10, entry(tookSomething = true, after2h = 10).intensityAfter2Hours)
        assertThrows(IllegalArgumentException::class.java) { entry(tookSomething = true, after2h = 11) }
        assertThrows(IllegalArgumentException::class.java) { entry(tookSomething = false, after2h = 3) }
    }

    @Test fun `the two intensities are stored as entered`() {
        val e = entry(intensity = 7, tookSomething = true, after2h = 2)
        assertEquals(7, e.intensity)
        assertEquals(2, e.intensityAfter2Hours)
    }

    @Test fun `medication helped me needs something taken`() {
        assertTrue(entry(tookSomething = true, medicationHelpedMe = true).medicationHelpedMe)
        assertThrows(IllegalArgumentException::class.java) { entry(medicationHelpedMe = true) }
    }

    @Test fun `relief helped me needs a relief method or a personal relief tag`() {
        assertTrue(entry(relief = setOf(ReliefMethod.HEAT), reliefHelpedMe = true).reliefHelpedMe)
        assertTrue(entry(customRelief = setOf(MigraineTagId.generate()), reliefHelpedMe = true).reliefHelpedMe)
        assertThrows(IllegalArgumentException::class.java) { entry(reliefHelpedMe = true) }
    }

    @Test fun `medication text is limited to 100 characters`() {
        assertEquals(100, entry(medicationText = "a".repeat(100)).medicationText!!.length)
        assertThrows(IllegalArgumentException::class.java) { entry(medicationText = "a".repeat(101)) }
        assertThrows(IllegalArgumentException::class.java) { entry(medicationText = " ") }
    }

    @Test fun `normalizeMedicationText trims and drops blank`() {
        assertEquals("my usual one", MigraineEntry.normalizeMedicationText("  my usual one "))
        assertNull(MigraineEntry.normalizeMedicationText("   "))
        assertNull(MigraineEntry.normalizeMedicationText(null))
        assertThrows(IllegalArgumentException::class.java) { MigraineEntry.normalizeMedicationText("a".repeat(101)) }
    }

    @Test fun `time of day boundaries`() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(LocalTime.of(0, 0)))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(LocalTime.of(5, 59)))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(LocalTime.of(6, 0)))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(LocalTime.of(11, 59)))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(LocalTime.of(12, 0)))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(LocalTime.of(17, 59)))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(LocalTime.of(18, 0)))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(LocalTime.of(23, 59)))
    }

    @Test fun `start time of day follows the zone`() {
        assertEquals(TimeOfDay.EVENING, entry().startTimeOfDay(zone))
        assertEquals(TimeOfDay.EVENING, entry().startTimeOfDay(ZoneId.of("UTC")))
        assertEquals(TimeOfDay.NIGHT, entry(start = Instant.parse("2026-03-09T23:30:00Z")).startTimeOfDay(zone))
    }
}
