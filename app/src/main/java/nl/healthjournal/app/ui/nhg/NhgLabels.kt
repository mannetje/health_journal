package nl.healthjournal.app.ui.nhg

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory

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
        NhgBloodPressureCategory.OPTIMAL -> R.string.nhg_bp_optimal
        NhgBloodPressureCategory.NORMAL -> R.string.nhg_bp_normal
        NhgBloodPressureCategory.HIGH_NORMAL -> R.string.nhg_bp_high_normal
        NhgBloodPressureCategory.HYPERTENSION_GRADE_1 -> R.string.nhg_bp_grade1
        NhgBloodPressureCategory.HYPERTENSION_GRADE_2 -> R.string.nhg_bp_grade2
        NhgBloodPressureCategory.HYPERTENSION_GRADE_3 -> R.string.nhg_bp_grade3
    }
)

@Composable
fun NhgGlucoseCategory.label(): String = stringResource(
    when (this) {
        NhgGlucoseCategory.HYPOGLYCAEMIA -> R.string.nhg_glucose_hypo
        NhgGlucoseCategory.NORMAL -> R.string.nhg_glucose_normal
        NhgGlucoseCategory.IMPAIRED_FASTING -> R.string.nhg_glucose_impaired_fasting
        NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE -> R.string.nhg_glucose_impaired_tolerance
        NhgGlucoseCategory.DIABETES_RANGE -> R.string.nhg_glucose_diabetes_range
    }
)

@Composable
fun GlucoseContext.label(): String = stringResource(
    when (this) {
        GlucoseContext.FASTING -> R.string.log_glucose_context_fasting
        GlucoseContext.POSTPRANDIAL -> R.string.log_glucose_context_postprandial
    }
)
