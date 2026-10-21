package nl.healthjournal.app.ui.mindfulness

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

private const val HIDE_AFTER_MILLIS = 4000L
private const val FADE_MILLIS = 600

/**
 * Whether the controls of a running exercise are showing. They fade out a few seconds after the last touch and come
 * back on the next one. A touch that brings them back does not also press them. With a screen reader on, or when
 * [alwaysVisible] is set, they never fade.
 */
class FadingControls internal constructor(private val alwaysVisible: Boolean, private val reduceMotion: Boolean) {
    internal var visible by mutableStateOf(true)
    private var touches by mutableIntStateOf(0)
    private var pressAllowed = true

    /** The alpha to draw the controls with. */
    val alpha: Float @Composable get() {
        val target = if (visible || alwaysVisible) 1f else 0f
        return animateFloatAsState(target, tween(if (reduceMotion) 0 else FADE_MILLIS), label = "controlsAlpha").value
    }

    /** True when a press on a control should be acted on, false when it only revealed the controls. */
    val canPress: Boolean get() = pressAllowed || alwaysVisible

    internal fun touched(pressed: Boolean) {
        if (pressed) pressAllowed = visible
        visible = true
        touches++
    }

    @Composable
    internal fun Schedule() {
        LaunchedEffect(touches, alwaysVisible) {
            visible = true
            if (!alwaysVisible) {
                delay(HIDE_AFTER_MILLIS)
                visible = false
            }
        }
    }

    /** Watches every touch on the screen without consuming it. */
    fun Modifier.revealOnTouch(): Modifier = pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                touched(event.changes.any { it.changedToDown() })
            }
        }
    }
}

/** Fading controls for a running exercise; [keepVisible] holds them on, for example once the exercise is done. */
@Composable
fun rememberFadingControls(keepVisible: Boolean = false): FadingControls {
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()
    val screenReader = remember {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        manager.isTouchExplorationEnabled
    }
    val controls = remember(keepVisible, screenReader, reduceMotion) { FadingControls(keepVisible || screenReader, reduceMotion) }
    controls.Schedule()
    return controls
}
