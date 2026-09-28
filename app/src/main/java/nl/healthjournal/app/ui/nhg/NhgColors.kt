package nl.healthjournal.app.ui.nhg

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import nl.healthjournal.app.ui.theme.NhgDeepOrange
import nl.healthjournal.app.ui.theme.NhgDeepOrangeDark
import nl.healthjournal.app.ui.theme.NhgNormalGreen
import nl.healthjournal.app.ui.theme.NhgNormalGreenDark
import nl.healthjournal.app.ui.theme.NhgOptimalGreen
import nl.healthjournal.app.ui.theme.NhgOptimalGreenDark
import nl.healthjournal.app.ui.theme.NhgOrange
import nl.healthjournal.app.ui.theme.NhgOrangeDark
import nl.healthjournal.app.ui.theme.NhgRed
import nl.healthjournal.app.ui.theme.NhgRedDark
import nl.healthjournal.app.ui.theme.NhgSevereRed
import nl.healthjournal.app.ui.theme.NhgSevereRedDark
import nl.healthjournal.app.ui.theme.NhgWarningYellow
import nl.healthjournal.app.ui.theme.NhgWarningYellowDark
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgBmiCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory

@Composable
fun getBmiColor(category: NhgBmiCategory): Color {
    val dark = isSystemInDarkTheme()
    return when (category) {
        NhgBmiCategory.NORMAL -> if (dark) NhgOptimalGreenDark else NhgOptimalGreen
        NhgBmiCategory.UNDERWEIGHT -> if (dark) NhgWarningYellowDark else NhgWarningYellow
        NhgBmiCategory.OVERWEIGHT -> if (dark) NhgOrangeDark else NhgOrange
        NhgBmiCategory.OBESE -> if (dark) NhgRedDark else NhgRed
    }
}

@Composable
fun getBpColor(category: NhgBloodPressureCategory): Color {
    val dark = isSystemInDarkTheme()
    return when (category) {
        NhgBloodPressureCategory.OPTIMAL -> if (dark) NhgOptimalGreenDark else NhgOptimalGreen
        NhgBloodPressureCategory.NORMAL -> if (dark) NhgNormalGreenDark else NhgNormalGreen
        NhgBloodPressureCategory.HIGH_NORMAL -> if (dark) NhgWarningYellowDark else NhgWarningYellow
        NhgBloodPressureCategory.HYPERTENSION_GRADE_1 -> if (dark) NhgOrangeDark else NhgOrange
        NhgBloodPressureCategory.HYPERTENSION_GRADE_2 -> if (dark) NhgDeepOrangeDark else NhgDeepOrange
        NhgBloodPressureCategory.HYPERTENSION_GRADE_3 -> if (dark) NhgSevereRedDark else NhgSevereRed
    }
}

@Composable
fun getGlucoseColor(category: NhgGlucoseCategory): Color {
    val dark = isSystemInDarkTheme()
    return when (category) {
        NhgGlucoseCategory.NORMAL -> if (dark) NhgOptimalGreenDark else NhgOptimalGreen
        NhgGlucoseCategory.IMPAIRED_FASTING, NhgGlucoseCategory.IMPAIRED_GLUCOSE_TOLERANCE -> if (dark) NhgOrangeDark else NhgOrange
        NhgGlucoseCategory.HYPOGLYCAEMIA, NhgGlucoseCategory.DIABETES_RANGE -> if (dark) NhgRedDark else NhgRed
    }
}
