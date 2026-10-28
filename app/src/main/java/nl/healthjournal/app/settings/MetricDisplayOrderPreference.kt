package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The order of the metrics on the Log tabs and History chips; one order for both screens. */
class MetricDisplayOrderPreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _order = MutableStateFlow(MetricSlot.parseOrder(prefs.getString(KEY_ORDER, null)))

    /** The current order; collectors see a change at once, without reloading. */
    val order: StateFlow<List<MetricSlot>> = _order.asStateFlow()

    fun save(order: List<MetricSlot>) {
        val complete = MetricSlot.complete(order)
        prefs.edit().putString(KEY_ORDER, MetricSlot.serialiseOrder(complete)).apply()
        _order.value = complete
    }

    companion object {
        private const val PREFS_NAME = "metric_order_prefs"
        private const val KEY_ORDER = "metric_display_order"
    }
}
