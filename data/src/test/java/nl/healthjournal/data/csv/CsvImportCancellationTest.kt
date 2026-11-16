package nl.healthjournal.data.csv

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.port.ProfileRepositoryPort
import nl.healthjournal.domain.shared.model.HeightCm
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cancelling an import must reach the caller. It is never a skipped row: the broad per-row catch in the
 * adapters used to record it as "invalid data" and carry on.
 */
class CsvImportCancellationTest {

    private class Profiles(profile: Profile) : ProfileRepositoryPort {
        private val all = mutableMapOf(profile.id to profile)
        override suspend fun save(profile: Profile) { all[profile.id] = profile }
        override suspend fun getById(id: ProfileId): Profile? = all[id]
        override suspend fun getAll(): List<Profile> = all.values.toList()
        override suspend fun getActiveProfile(): Profile? = all.values.firstOrNull()
        override suspend fun setActiveProfile(id: ProfileId) {}
    }

    /** The first save is cancelled; later saves would succeed, so a loop that carried on would show it. */
    private class CancellingWeights(private val delegate: FakeHealthLogRepository) : WeightRepositoryPort by delegate {
        var saves = 0
        override suspend fun saveWeight(entry: WeightEntry) {
            saves++
            if (saves == 1) throw CancellationException("cancelled while saving")
            delegate.saveWeight(entry)
        }
    }

    private val profile = Profile.create("Test User", LocalDate.of(1990, 1, 1), HeightCm(180))

    private fun adapter(weights: WeightRepositoryPort, other: FakeHealthLogRepository) =
        CsvDataImportAdapter(weights, other, other, other, other, other, other, Profiles(profile))

    @Test
    fun `standard CSV import rethrows cancellation and saves no later row`() = runBlocking {
        val store = FakeHealthLogRepository()
        val weights = CancellingWeights(store)
        val csv = "timestamp,weight_kg,bmi,comment\n" +
            "2026-09-20T08:00:00Z,75.0,23.1,\n" +
            "2026-09-21T08:00:00Z,74.5,23.0,\n"

        val outcome = runCatching { adapter(weights, store).importCsv(profile.id, "weight", csv) }

        assertTrue("cancellation must reach the caller", outcome.exceptionOrNull() is CancellationException)
        assertEquals("the second row must not be tried", 1, weights.saves)
        assertEquals(0, store.weights.size)
    }

    @Test
    fun `Libra CSV import rethrows cancellation and saves no later row`() = runBlocking {
        val store = FakeHealthLogRepository()
        val weights = CancellingWeights(store)
        val libra = """
            #Version: 6
            #Units: kg
            #date;weight;weight trend;body fat;body fat trend;muscle mass;body water;bone mass;comments
            2026-09-20T08:00:00.000Z;74.5;;;;;;;
            2026-09-21T08:00:00.000Z;74.2;;;;;;;
        """.trimIndent()

        val outcome = runCatching { adapter(weights, store).importCsv(profile.id, "weight", libra) }

        assertTrue("cancellation must reach the caller", outcome.exceptionOrNull() is CancellationException)
        assertEquals("the second row must not be tried", 1, weights.saves)
        assertEquals(0, store.weights.size)
    }

    @Test
    fun `an ordinary failure on a row is still a skipped row`() = runBlocking {
        val store = FakeHealthLogRepository()
        val csv = "timestamp,weight_kg,bmi,comment\n" +
            "not-a-time,75.0,23.1,\n" +
            "2026-09-21T08:00:00Z,74.5,23.0,\n"

        val result = adapter(store, store).importCsv(profile.id, "weight", csv)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.skippedRows.size)
    }
}
