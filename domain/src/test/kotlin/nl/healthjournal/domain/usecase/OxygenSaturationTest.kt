package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.testing.FakeHealthLogRepository
import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.common.EntryComment
import nl.healthjournal.domain.model.metrics.OxygenContext
import nl.healthjournal.domain.model.metrics.OxygenSaturationEntry
import nl.healthjournal.domain.model.metrics.OxygenSaturationPercent
import nl.healthjournal.domain.model.metrics.OxygenSite
import nl.healthjournal.domain.model.metrics.OxygenStatistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class OxygenSaturationTest {
    private val profileId = ProfileId.generate()
    private val repo = FakeHealthLogRepository()

    private fun entry(percent: Int, context: OxygenContext? = null, at: Long = 0) = OxygenSaturationEntry(
        id = MeasurementId.generate(),
        profileId = profileId,
        timestamp = Instant.ofEpochSecond(at),
        saturation = OxygenSaturationPercent(percent),
        context = context
    )

    @Test
    fun `percent accepts the limits and refuses outside them`() {
        assertEquals(50, OxygenSaturationPercent(50).value)
        assertEquals(100, OxygenSaturationPercent(100).value)
        assertFailsWithIllegalArgument { OxygenSaturationPercent(49) }
        assertFailsWithIllegalArgument { OxygenSaturationPercent(101) }
    }

    @Test
    fun `pulse is optional and limited to 30 to 250`() {
        assertNull(entry(97).pulse)
        assertNotNull(entry(97).copy(pulse = 30))
        assertNotNull(entry(97).copy(pulse = 250))
        assertFailsWithIllegalArgument { entry(97).copy(pulse = 29) }
        assertFailsWithIllegalArgument { entry(97).copy(pulse = 251) }
    }

    @Test
    fun `context and site codes read back and unknown codes read as none`() {
        OxygenContext.entries.forEach { assertEquals(it, OxygenContext.fromCode(it.name)) }
        OxygenSite.entries.forEach { assertEquals(it, OxygenSite.fromCode(it.name)) }
        assertNull(OxygenContext.fromCode("UNKNOWN_X"))
        assertNull(OxygenSite.fromCode("ARM"))
        assertNull(OxygenContext.fromCode(null))
    }

    @Test
    fun `record stores the reading with its notes and comment`() = runBlocking {
        val saved = RecordOxygenSaturationUseCase(repo)(
            profileId, 96, pulse = 68, context = OxygenContext.AT_REST, site = OxygenSite.FINGER,
            oxygenGiven = true, comment = "  after the stairs "
        )
        assertEquals(listOf(saved), repo.oxygenSaturations)
        assertEquals(96, saved.saturation.value)
        assertEquals(68, saved.pulse)
        assertEquals(OxygenContext.AT_REST, saved.context)
        assertEquals(OxygenSite.FINGER, saved.site)
        assertTrue(saved.oxygenGiven)
        assertEquals(EntryComment("after the stairs"), saved.comment)
    }

    @Test
    fun `record without notes stores none`() = runBlocking {
        val saved = RecordOxygenSaturationUseCase(repo)(profileId, 97)
        assertNull(saved.pulse)
        assertNull(saved.context)
        assertNull(saved.site)
        assertFalse(saved.oxygenGiven)
        assertNull(saved.comment)
    }

    @Test
    fun `record refuses an invalid value and stores nothing`() = runBlocking {
        val record = RecordOxygenSaturationUseCase(repo)
        assertFailsWithIllegalArgument { record(profileId, 101) }
        assertFailsWithIllegalArgument { record(profileId, 97, pulse = 20) }
        assertTrue(repo.oxygenSaturations.isEmpty())
    }

    @Test
    fun `update replaces the entry and returns null for an unknown id`() = runBlocking {
        val saved = RecordOxygenSaturationUseCase(repo)(profileId, 97)
        val update = UpdateOxygenSaturationUseCase(repo)
        val changed = update(saved.id, profileId, 93, null, OxygenContext.OTHER, OxygenSite.TOE, false, saved.timestamp)
        assertEquals(93, changed?.saturation?.value)
        assertEquals(93, repo.oxygenSaturations.single().saturation.value)
        assertNull(update(MeasurementId.generate(), profileId, 93, null, null, null, false, saved.timestamp))
        assertFailsWithIllegalArgument { update(saved.id, profileId, 101, null, null, null, false, saved.timestamp) }
        assertEquals(93, repo.oxygenSaturations.single().saturation.value)
    }

    @Test
    fun `delete removes the entry and returns false for an unknown id`() = runBlocking {
        val saved = RecordOxygenSaturationUseCase(repo)(profileId, 97)
        val delete = DeleteOxygenSaturationUseCase(repo)
        assertFalse(delete(MeasurementId.generate()))
        assertTrue(delete(saved.id))
        assertTrue(repo.oxygenSaturations.isEmpty())
    }

    @Test
    fun `history is newest first`() = runBlocking {
        val record = RecordOxygenSaturationUseCase(repo)
        record(profileId, 95, timestamp = Instant.ofEpochSecond(10))
        record(profileId, 97, timestamp = Instant.ofEpochSecond(30))
        record(profileId, 96, timestamp = Instant.ofEpochSecond(20))
        assertEquals(listOf(97, 96, 95), repo.getOxygenSaturationHistory(profileId).map { it.saturation.value })
    }

    @Test
    fun `statistics of an empty range is null`() {
        assertNull(OxygenStatistics.of(emptyList()))
    }

    @Test
    fun `statistics of one reading`() {
        val stats = OxygenStatistics.of(listOf(entry(97)))!!
        assertEquals(97, stats.lowest)
        assertEquals(97, stats.highest)
        assertEquals(BigDecimal("97.0"), stats.average)
        assertEquals(1, stats.count)
    }

    @Test
    fun `statistics lowest highest and average with one decimal`() {
        val stats = OxygenStatistics.of(listOf(entry(98), entry(96), entry(94)))!!
        assertEquals(94, stats.lowest)
        assertEquals(98, stats.highest)
        assertEquals(BigDecimal("96.0"), stats.average)
        assertEquals(3, stats.count)
    }

    @Test
    fun `statistics average rounds half up to one decimal and ties keep both counts`() {
        // 97 + 96 + 96 = 289 / 3 = 96.333...; 97 + 98 = 195 / 2 = 97.5
        assertEquals(BigDecimal("96.3"), OxygenStatistics.of(listOf(entry(97), entry(96), entry(96)))!!.average)
        assertEquals(BigDecimal("97.5"), OxygenStatistics.of(listOf(entry(97), entry(98)))!!.average)
        // 91 + 92 + 92 + 92 = 367 / 4 = 91.75 -> 91.8
        assertEquals(BigDecimal("91.8"), OxygenStatistics.of(listOf(entry(91), entry(92), entry(92), entry(92)))!!.average)
    }

    @Test
    fun `statistics count per context includes readings without a context`() {
        val stats = OxygenStatistics.of(
            listOf(
                entry(97, OxygenContext.AT_REST),
                entry(96, OxygenContext.AT_REST),
                entry(92, OxygenContext.AFTER_ACTIVITY),
                entry(95)
            )
        )!!
        assertEquals(2, stats.countPerContext[OxygenContext.AT_REST])
        assertEquals(1, stats.countPerContext[OxygenContext.AFTER_ACTIVITY])
        assertEquals(1, stats.countPerContext[null])
        assertNull(stats.countPerContext[OxygenContext.OTHER])
    }

    @Test
    fun `site and the oxygen given note change no figure`() {
        val plain = listOf(entry(97), entry(93))
        val noted = plain.map { it.copy(site = OxygenSite.EARLOBE, oxygenGiven = true) }
        assertEquals(OxygenStatistics.of(plain), OxygenStatistics.of(noted))
    }

    private fun assertFailsWithIllegalArgument(block: suspend () -> Unit) {
        try {
            runBlocking { block() }
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
