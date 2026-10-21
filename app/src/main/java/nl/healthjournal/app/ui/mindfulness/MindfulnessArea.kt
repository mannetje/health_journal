package nl.healthjournal.app.ui.mindfulness

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import nl.healthjournal.domain.model.mindfulness.BreathingDuration
import nl.healthjournal.domain.model.mindfulness.BreathingPatternId
import nl.healthjournal.domain.model.mindfulness.BreathingPatterns
import nl.healthjournal.domain.model.mindfulness.BreathingTimer
import nl.healthjournal.domain.model.mindfulness.GuidedExerciseCatalogue

private enum class MindfulnessRoute { HOME, BREATHE_SETUP, BREATHE_RUN, GUIDED_LIST, GUIDED_PLAY }

/**
 * The Mindfulness area: home, the breathing setup and the running exercise. Back goes one step up;
 * the area's own state is only the current screen and the last choices on the setup screen.
 */
@Composable
fun MindfulnessArea() {
    var route by rememberSaveable { mutableStateOf(MindfulnessRoute.HOME) }
    var patternId by rememberSaveable { mutableStateOf(BreathingPatternId.BELLY) }
    var duration by rememberSaveable { mutableStateOf(BreathingDuration.THREE_MINUTES) }
    var visual by rememberSaveable { mutableStateOf(BreathingVisual.LOTUS) }
    var guidedId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = route == MindfulnessRoute.BREATHE_SETUP || route == MindfulnessRoute.GUIDED_LIST) { route = MindfulnessRoute.HOME }

    when (route) {
        MindfulnessRoute.HOME -> MindfulnessHomeScreen(
            onBreathe = { route = MindfulnessRoute.BREATHE_SETUP },
            onGuided = { route = MindfulnessRoute.GUIDED_LIST },
        )
        MindfulnessRoute.GUIDED_LIST -> GuidedListScreen(onOpen = { guidedId = it.id; route = MindfulnessRoute.GUIDED_PLAY })
        MindfulnessRoute.GUIDED_PLAY -> {
            val exercise = guidedId?.let { GuidedExerciseCatalogue.byId(it) }
            if (exercise == null) route = MindfulnessRoute.GUIDED_LIST
            else GuidedPlayerScreen(exercise = exercise, onClose = { route = MindfulnessRoute.GUIDED_LIST })
        }
        MindfulnessRoute.BREATHE_SETUP -> BreatheSetupScreen(
            patternId = patternId,
            duration = duration,
            visual = visual,
            onPattern = { patternId = it },
            onDuration = { duration = it },
            onVisual = { visual = it },
            onStart = { route = MindfulnessRoute.BREATHE_RUN },
        )
        MindfulnessRoute.BREATHE_RUN -> {
            val pattern = BreathingPatterns.byId(patternId) ?: BreathingPatterns.belly
            val timer = remember(pattern, duration) { BreathingTimer(pattern, duration) }
            BreathingExerciseScreen(
                timer = timer,
                visual = visual,
                onClose = { route = MindfulnessRoute.BREATHE_SETUP },
            )
        }
    }
}
