package nl.healthjournal.domain.metrics.usecase

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.metrics.model.FhirExportMetric
import nl.healthjournal.domain.metrics.port.FhirExportSourcePort
import nl.healthjournal.domain.metrics.usecase.GetFhirExportDataUseCase
import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.BloodPressureReading
import nl.healthjournal.domain.metrics.model.NhgBloodPressureCategory
import nl.healthjournal.domain.metrics.model.WeightEntry
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.model.Sex
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.shared.model.WeightKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class GetFhirExportDataUseCaseTest {

    private val profileId = ProfileId.generate()
    private val t1 = Instant.parse("2026-11-01T10:00:00Z")
    private val t2 = Instant.parse("2026-11-10T10:00:00Z")
    private val t3 = Instant.parse("2026-11-20T10:00:00Z")

    private val sampleProfile = Profile.create(
        name = "Test User",
        dateOfBirth = LocalDate.of(1985, 5, 20),
        sex = Sex.FEMALE,
        id = profileId
    )

    private val sampleWeights = listOf(
        WeightEntry(MeasurementId.generate(), profileId, t3, WeightKg(BigDecimal("72.0"))),
        WeightEntry(MeasurementId.generate(), profileId, t1, WeightKg(BigDecimal("70.0"))),
        WeightEntry(MeasurementId.generate(), profileId, t2, WeightKg(BigDecimal("71.0")))
    )

    private val sampleBloodPressures = listOf(
        BloodPressureEntry(MeasurementId.generate(), profileId, t2, BloodPressureReading(120, 80, 65), NhgBloodPressureCategory.NORMAL),
        BloodPressureEntry(MeasurementId.generate(), profileId, t1, BloodPressureReading(118, 78, 62), NhgBloodPressureCategory.NORMAL)
    )

    private val fakePort = object : FhirExportSourcePort {
        override suspend fun getWeights(profileId: ProfileId): List<WeightEntry> = sampleWeights
        override suspend fun getBloodPressures(profileId: ProfileId): List<BloodPressureEntry> = sampleBloodPressures
        override suspend fun getProfile(profileId: ProfileId): Profile? = sampleProfile
    }

    private val useCase = GetFhirExportDataUseCase(fakePort)

    @Test
    fun `all entries are returned sorted oldest first when range is unrestricted`() = runBlocking {
        val result = useCase(profileId)
        assertEquals(3, result.weights.size)
        assertEquals(t1, result.weights[0].timestamp)
        assertEquals(t2, result.weights[1].timestamp)
        assertEquals(t3, result.weights[2].timestamp)

        assertEquals(2, result.bloodPressures.size)
        assertEquals(t1, result.bloodPressures[0].timestamp)
        assertEquals(t2, result.bloodPressures[1].timestamp)

        assertNotNull(result.profile)
        assertEquals(sampleProfile.id, result.profile?.id)
    }

    @Test
    fun `entries are filtered by date range`() = runBlocking {
        val result = useCase(profileId, from = t2, to = t3)
        assertEquals(2, result.weights.size)
        assertEquals(t2, result.weights[0].timestamp)
        assertEquals(t3, result.weights[1].timestamp)

        assertEquals(1, result.bloodPressures.size)
        assertEquals(t2, result.bloodPressures[0].timestamp)
    }

    @Test
    fun `entries are filtered by metric set`() = runBlocking {
        val weightOnly = useCase(profileId, metrics = setOf(FhirExportMetric.WEIGHT))
        assertEquals(3, weightOnly.weights.size)
        assertEquals(0, weightOnly.bloodPressures.size)

        val bpOnly = useCase(profileId, metrics = setOf(FhirExportMetric.BLOOD_PRESSURE))
        assertEquals(0, bpOnly.weights.size)
        assertEquals(2, bpOnly.bloodPressures.size)
    }
}
