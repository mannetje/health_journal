package nl.healthjournal.app.ui.logging.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import nl.healthjournal.app.R

/**
 * Makes a scrolling picker one adjustable control for a screen reader, like a slider. The tape or the rows of
 * numbers stay visual only: reading them aloud would announce hundreds of ticks. The control has a name, a
 * current value and a "not set yet" note, moves by one step (swipe up or down, or the volume keys) and has two
 * actions that move by [bigStep], which a long range needs.
 *
 * [onSet] gets the requested value, already limited to [range]; the caller rounds it to its own step.
 */
@Composable
fun Modifier.adjustableValue(
    label: String,
    valueText: String,
    isSet: Boolean,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    bigStep: Float,
    bigStepText: String,
    onSet: (Float) -> Unit
): Modifier {
    val description = if (isSet) valueText else stringResource(R.string.log_picker_state, valueText, stringResource(R.string.log_picker_not_set))
    val increase = stringResource(R.string.log_picker_increase_by, bigStepText)
    val decrease = stringResource(R.string.log_picker_decrease_by, bigStepText)
    fun move(target: Float): Boolean {
        onSet(target.coerceIn(range.start, range.endInclusive))
        return true
    }
    return clearAndSetSemantics {
        contentDescription = label
        stateDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(range.start, range.endInclusive), range, steps)
        setProgress { move(it) }
        customActions = listOf(
            CustomAccessibilityAction(increase) { move(value + bigStep) },
            CustomAccessibilityAction(decrease) { move(value - bigStep) }
        )
    }
}
