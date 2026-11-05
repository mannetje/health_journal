package nl.healthjournal.app.ui.logging

import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.distanceToMeters
import nl.healthjournal.app.ui.common.parseDecimal
import nl.healthjournal.app.ui.common.weightToKg
import nl.healthjournal.domain.metrics.command.RecordActivityCommand
import nl.healthjournal.domain.metrics.command.RecordBloodPressureCommand
import nl.healthjournal.domain.metrics.command.RecordGlucoseCommand
import nl.healthjournal.domain.metrics.command.RecordOxygenSaturationCommand
import nl.healthjournal.domain.metrics.command.RecordWaistCircumferenceCommand
import nl.healthjournal.domain.metrics.command.RecordWeightCommand
import nl.healthjournal.domain.shared.model.GlucoseUnit
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.shared.model.UnitConversion
import java.math.BigDecimal
import java.time.Instant

/**
 * Turns the text and picker values of the Log form into the domain command, or into the message to show when the
 * text is not a number at all. These functions are pure: no state, no dispatcher, so each one can be tested alone.
 * The rules (ranges, comment length) belong to the domain; only unparsable text is rejected here.
 */
internal sealed interface FormResult<out C> {
    data class Ready<C>(val command: C) : FormResult<C>
    data class Unreadable(val message: UiText) : FormResult<Nothing>
}

/** The pulse only counts when the person set it; the starting position is never recorded. */
internal fun LoggingUiState.chosenPulse(): Int? = pulseValue.takeIf { LogField.PULSE in chosen }

internal fun weightCommand(profileId: ProfileId, state: LoggingUiState, units: DisplayUnits): FormResult<RecordWeightCommand> {
    val weightKg = state.weightInput.parseDecimal()?.let { units.weightToKg(it) }
        ?: return FormResult.Unreadable(UiText.Res(R.string.log_err_weight, units.weightSymbol))
    return FormResult.Ready(RecordWeightCommand(profileId, UnitConversion.toStoredKg(weightKg), comment = state.commentInput))
}

internal fun bloodPressureCommand(profileId: ProfileId, state: LoggingUiState): FormResult<RecordBloodPressureCommand> {
    val systolic = state.systolicInput.toIntOrNull() ?: return FormResult.Unreadable(UiText.Res(R.string.log_err_systolic))
    val diastolic = state.diastolicInput.toIntOrNull() ?: return FormResult.Unreadable(UiText.Res(R.string.log_err_diastolic))
    return FormResult.Ready(RecordBloodPressureCommand(profileId, systolic, diastolic, state.chosenPulse(), comment = state.commentInput))
}

internal fun glucoseCommand(profileId: ProfileId, state: LoggingUiState, units: DisplayUnits): FormResult<RecordGlucoseCommand> {
    val value = state.glucoseInput.parseDecimal()?.let { BigDecimal.valueOf(it) }
        ?: return FormResult.Unreadable(UiText.Res(R.string.log_err_glucose))
    val command = if (units.glucose == GlucoseUnit.MG_PER_DL) {
        RecordGlucoseCommand(profileId, state.glucoseContext, valueInMgDl = value, comment = state.commentInput)
    } else {
        RecordGlucoseCommand(profileId, state.glucoseContext, valueInMmolL = value, comment = state.commentInput)
    }
    return FormResult.Ready(command)
}

/** The activity ends at [now] and started [LoggingUiState.activityDurationInput] minutes before. */
internal fun activityCommand(
    profileId: ProfileId,
    state: LoggingUiState,
    units: DisplayUnits,
    now: Instant
): FormResult<RecordActivityCommand> {
    val durationMinutes = state.activityDurationInput.parseDecimal()?.takeIf { it > 0 }
        ?: return FormResult.Unreadable(UiText.Res(R.string.log_err_duration))
    val distanceMeters = state.activityDistanceInput.parseDecimal()
        ?.takeIf { it >= 0 }
        ?.let { units.distanceToMeters(it) }
        ?: return FormResult.Unreadable(UiText.Res(R.string.log_err_distance, units.distanceSymbol))
    val startTime = now.minusSeconds((durationMinutes * 60).toLong())
    return FormResult.Ready(RecordActivityCommand(profileId, startTime, now, distanceMeters, comment = state.commentInput))
}

internal fun waistCommand(profileId: ProfileId, state: LoggingUiState): FormResult<RecordWaistCircumferenceCommand> {
    val cm = state.waistInput.toDoubleOrNull() ?: return FormResult.Unreadable(UiText.Res(R.string.log_err_waist))
    return FormResult.Ready(RecordWaistCircumferenceCommand(profileId, cm, comment = state.commentInput))
}

internal fun oxygenCommand(profileId: ProfileId, state: LoggingUiState): RecordOxygenSaturationCommand =
    RecordOxygenSaturationCommand(
        profileId = profileId,
        percent = state.oxygenPercentValue,
        pulse = state.oxygenPulseInput.takeIf { it.isNotEmpty() }?.toIntOrNull(),
        context = state.oxygenContext,
        site = state.oxygenSite,
        oxygenGiven = state.oxygenGiven,
        comment = state.commentInput
    )

/** The command when the text was readable; otherwise [unreadable] gets the message (usually a `return it`). */
internal inline fun <C> FormResult<C>.orReturn(unreadable: (UiText) -> Nothing): C = when (this) {
    is FormResult.Ready -> command
    is FormResult.Unreadable -> unreadable(message)
}
