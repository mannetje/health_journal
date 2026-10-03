package nl.healthjournal.app.ui.nhg

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import nl.healthjournal.app.ui.theme.RangeStep0
import nl.healthjournal.app.ui.theme.RangeStep0Dark
import nl.healthjournal.app.ui.theme.RangeStep1
import nl.healthjournal.app.ui.theme.RangeStep1Dark
import nl.healthjournal.app.ui.theme.RangeStep2
import nl.healthjournal.app.ui.theme.RangeStep2Dark
import nl.healthjournal.app.ui.theme.RangeStep3
import nl.healthjournal.app.ui.theme.RangeStep3Dark
import nl.healthjournal.app.ui.theme.RangeStep4
import nl.healthjournal.app.ui.theme.RangeStep4Dark
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory

/** Position on the neutral ramp: a higher step is a higher band, not a warning. */
internal fun NhgBmiCategory.rampStep(): Int = when (this) {
    NhgBmiCategory.UNDERWEIGHT -> 0
    NhgBmiCategory.NORMAL -> 1
    NhgBmiCategory.OVERWEIGHT -> 2
    NhgBmiCategory.OBESE -> 3
}

internal fun NhgBloodPressureCategory.rampStep(): Int = when (this) {
    NhgBloodPressureCategory.NORMAL -> 1
    NhgBloodPressureCategory.HIGH -> 3
    NhgBloodPressureCategory.SERIOUSLY_RAISED -> 4
}

internal fun NhgGlucoseCategory.rampStep(): Int = when (this) {
    NhgGlucoseCategory.HYPOGLYCAEMIA -> 0
    NhgGlucoseCategory.NORMAL -> 1
    NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE -> 2
    NhgGlucoseCategory.DIABETES_RANGE -> 3
}

@Composable
private fun rampColor(step: Int): Color {
    val dark = isSystemInDarkTheme()
    return when (step) {
        0 -> if (dark) RangeStep0Dark else RangeStep0
        1 -> if (dark) RangeStep1Dark else RangeStep1
        2 -> if (dark) RangeStep2Dark else RangeStep2
        3 -> if (dark) RangeStep3Dark else RangeStep3
        else -> if (dark) RangeStep4Dark else RangeStep4
    }
}

@Composable
fun getBmiColor(category: NhgBmiCategory): Color = rampColor(category.rampStep())

@Composable
fun getBpColor(category: NhgBloodPressureCategory): Color = rampColor(category.rampStep())

@Composable
fun getGlucoseColor(category: NhgGlucoseCategory): Color = rampColor(category.rampStep())
