package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What fills with today's total on the Fluid tab. Only the look changes; the numbers and the label stay the same. */
enum class FluidGaugeStyle { GLASS, WOMAN, MAN }

/** The person's choice of gauge, kept on the device. A missing or unknown value reads as the glass. */
class FluidGaugePreference(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _style = MutableStateFlow(parse(prefs.getString(KEY_STYLE, null)))
    val style: StateFlow<FluidGaugeStyle> = _style.asStateFlow()

    fun save(style: FluidGaugeStyle) {
        prefs.edit().putString(KEY_STYLE, style.name).apply()
        _style.value = style
    }

    companion object {
        private const val PREFS_NAME = "fluid_gauge_prefs"
        private const val KEY_STYLE = "fluid_gauge_style"

        fun parse(stored: String?): FluidGaugeStyle =
            FluidGaugeStyle.entries.firstOrNull { it.name == stored } ?: FluidGaugeStyle.GLASS
    }
}
