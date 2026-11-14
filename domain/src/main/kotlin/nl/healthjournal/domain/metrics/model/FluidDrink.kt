package nl.healthjournal.domain.metrics.model

/**
 * The drink of an entry. A label only: every drink counts equally towards the total and carries no caffeine, sugar,
 * alcohol or nutrition attribute (ADR 0030). A missing or unknown stored code reads as [OTHER].
 */
enum class FluidDrink(val code: String) {
    WATER("water"),
    COFFEE("coffee"),
    TEA("tea"),
    MILK("milk"),
    JUICE("juice"),
    SOUP("soup"),
    OTHER("other");

    companion object {
        fun fromCode(code: String?): FluidDrink = entries.firstOrNull { it.code == code?.trim()?.lowercase() } ?: OTHER
    }
}

/** The person's own portion per drink in ml, with the starting values of ADR 0030. */
data class DrinkPortions(private val portions: Map<FluidDrink, Int> = DEFAULTS) {
    fun of(drink: FluidDrink): Int = portions[drink] ?: DEFAULTS.getValue(drink)

    fun with(drink: FluidDrink, ml: Int): DrinkPortions = DrinkPortions(portions + (drink to ml))

    companion object {
        val DEFAULTS: Map<FluidDrink, Int> = mapOf(
            FluidDrink.WATER to 250, FluidDrink.COFFEE to 125, FluidDrink.TEA to 125, FluidDrink.MILK to 200,
            FluidDrink.JUICE to 150, FluidDrink.SOUP to 200, FluidDrink.OTHER to 200
        )
    }
}

/** Plain ml per drink over a set of entries, largest first, ties in the fixed drink order. */
data class DrinkTotal(val drink: FluidDrink, val ml: Int)

object FluidDrinkTotals {
    fun of(entries: List<FluidIntakeEntry>): List<DrinkTotal> =
        entries.groupBy { it.drink }
            .map { (drink, list) -> DrinkTotal(drink, list.sumOf { it.amount.value }) }
            .sortedWith(compareByDescending<DrinkTotal> { it.ml }.thenBy { it.drink.ordinal })
}
