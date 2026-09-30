package nl.healthjournal.app.ui.common

import androidx.compose.runtime.compositionLocalOf
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.domain.model.common.GlucoseUnit
import nl.healthjournal.domain.model.common.UnitConversion
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/** The units currently shown and typed, provided once at the top of the composition. */
val LocalDisplayUnits = compositionLocalOf { DisplayUnits.DEFAULT }

/** Accepts a decimal comma as well as a point, since keyboards follow the region. */
fun String.parseDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Formats [value] with a fixed number of decimals using the active region's separator. */
fun formatDecimal(value: Double, digits: Int): String =
    String.format(Locale.getDefault(), "%.${digits}f", value)

/** Text for an input field: rounded, no trailing zeros, and the region's decimal separator. */
fun formatInput(value: Double, digits: Int): String {
    val plain = BigDecimal.valueOf(value).setScale(digits, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return plain.replace('.', DecimalFormatSymbols.getInstance().decimalSeparator)
}

// Weight (stored as kg)
fun DisplayUnits.weightFromKg(kg: Double): Double = if (isImperial) UnitConversion.kgToLb(kg) else kg
fun DisplayUnits.weightToKg(shown: Double): Double = if (isImperial) UnitConversion.lbToKg(shown) else shown
fun DisplayUnits.formatWeight(kg: Double): String = "${formatDecimal(weightFromKg(kg), 1)} $weightSymbol"

// Distance (stored as meters)
fun DisplayUnits.distanceFromMeters(meters: Double): Double =
    if (isImperial) UnitConversion.kmToMiles(meters / 1000.0) else meters / 1000.0
fun DisplayUnits.distanceToMeters(shown: Double): Double =
    (if (isImperial) UnitConversion.milesToKm(shown) else shown) * 1000.0
fun DisplayUnits.formatDistance(meters: Double): String =
    "${formatDecimal(distanceFromMeters(meters), 2)} $distanceSymbol"

// Glucose (stored as mmol/L)
private val DisplayUnits.mgDl: Boolean get() = glucose == GlucoseUnit.MG_PER_DL
val DisplayUnits.glucoseDigits: Int get() = if (mgDl) 0 else 1
fun DisplayUnits.glucoseFromMmol(mmol: Double): Double = if (mgDl) UnitConversion.mmolToMgDl(mmol) else mmol
fun DisplayUnits.glucoseToMmol(shown: Double): Double = if (mgDl) UnitConversion.mgDlToMmol(shown) else shown
fun DisplayUnits.formatGlucose(mmol: Double): String =
    "${formatDecimal(glucoseFromMmol(mmol), glucoseDigits)} $glucoseSymbol"

/** Formats a stored height for display, for example "178 cm" or "5 ft 10 in". */
fun DisplayUnits.formatHeight(cm: Int): String =
    if (isImperial) {
        val (feet, inches) = UnitConversion.cmToFeetInches(cm)
        "$feet ft $inches in"
    } else {
        "$cm cm"
    }

/** Formats a duration in seconds as whole minutes for History cards. */
fun formatMinutes(seconds: Long): String = "${Math.round(seconds / 60.0)} min"
