package nl.healthjournal.domain.model.profile

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.metrics.WeightKg
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class BmiResult(
    val bmi: BigDecimal
)

class Profile private constructor(
    val id: ProfileId,
    val name: String,
    val dateOfBirth: LocalDate,
    val height: HeightCm?
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
        require(dateOfBirth.isBefore(LocalDate.now())) {
            "Date of birth ($dateOfBirth) must be in the past"
        }
        require(dateOfBirth.isAfter(LocalDate.now().minusYears(MAX_AGE_YEARS))) {
            "Date of birth ($dateOfBirth) cannot be older than $MAX_AGE_YEARS years"
        }
    }

    fun updateName(newName: String): Profile {
        return Profile(id = this.id, name = newName.trim(), dateOfBirth = this.dateOfBirth, height = this.height)
    }

    fun updateHeight(newHeight: HeightCm?): Profile {
        return Profile(id = this.id, name = this.name, dateOfBirth = this.dateOfBirth, height = newHeight)
    }

    /**
     * Derives BMI as: weight(kg) / (height(m))^2, rounded to 1 decimal place.
     * Returns null if height is not configured.
     */
    fun calculateBmi(weight: WeightKg): BmiResult? {
        val h = height ?: return null
        val heightInMeters = BigDecimal.valueOf(h.value.toLong()).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val heightSquared = heightInMeters.multiply(heightInMeters)
        val bmiValue = weight.value.divide(heightSquared, 1, RoundingMode.HALF_UP)
        return BmiResult(bmiValue)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Profile) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Profile(id=$id, name='$name', dateOfBirth=$dateOfBirth, height=$height)"

    companion object {
        const val MAX_AGE_YEARS = 130L

        fun create(
            name: String,
            dateOfBirth: LocalDate,
            height: HeightCm? = null,
            id: ProfileId = ProfileId.generate()
        ): Profile = Profile(
            id = id,
            name = name.trim(),
            dateOfBirth = dateOfBirth,
            height = height
        )

        fun reconstruct(
            id: ProfileId,
            name: String,
            dateOfBirth: LocalDate,
            height: HeightCm?
        ): Profile = Profile(
            id = id,
            name = name,
            dateOfBirth = dateOfBirth,
            height = height
        )
    }
}
