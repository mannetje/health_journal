package nl.healthjournal.domain.model.medication

import java.math.BigDecimal

/** Free-text name chosen by the user: trimmed, 1 to [MAX_LENGTH] characters. No suggestions or lookups exist. */
@JvmInline
value class MedicationName(val value: String) {
    init {
        require(value.isNotBlank()) { "Name must not be blank" }
        require(value == value.trim()) { "Name must be trimmed" }
        require(value.length <= MAX_LENGTH) { "Name must be at most $MAX_LENGTH characters, got: ${value.length}" }
    }

    companion object {
        const val MAX_LENGTH = 80

        fun of(raw: String): MedicationName = MedicationName(raw.trim())
    }
}

/** What a unit of the medicine contains (for example 500 mg per tablet). Stored as entered, never converted. */
enum class StrengthUnit { MG, MCG, G, IU, MG_PER_ML, MCG_PER_ML, IU_PER_ML }

/** How much is taken per intake. Stored as entered, never converted (no mg to ml, no unit to mg). */
enum class DoseUnit { MG, MCG, G, ML, IU, UNITS, DROPS, PUFFS, TABLETS, CAPSULES, PATCHES, APPLICATIONS, OTHER }

enum class MedicationForm { TABLET, CAPSULE, LIQUID, DROPS, SPRAY, INHALER, INJECTION, PATCH, CREAM, OTHER }

data class Strength(val amount: BigDecimal, val unit: StrengthUnit) {
    init {
        require(amount > BigDecimal.ZERO) { "Strength must be greater than 0, got: $amount" }
        require(amount <= MAX_AMOUNT) { "Strength must be at most $MAX_AMOUNT, got: $amount" }
    }

    companion object {
        val MAX_AMOUNT: BigDecimal = BigDecimal(100_000)
    }
}

/** Strength of the medicine and the amount taken per intake; the two units are independent. */
data class Dosage(
    val strength: Strength?,
    val amountPerIntake: BigDecimal,
    val doseUnit: DoseUnit
) {
    init {
        requireValidAmount(amountPerIntake)
    }

    companion object {
        val MAX_AMOUNT: BigDecimal = BigDecimal(1000)

        /** A dose amount, planned or actual, is greater than 0 and at most [MAX_AMOUNT]. */
        fun requireValidAmount(amount: BigDecimal) {
            require(amount > BigDecimal.ZERO) { "Dose must be greater than 0, got: $amount" }
            require(amount <= MAX_AMOUNT) { "Dose must be at most $MAX_AMOUNT, got: $amount" }
        }
    }
}

enum class PillColor { WHITE, YELLOW, ORANGE, RED, PINK, PURPLE, BLUE, GREEN, BROWN, GREY }

enum class PillShape { ROUND, OVAL, CAPSULE, OBLONG, SQUARE, OTHER }

/** Chosen by the user to recognise the medication. Colour is never the only identifier in the UI. */
data class PillAppearance(val color: PillColor, val shape: PillShape)
