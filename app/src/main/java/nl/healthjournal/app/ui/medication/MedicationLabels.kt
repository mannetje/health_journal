package nl.healthjournal.app.ui.medication

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.Dosage
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.PillColor
import nl.healthjournal.domain.model.medication.PillShape
import nl.healthjournal.domain.model.medication.PlannedStatus
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.StrengthUnit
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// Labels always come from string resources, never from enum names, so they follow the app language.

@StringRes
fun MedicationForm.labelRes(): Int = when (this) {
    MedicationForm.TABLET -> R.string.medication_form_tablet
    MedicationForm.CAPSULE -> R.string.medication_form_capsule
    MedicationForm.LIQUID -> R.string.medication_form_liquid
    MedicationForm.DROPS -> R.string.medication_form_drops
    MedicationForm.SPRAY -> R.string.medication_form_spray
    MedicationForm.INHALER -> R.string.medication_form_inhaler
    MedicationForm.INJECTION -> R.string.medication_form_injection
    MedicationForm.PATCH -> R.string.medication_form_patch
    MedicationForm.CREAM -> R.string.medication_form_cream
    MedicationForm.OTHER -> R.string.medication_form_other
}

@StringRes
fun StrengthUnit.labelRes(): Int = when (this) {
    StrengthUnit.MG -> R.string.medication_strength_mg
    StrengthUnit.MCG -> R.string.medication_strength_mcg
    StrengthUnit.G -> R.string.medication_strength_g
    StrengthUnit.IU -> R.string.medication_strength_iu
    StrengthUnit.MG_PER_ML -> R.string.medication_strength_mg_per_ml
    StrengthUnit.MCG_PER_ML -> R.string.medication_strength_mcg_per_ml
    StrengthUnit.IU_PER_ML -> R.string.medication_strength_iu_per_ml
}

/** The unit name on its own, for the unit picker. */
@StringRes
fun DoseUnit.nameRes(): Int = when (this) {
    DoseUnit.MG -> R.string.medication_unit_mg
    DoseUnit.MCG -> R.string.medication_unit_mcg
    DoseUnit.G -> R.string.medication_unit_g
    DoseUnit.ML -> R.string.medication_unit_ml
    DoseUnit.IU -> R.string.medication_unit_iu
    DoseUnit.UNITS -> R.string.medication_unit_units
    DoseUnit.DROPS -> R.string.medication_unit_drops
    DoseUnit.PUFFS -> R.string.medication_unit_puffs
    DoseUnit.TABLETS -> R.string.medication_unit_tablets
    DoseUnit.CAPSULES -> R.string.medication_unit_capsules
    DoseUnit.PATCHES -> R.string.medication_unit_patches
    DoseUnit.APPLICATIONS -> R.string.medication_unit_applications
    DoseUnit.OTHER -> R.string.medication_unit_other
}

/** Counted units read as "1 tablet" and "2 tablets"; null for units written as a plain suffix. */
@PluralsRes
private fun DoseUnit.pluralsRes(): Int? = when (this) {
    DoseUnit.UNITS -> R.plurals.medication_dose_units
    DoseUnit.DROPS -> R.plurals.medication_dose_drops
    DoseUnit.PUFFS -> R.plurals.medication_dose_puffs
    DoseUnit.TABLETS -> R.plurals.medication_dose_tablets
    DoseUnit.CAPSULES -> R.plurals.medication_dose_capsules
    DoseUnit.PATCHES -> R.plurals.medication_dose_patches
    DoseUnit.APPLICATIONS -> R.plurals.medication_dose_applications
    else -> null
}

@StringRes
fun PillColor.labelRes(): Int = when (this) {
    PillColor.WHITE -> R.string.medication_color_white
    PillColor.YELLOW -> R.string.medication_color_yellow
    PillColor.ORANGE -> R.string.medication_color_orange
    PillColor.RED -> R.string.medication_color_red
    PillColor.PINK -> R.string.medication_color_pink
    PillColor.PURPLE -> R.string.medication_color_purple
    PillColor.BLUE -> R.string.medication_color_blue
    PillColor.GREEN -> R.string.medication_color_green
    PillColor.BROWN -> R.string.medication_color_brown
    PillColor.GREY -> R.string.medication_color_grey
}

@StringRes
fun PillShape.labelRes(): Int = when (this) {
    PillShape.ROUND -> R.string.medication_shape_round
    PillShape.OVAL -> R.string.medication_shape_oval
    PillShape.CAPSULE -> R.string.medication_shape_capsule
    PillShape.OBLONG -> R.string.medication_shape_oblong
    PillShape.SQUARE -> R.string.medication_shape_square
    PillShape.OTHER -> R.string.medication_shape_other
}

@StringRes
fun PlannedStatus.labelRes(): Int = when (this) {
    PlannedStatus.PENDING -> R.string.medication_status_pending
    PlannedStatus.MISSED -> R.string.medication_status_missed
    PlannedStatus.TAKEN -> R.string.medication_status_taken
    PlannedStatus.SKIPPED -> R.string.medication_status_skipped
}

@StringRes
fun TimeOfDay.labelRes(): Int = when (this) {
    TimeOfDay.MORNING -> R.string.medication_time_morning
    TimeOfDay.AFTERNOON -> R.string.medication_time_afternoon
    TimeOfDay.EVENING -> R.string.medication_time_evening
    TimeOfDay.NIGHT -> R.string.medication_time_night
}

@StringRes
fun DayStatus.labelRes(): Int = when (this) {
    DayStatus.COMPLETE -> R.string.medication_week_complete
    DayStatus.PARTIAL -> R.string.medication_week_partial
    DayStatus.MISSED -> R.string.medication_week_missed
    DayStatus.EMPTY -> R.string.medication_week_empty
}

/** Numbers follow the app region: a decimal comma in Dutch, a point in English. */
fun formatAmount(amount: BigDecimal): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 3 }.format(amount)

/** "2 tablets", "1,5 ml": the amount taken per intake, as entered and never converted. */
@Composable
fun doseText(amount: BigDecimal, unit: DoseUnit): String {
    val number = formatAmount(amount)
    val plurals = unit.pluralsRes()
    return when {
        plurals != null -> pluralText(plurals, if (amount.compareTo(BigDecimal.ONE) == 0) 1 else 2, number)
        unit == DoseUnit.OTHER -> stringResource(R.string.medication_dose_other, number)
        else -> stringResource(R.string.medication_amount_unit, number, stringResource(unit.nameRes()))
    }
}

@Composable
fun Dosage.doseText(): String = doseText(amountPerIntake, doseUnit)

/** "500 mg": what one unit of the medicine contains, or null when no strength was entered. */
@Composable
fun Dosage.strengthText(): String? = strength?.let {
    stringResource(R.string.medication_amount_unit, formatAmount(it.amount), stringResource(it.unit.labelRes()))
}

/** A short phrase for the schedule, for example "2 times a day, Mon, Thu" or "As needed". */
@Composable
fun Medication.frequencyText(): String {
    val schedule = schedules.last().schedule
    if (schedule !is Schedule.Recurring) return stringResource(R.string.medication_as_needed)
    val perDay = pluralText(R.plurals.medication_freq_per_day, schedule.times.size, schedule.times.size)
    return when (val days = schedule.days) {
        is DayPattern.EveryNDays ->
            "$perDay, " + pluralText(R.plurals.medication_freq_every_n_days, days.interval, days.interval)
        is DayPattern.Weekdays ->
            if (days.days.size == DayOfWeek.entries.size) perDay
            else perDay + ", " + days.days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
}

/** A plural string resource; Android picks the one/other form for the active language. */
@Composable
fun pluralText(@PluralsRes id: Int, quantity: Int, vararg args: Any): String =
    LocalContext.current.resources.getQuantityString(id, quantity, *args)
