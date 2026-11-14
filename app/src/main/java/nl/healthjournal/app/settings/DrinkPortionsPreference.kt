package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.healthjournal.domain.metrics.model.DrinkPortions
import nl.healthjournal.domain.metrics.model.FluidDrink

/**
 * The person's own portion per drink, kept on the device as `water=250;coffee=125`. It is a display choice like the
 * gauge: no entry and no limit is stored here. A missing, unknown or out-of-range piece reads as that drink's
 * starting portion.
 */
class DrinkPortionsPreference(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _portions = MutableStateFlow(parse(prefs.getString(KEY_PORTIONS, null)))
    val portions: StateFlow<DrinkPortions> = _portions.asStateFlow()

    fun save(portions: DrinkPortions) {
        prefs.edit().putString(KEY_PORTIONS, encode(portions)).apply()
        _portions.value = portions
    }

    companion object {
        private const val PREFS_NAME = "fluid_portion_prefs"
        private const val KEY_PORTIONS = "fluid_portions"

        fun encode(portions: DrinkPortions): String =
            FluidDrink.entries.joinToString(";") { "${it.code}=${portions.of(it)}" }

        fun parse(stored: String?): DrinkPortions {
            val saved = stored.orEmpty().split(';').mapNotNull { piece ->
                val (code, value) = piece.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                val drink = FluidDrink.entries.firstOrNull { it.code == code.trim() } ?: return@mapNotNull null
                val ml = value.trim().toIntOrNull()?.takeIf(DrinkPortions::isValid) ?: return@mapNotNull null
                drink to ml
            }.toMap()
            return DrinkPortions(DrinkPortions.DEFAULTS + saved)
        }
    }
}
