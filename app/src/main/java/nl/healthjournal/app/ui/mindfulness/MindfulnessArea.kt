package nl.healthjournal.app.ui.mindfulness

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import nl.healthjournal.app.settings.MindfulnessPreferences
import nl.healthjournal.domain.model.mindfulness.BreathingPatterns
import nl.healthjournal.domain.model.mindfulness.BreathingTimer
import nl.healthjournal.domain.model.mindfulness.CueChoices
import nl.healthjournal.domain.model.mindfulness.GuidedExerciseCatalogue
import nl.healthjournal.domain.model.mindfulness.MeditationTimer

private enum class MindfulnessRoute { HOME, BREATHE_SETUP, BREATHE_RUN, MEDITATE_SETUP, MEDITATE_RUN, GUIDED_LIST, GUIDED_PLAY }

/**
 * The Mindfulness area: home, the breathing and meditation setups and the running exercise. Back goes one step up;
 * the area's own state is the current screen and the last choices on the setup screens, which are kept in
 * [MindfulnessPreferences] so they are there next time.
 */
@Composable
fun MindfulnessArea() {
    var route by rememberSaveable { mutableStateOf(MindfulnessRoute.HOME) }
    val context = LocalContext.current
    val saved = remember { MindfulnessPreferences(context.applicationContext) }
    var patternId by rememberSaveable { mutableStateOf(saved.pattern) }
    var duration by rememberSaveable { mutableStateOf(saved.duration) }
    var visual by rememberSaveable { mutableStateOf(saved.visual) }
    var meditateMinutes by rememberSaveable { mutableStateOf(saved.meditateMinutes) }
    var meditateInterval by rememberSaveable { mutableStateOf(saved.meditateInterval) }
    var breatheCues by rememberSaveable(stateSaver = CueSaver) { mutableStateOf(saved.breatheCues) }
    var meditateCues by rememberSaveable(stateSaver = CueSaver) { mutableStateOf(saved.meditateCues) }
    var guidedId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = route == MindfulnessRoute.BREATHE_SETUP || route == MindfulnessRoute.MEDITATE_SETUP || route == MindfulnessRoute.GUIDED_LIST) { route = MindfulnessRoute.HOME }

    when (route) {
        MindfulnessRoute.HOME -> MindfulnessHomeScreen(
            onBreathe = { route = MindfulnessRoute.BREATHE_SETUP },
            onMeditate = { route = MindfulnessRoute.MEDITATE_SETUP },
            onGuided = { route = MindfulnessRoute.GUIDED_LIST },
        )
        MindfulnessRoute.MEDITATE_SETUP -> MeditateSetupScreen(
            minutes = meditateMinutes,
            intervalMinutes = meditateInterval,
            onMinutes = { minutes ->
                meditateMinutes = minutes
                saved.meditateMinutes = minutes
                if (meditateInterval?.let { it >= minutes } == true) {
                    meditateInterval = null
                    saved.meditateInterval = null
                }
            },
            onInterval = { meditateInterval = it; saved.meditateInterval = it },
            cues = meditateCues,
            onCues = { meditateCues = it; saved.meditateCues = it },
            onStart = { route = MindfulnessRoute.MEDITATE_RUN },
        )
        MindfulnessRoute.MEDITATE_RUN -> {
            val timer = remember(meditateMinutes, meditateInterval) { MeditationTimer(meditateMinutes, meditateInterval) }
            MeditationRunScreen(timer = timer, cues = meditateCues, onClose = { route = MindfulnessRoute.MEDITATE_SETUP })
        }
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
            onPattern = { patternId = it; saved.pattern = it },
            onDuration = { duration = it; saved.duration = it },
            onVisual = { visual = it; saved.visual = it },
            cues = breatheCues,
            onCues = { breatheCues = it; saved.breatheCues = it },
            onStart = { route = MindfulnessRoute.BREATHE_RUN },
        )
        MindfulnessRoute.BREATHE_RUN -> {
            val pattern = BreathingPatterns.byId(patternId) ?: BreathingPatterns.belly
            val timer = remember(pattern, duration) { BreathingTimer(pattern, duration) }
            BreathingExerciseScreen(
                timer = timer,
                visual = visual,
                cues = breatheCues,
                onClose = { route = MindfulnessRoute.BREATHE_SETUP },
            )
        }
    }
}

private val CueSaver = listSaver<CueChoices, Any?>(
    save = { listOf(it.tones, it.haptics, it.speech, it.voiceName) },
    restore = { CueChoices(tones = it[0] as Boolean, haptics = it[1] as Boolean, speech = it[2] as Boolean, voiceName = it[3] as String?) },
)
