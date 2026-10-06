package nl.healthjournal.domain.usecase

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.model.metrics.GlucoseContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class EntryCommentUseCasesTest {

    private lateinit var profileRepo: FakeProfileRepository
    private lateinit var healthRepo: FakeHealthLogRepository

    @Before
    fun setup() {
        profileRepo = FakeProfileRepository()
        healthRepo = FakeHealthLogRepository()
    }

    private suspend fun profileId() = CreateProfileUseCase(profileRepo)("Test", LocalDate.of(1990, 1, 1), 180)

    @Test
    fun `record stores a normalised comment on every entry type`() = runBlocking {
        val id = profileId()
        val start = Instant.parse("2026-01-01T08:00:00Z")

        RecordWeightUseCase(healthRepo, profileRepo)(id, BigDecimal("80.0"), comment = "  a\nb ")
        RecordBloodPressureUseCase(healthRepo)(id, 120, 80, 70, comment = "bp")
        RecordGlucoseUseCase(healthRepo)(id, GlucoseContext.FASTING, valueInMmolL = BigDecimal("5.5"), comment = "glucose")
        RecordWaistCircumferenceUseCase(healthRepo, profileRepo)(id, 85.0, comment = "waist")
        RecordActivityUseCase(healthRepo)(id, start, start.plusSeconds(600), 1000.0, comment = "activity")

        assertEquals("a b", healthRepo.weights.single().comment?.text)
        assertEquals("bp", healthRepo.bloodPressures.single().comment?.text)
        assertEquals("glucose", healthRepo.glucoses.single().comment?.text)
        assertEquals("waist", healthRepo.waistCircumferences.single().comment?.text)
        assertEquals("activity", healthRepo.activities.single().comment?.text)
    }

    @Test
    fun `record without a comment stores none`() = runBlocking {
        RecordWeightUseCase(healthRepo, profileRepo)(profileId(), BigDecimal("80.0"))
        assertNull(healthRepo.weights.single().comment)
    }

    @Test
    fun `record rejects a comment over 200 characters`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { RecordWeightUseCase(healthRepo, profileRepo)(profileId(), BigDecimal("80.0"), comment = "a".repeat(201)) }
        }
    }

    @Test
    fun `update replaces and clears the comment`() = runBlocking {
        val id = profileId()
        val original = RecordWeightUseCase(healthRepo, profileRepo)(id, BigDecimal("80.0"), comment = "before")
        val update = UpdateWeightUseCase(healthRepo, profileRepo)

        update(original.id, id, BigDecimal("80.0"), original.timestamp, comment = "after")
        assertEquals("after", healthRepo.weights.single().comment?.text)

        update(original.id, id, BigDecimal("80.0"), original.timestamp, comment = null)
        assertNull(healthRepo.weights.single().comment)
    }
}
