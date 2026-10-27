package nl.healthjournal.app.ui.history.charts

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class MigraineDateRangeTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val profile = ProfileId.generate()
    private val today = LocalDate.now(zone)

    private fun entryOn(day: LocalDate) =
        MigraineEntry(MeasurementId.generate(), profile, day.atTime(12, 0).atZone(zone).toInstant())

    @Test
    fun aFixedRangeEndsTodayAndCoversExactlyThatManyDays() {
        val range = migraineDateRange(emptyList(), TrendDateRange.SEVEN_DAYS, zone)

        assertEquals(today, range.endInclusive)
        assertEquals(today.minusDays(6), range.start)
    }

    @Test
    fun aFixedRangeDoesNotDependOnTheEntries() {
        val entries = listOf(entryOn(today.minusDays(200)))

        val range = migraineDateRange(entries, TrendDateRange.THIRTY_DAYS, zone)

        assertEquals(today.minusDays(29)..today, range)
    }

    @Test
    fun allStartsAtTheFirstEntryAndStillEndsToday() {
        val entries = listOf(entryOn(today.minusDays(3)), entryOn(today.minusDays(40)), entryOn(today.minusDays(10)))

        val range = migraineDateRange(entries, TrendDateRange.ALL, zone)

        assertEquals(today.minusDays(40)..today, range)
    }

    @Test
    fun allWithoutEntriesIsJustToday() {
        assertEquals(today..today, migraineDateRange(emptyList(), TrendDateRange.ALL, zone))
    }
}
