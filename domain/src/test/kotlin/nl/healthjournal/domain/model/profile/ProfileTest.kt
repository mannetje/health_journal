package nl.healthjournal.domain.model.profile

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.metrics.WeightKg
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ProfileTest {

    @Test
    fun `create valid profile`() {
        val dob = LocalDate.of(1990, 5, 15)
        val profile = Profile.create(
            name = "Jan de Vries",
            dateOfBirth = dob,
            height = HeightCm(182)
        )

        assertEquals("Jan de Vries", profile.name)
        assertEquals(dob, profile.dateOfBirth)
        assertEquals(182, profile.height?.value)
        assertNotNull(profile.id)
    }

    @Test
    fun `reject blank name`() {
        assertThrows(IllegalArgumentException::class.java) {
            Profile.create(
                name = "   ",
                dateOfBirth = LocalDate.of(1990, 1, 1)
            )
        }
    }

    @Test
    fun `reject future date of birth`() {
        assertThrows(IllegalArgumentException::class.java) {
            Profile.create(
                name = "Test",
                dateOfBirth = LocalDate.now().plusDays(1)
            )
        }
    }

    @Test
    fun `reject today date of birth`() {
        assertThrows(IllegalArgumentException::class.java) {
            Profile.create(
                name = "Test",
                dateOfBirth = LocalDate.now()
            )
        }
    }

    @Test
    fun `reject older than 130 years`() {
        assertThrows(IllegalArgumentException::class.java) {
            Profile.create(
                name = "Ancient",
                dateOfBirth = LocalDate.now().minusYears(131)
            )
        }
    }

    @Test
    fun `calculate BMI when height is present`() {
        val profile = Profile.create(
            name = "Test",
            dateOfBirth = LocalDate.of(1985, 3, 10),
            height = HeightCm(180) // 1.80m
        )
        // 1.8 * 1.8 = 3.24
        // 81.0 / 3.24 = 25.0
        val bmi = profile.calculateBmi(WeightKg(BigDecimal("81.0")))
        assertNotNull(bmi)
        assertEquals(BigDecimal("25.0"), bmi?.bmi)
    }

    @Test
    fun `calculate BMI returns null when height is absent`() {
        val profile = Profile.create(
            name = "Test",
            dateOfBirth = LocalDate.of(1985, 3, 10),
            height = null
        )
        val bmi = profile.calculateBmi(WeightKg(BigDecimal("81.0")))
        assertNull(bmi)
    }
}
