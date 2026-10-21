## Context

The area follows the pillbox pattern: a button in the top bar opens its own screens over the current tab. Content decisions (own texts, tones generated in code, no third-party audio) come from [docs/dev/mindfulness.md](../../../../docs/dev/mindfulness.md). The app has no network permission and a build check keeps it that way.

## Goals / Non-Goals

**Goals:**
- Calm, simple exercises that work offline, in English and Dutch, with a quiet visual.
- Pattern and timer logic that is plain data and plain Kotlin, so it is unit tested without Android.
- Nothing about a person's practice is stored.
- No copyright risk: every piece of content is either original or used under a confirmed open licence, with credit.

**Non-Goals:** see the proposal.

## Decisions

### 1. Patterns are data in `:domain`
A `BreathingPattern` is an id, a list of phases (kind: inhale, hold, exhale; length in milliseconds) and a flag for whether it holds the breath. The built-in patterns and the custom pattern use the same type. A pure `BreathingTimer` maps elapsed time to the current phase, its progress (0 to 1) and the seconds left in the phase, and to the end of the chosen duration. It has no clock and no Android types, so tests drive it with plain numbers. Breathing counts such as 4-4-4-4 are facts about a technique, not protected text.

### 2. Rendering in `:app` with Compose Canvas
The lotus is a ring of petals (ellipses) drawn by the app, whose spread and scale follow an eased progress value: open on the inhale, still on a hold, closing on the exhale. A second visual, the breathing circle, is one soft circle (with a faint outer ring) whose radius follows the same eased progress value. Both visuals draw from the same progress value, so a visual is a small composable that takes progress and phase and nothing else; the choice is a display preference and further visuals can be added the same way. Box breathing adds a dot that travels one side of a rounded square per phase. Colours come from the theme (light and dark, `isAppDarkTheme()`), no gradients, slow easing. When the system animation setting is off, the lotus switches between two states per phase and the dot jumps to the next corner, and the phase text and countdown carry the exercise. No image files are used.

### 3. Cues
Haptics use the platform vibrator (`VIBRATE`, a normal permission) with a short pulse at phase changes. Bells and phase tones are generated in code (sine tones with a soft envelope played through `AudioTrack`), so there are no audio files and no licence question. Spoken cues use `TextToSpeech` with a voice that does not require a network connection; if the app language has no such voice, spoken cues are unavailable and the setting says so. Haptics, tones and spoken cues are each optional, chosen by the user.

### 4. Screen behaviour
The screen stays on during an exercise or timer, controls fade out after a few seconds and return on touch, the secure-screen setting still applies, and TalkBack announces each phase change once. A short "ready" moment comes before the first phase and a quiet "done" screen ends the exercise. Leaving the screen stops everything. There is no persisted result.

### 4a. Undisturbed, and restored afterwards
An exercise holds one small session object that acquires everything it needs and releases it in one place when the exercise ends, is left, or the screen goes to the background: the keep-screen-on flag, a transient audio focus request (so other audio pauses or lowers and resumes afterwards), the `MediaPlayer`, `AudioTrack`, `TextToSpeech` and vibration. Nothing outside the app is changed: no volume, ringer mode, Do Not Disturb policy or brightness is touched (the first two would need extra permissions and would be easy to forget to undo). While a session runs, the app's own transient UI (dialogs, snackbars, prompts) is held back and shown afterwards, and the app lock does not trigger for time spent in an exercise. Notifications from other apps and the device's own settings are left to the user: the medication reminders of this app are not silenced, because a missed dose reminder matters more than a calm exercise. If the user wants silence, the phone's own Do Not Disturb does that and the app does not need to know. Leaving the screen (back, home, a call) stops the exercise instead of letting it run on in the background.

### 5. No history
No session is written anywhere: not to Room, not to preferences, not to the log. The only persisted values are the display choices of decision 6.

### 6. Preferences
Last used duration, cues on or off, the custom pattern counts and whether the safety note was acknowledged are kept in the existing preferences. They are display choices, not history. They are not added to the backup `preferences` object in this change (the format would need a schema bump for little value); this is revisited if users ask.

### 7. Safety note
The first time the area opens, a short note is shown and acknowledged once. Patterns that hold the breath show a one-line reminder before starting. The full text lives under "About these exercises". All three share one string set in English and Dutch. The wording is neutral and descriptive, written for the app, and does not claim a benefit.

### 8. Start here is a fixed list
The ladder is a fixed ordered list of existing exercises and timer settings, shown as steps. It has no progress tracking, since nothing is stored.

### 9. Guided exercises from the Free Mindfulness Project
The catalogue is plain data in `:domain`: an id, a language tag of the recording, a duration, a title and description per interface language (written for the app, English and Dutch), the raw resource name, and a credit (teacher, source URL, licence name and version, licence URL). The player in `:app` plays the bundled MP3 with `MediaPlayer` from a raw resource, offline, keeps the screen on, stops when the screen is left and logs nothing. The first selection is Three Step Breathing Space, Three minute mindfulness of sounds and Mountain meditation, all by the project's creator, who is also the person to ask if the terms need confirming. Items attributed to university centres are left out for now: their own sites publish the same recordings under stricter terms than the aggregator page states.

Language: every item carries the language of its audio. The list shows items whose audio language matches the interface language first, then the others with a label (for example "Audio in English"). In the Dutch interface the English recordings therefore appear with Dutch titles, descriptions and controls and a clear label. A later Dutch recording is one more catalogue entry with language `nl`; nothing else changes. The player has no transcript, because the project publishes none and writing one would be an adaptation.

Licence: the recordings are used unchanged (no re-encoding, trimming or mixing) and credited. Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported means: credit the source, no commercial use, and any adaptation under the same licence. The app therefore stays free and without advertising while it contains them, and the credits screen shows the licence with a link. The project code is under Apache-2.0 (`LICENSE`, `NOTICE`); the recordings are not covered by it and stay under their own licence inside the app, which `NOTICE` and the credits say.

### 10. Content provenance and credit
The own texts of the app (descriptions, phase names, the safety note, the "Before you start" and "About" pages) are written for the app in English and Dutch under the licence of the project. Outside material is allowed when it is openly licensed and the licence covers what the app does with it: bundling in an app, translation or adaptation when the app needs that, and non-commercial or share-alike terms the project can meet. Each item records its source, author, licence and version, and the licence text, in `docs/` and in an in-app "Credits" screen that links to the original. Material is never used on an assumption: a candidate with an unknown or unconfirmed licence stays out until the licence is confirmed (for the Free Mindfulness Project the version was found: Attribution-NonCommercial-ShareAlike 3.0 Unported, and the use is confirmed with the project before release). A licence with a no-derivatives clause allows the original wording only, so a Dutch translation then needs written permission. Guided audio from an openly licensed source may be bundled under the same rules, with its size counted against the app. The reviewer checks every item against its licence before the change is archived.

## Risks / Trade-offs

- **Breath holds may not suit everyone.** Mitigated by the safety note and by offering patterns without a hold (belly breathing, physiological sigh).
- **Text-to-speech quality and availability vary by phone.** Spoken cues are optional, and every phase is also shown as text.
- **Tone generation timing.** Cues are tied to the timer, not to audio callbacks, so a late buffer can shift a tone by a fraction of a second; acceptable for a calm exercise.
- **Cleanup missed on a crash or process death.** The screen-on flag and audio focus belong to the window and the process, so the platform releases them; the single release point and a test with a fake session cover the normal paths.
- **Animation cost.** Canvas drawing at the display rate is cheap; the exercise stops drawing when the screen is not visible.
- **No history means no sense of progress.** This is a deliberate privacy choice, recorded in the non-goals.
- **The licence statement comes from the aggregator page.** The download page states Attribution-NonCommercial-ShareAlike 3.0 Unported for all files, while the about page says only that contributors agreed to sharing. The recordings may have come from other organisations whose own pages use other terms. Mitigated by choosing the creator's own recordings, by checking the licence statement on the download page before each release, by keeping the app free and without advertising, and by dropping any item whose licence cannot be shown. Writing to the project is optional (a draft is kept in the docs) and becomes necessary only if the app is ever sold or carries advertising.
- **App size.** Bundled recordings add several MB each. Mitigated by a short first selection; downloading is not an option (no network).
- **Accidental similarity to existing scripts.** Short descriptions of well-known techniques can resemble other texts. Mitigated by writing from the pattern data (counts and phases) and by the review in decision 9.
