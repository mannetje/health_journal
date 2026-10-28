package nl.healthjournal.app.ui.logging

import androidx.annotation.StringRes
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.MetricSlot

/** The full name of a metric, as the Log tabs and the metric order list show it. */
val MetricSlot.nameRes: Int
    @StringRes get() = when (this) {
        MetricSlot.WEIGHT -> R.string.log_tab_weight
        MetricSlot.BLOOD_PRESSURE -> R.string.log_tab_blood_pressure
        MetricSlot.GLUCOSE -> R.string.log_tab_glucose
        MetricSlot.MIGRAINE -> R.string.log_tab_migraine
        MetricSlot.WAIST_CIRCUMFERENCE -> R.string.log_tab_waist_circumference
        MetricSlot.ACTIVITY -> R.string.log_tab_activity
    }
