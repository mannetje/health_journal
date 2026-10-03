package nl.healthjournal.app.ui.nhg

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.formatDecimal
import nl.healthjournal.app.ui.common.glucoseDigits
import nl.healthjournal.app.ui.common.glucoseFromMmol
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory

// Every label is "name · range" and never names a condition (range-labels).

@Composable
fun NhgBmiCategory.label(): String = stringResource(
    when (this) {
        NhgBmiCategory.UNDERWEIGHT -> R.string.nhg_bmi_underweight
        NhgBmiCategory.NORMAL -> R.string.nhg_bmi_normal
        NhgBmiCategory.OVERWEIGHT -> R.string.nhg_bmi_overweight
        NhgBmiCategory.OBESE -> R.string.nhg_bmi_obese
    }
)

@Composable
fun NhgBloodPressureCategory.label(): String = stringResource(
    when (this) {
        NhgBloodPressureCategory.NORMAL -> R.string.nhg_bp_normal
        NhgBloodPressureCategory.HIGH -> R.string.nhg_bp_high
        NhgBloodPressureCategory.SERIOUSLY_RAISED -> R.string.nhg_bp_seriously_raised
    }
)

/** Name plus only the range of the given [context], shown in the glucose unit the user picked. */
@Composable
fun NhgGlucoseCategory.label(context: GlucoseContext): String {
    val units = LocalDisplayUnits.current
    fun shown(mmol: Double) = formatDecimal(units.glucoseFromMmol(mmol), units.glucoseDigits)
    val name = stringResource(
        when (this) {
            NhgGlucoseCategory.HYPOGLYCAEMIA -> R.string.nhg_glucose_name_low
            NhgGlucoseCategory.NORMAL -> R.string.nhg_glucose_name_normal
            NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE ->
                R.string.nhg_glucose_name_slightly_raised
            NhgGlucoseCategory.DIABETES_RANGE -> R.string.nhg_glucose_name_high
        }
    )
    val ctx = stringResource(
        if (context == GlucoseContext.FASTING) R.string.nhg_context_fasting else R.string.nhg_context_postprandial
    )
    val unit = units.glucoseSymbol
    val range = glucoseRange(this, context)
    val rangeText = when (range.shape) {
        RangeShape.BELOW -> stringResource(R.string.nhg_glucose_range_below, ctx, shown(range.low), unit)
        RangeShape.BETWEEN ->
            stringResource(R.string.nhg_glucose_range_between, ctx, shown(range.low), shown(range.high!!), unit)
        RangeShape.BETWEEN_BELOW ->
            stringResource(R.string.nhg_glucose_range_between_below, ctx, shown(range.low), shown(range.high!!), unit)
        RangeShape.ABOVE_TO ->
            stringResource(R.string.nhg_glucose_range_above_to, ctx, shown(range.low), shown(range.high!!), unit)
        RangeShape.ABOVE -> stringResource(R.string.nhg_glucose_range_above, ctx, shown(range.low), unit)
    }
    return stringResource(R.string.nhg_glucose_label, name, rangeText)
}

@Composable
fun GlucoseContext.label(): String = stringResource(
    when (this) {
        GlucoseContext.FASTING -> R.string.log_glucose_context_fasting
        GlucoseContext.POSTPRANDIAL -> R.string.log_glucose_context_postprandial
    }
)
