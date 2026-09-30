package nl.healthjournal.domain.model.common

import java.math.BigDecimal
import java.math.RoundingMode

/** How the user wants measurements shown and typed. Storage is always metric (see ADR 0014). */
enum class UnitSystem { METRIC, IMPERIAL }

/** The blood glucose unit shown and typed. Storage is always mmol/L. */
enum class GlucoseUnit { MMOL_PER_L, MG_PER_DL }

/**
 * Pure conversions between the canonical stored units (kg, cm, km/m, mmol/L) and the units shown
 * to the user. Nothing here changes what is stored.
 */
object UnitConversion {
    private const val KG_PER_LB = 0.45359237
    private const val KM_PER_MILE = 1.609344
    private const val CM_PER_INCH = 2.54
    private const val INCHES_PER_FOOT = 12

    // Same factor the glucose value object uses for mg/dL input (mg/dL * 0.0555 = mmol/L).
    private const val MMOL_PER_MGDL = 0.0555

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB
    fun lbToKg(lb: Double): Double = lb * KG_PER_LB

    fun kmToMiles(km: Double): Double = km / KM_PER_MILE
    fun milesToKm(miles: Double): Double = miles * KM_PER_MILE

    fun mmolToMgDl(mmol: Double): Double = mmol / MMOL_PER_MGDL
    fun mgDlToMmol(mgDl: Double): Double = mgDl * MMOL_PER_MGDL

    /** Splits a stored height into whole feet and inches, rounded to the nearest inch. */
    fun cmToFeetInches(cm: Int): Pair<Int, Int> {
        val totalInches = Math.round(cm / CM_PER_INCH).toInt()
        return totalInches / INCHES_PER_FOOT to totalInches % INCHES_PER_FOOT
    }

    fun feetInchesToCm(feet: Int, inches: Int): Int =
        Math.round((feet * INCHES_PER_FOOT + inches) * CM_PER_INCH).toInt()

    /** Rounds a converted kg value to the 0.01 kg precision used for storage. */
    fun toStoredKg(kg: Double): BigDecimal = BigDecimal.valueOf(kg).setScale(2, RoundingMode.HALF_UP)
}
