## ADDED Requirements

### Requirement: Mindfulness area
The system SHALL provide a Mindfulness area with a Breathe section, a Meditate section and a Start here section. It SHALL work without a network and without an account.

#### Scenario: Open the area
- **WHEN** the user opens the area from the top bar
- **THEN** the sections Breathe, Meditate and Start here are listed, with text in the app language

#### Scenario: No account or network
- **WHEN** the device has no network connection
- **THEN** every exercise works the same

### Requirement: Breathing patterns
The Breathe section SHALL offer these patterns, each with a short description and its counts: box breathing (inhale 4, hold 4, exhale 4, hold 4 seconds), 4-7-8 (inhale 4, hold 7, exhale 8), physiological sigh (inhale 2, a second short inhale 1, exhale 6), belly breathing (inhale 4, exhale 6) and a custom pattern with counts the user enters. The user SHALL choose a duration of 1, 3 or 5 minutes. Counts of the custom pattern SHALL be whole seconds within a limited range and SHALL be validated before the exercise starts.

#### Scenario: Start box breathing
- **WHEN** the user starts box breathing for 3 minutes
- **THEN** the exercise runs the four phases in order, repeats them, and ends after 3 minutes

#### Scenario: Invalid custom counts
- **WHEN** a custom count is empty, zero or outside the allowed range
- **THEN** the exercise does not start and a message next to the field says what is allowed

#### Scenario: A pattern without a hold
- **WHEN** the user opens belly breathing or the physiological sigh
- **THEN** no breath-hold reminder is shown

### Requirement: Breathing exercise screen
During an exercise the screen SHALL name the current phase in text, count down the seconds of the phase and show a visual that grows or opens during the inhale, stays still during a hold and shrinks or closes during the exhale. The user SHALL be able to choose the visual: a lotus whose petals open and close (the default) or a circle that grows and shrinks. Box breathing SHALL also show a dot that moves along one side of a square per phase. The visual SHALL use the app theme in light and dark. When the system animation setting is off, the movement SHALL be replaced by a change of state per phase without continuous animation. The phase SHALL be announced to a screen reader when it changes.

#### Scenario: Phase text and countdown
- **WHEN** an exercise is in the inhale phase with 3 seconds left
- **THEN** the screen shows the phase name for the inhale and the number 3

#### Scenario: Choose the visual
- **WHEN** the user picks the circle instead of the lotus
- **THEN** the circle grows during the inhale and shrinks during the exhale, the choice is remembered for the next exercise, and the phase text and countdown are the same

#### Scenario: Animations turned off
- **WHEN** the system setting to remove animations is on
- **THEN** the chosen visual changes between its large and small state (open and closed for the lotus) at each phase change and does not animate continuously

#### Scenario: Leaving the exercise
- **WHEN** the user presses back during an exercise
- **THEN** the exercise stops and the previous screen is shown, and no result is kept

### Requirement: Ready and done
An exercise SHALL begin with a short ready moment before the first phase and SHALL end with a quiet screen that offers to repeat the exercise or to return, without a score, streak or total.

#### Scenario: Finish an exercise
- **WHEN** the chosen duration has passed
- **THEN** the exercise ends and a done screen without any figures about the session is shown

### Requirement: Guided exercises
The system SHALL offer a list of recorded guided exercises that play from audio bundled in the app, without a network. Each exercise SHALL show a title, a short description, its duration and the language of its recording, with the title and description in the app language. The first version SHALL include the exercises Three Step Breathing Space, Three minute mindfulness of sounds and Mountain meditation from the Free Mindfulness Project. The player SHALL offer play, pause and stop, SHALL keep the screen on while playing, SHALL stop when the user leaves the screen and SHALL NOT keep a record of what was played.

#### Scenario: Play an exercise offline
- **WHEN** the user starts a guided exercise with the device offline
- **THEN** the recording plays with its credit visible

#### Scenario: Dutch interface, English recording
- **WHEN** the app language is Dutch and the exercise recording is English
- **THEN** the title, description and controls are Dutch and the exercise states that the audio is in English

#### Scenario: Matching language first
- **WHEN** the catalogue holds recordings in both the app language and another language
- **THEN** the exercises in the app language are listed first and the others carry a language label

#### Scenario: Dutch exercise added later
- **WHEN** a Dutch recording is added to the catalogue with its credit
- **THEN** it appears in the list without any change to the screens

#### Scenario: Leaving the player
- **WHEN** the user leaves the player
- **THEN** playback stops and nothing about the session is kept

### Requirement: Meditation timer
The Meditate section SHALL offer a timer with a duration chosen by the user, a start bell, an end bell and optional interval bells. The timer SHALL stop when the user leaves the screen.

#### Scenario: Timer with an interval bell
- **WHEN** the user starts a 10 minute timer with an interval bell every 2 minutes
- **THEN** a bell sounds at the start, every 2 minutes and at the end

#### Scenario: Bells off
- **WHEN** tones are turned off
- **THEN** the timer shows the remaining time and ends with a visible message and no sound

### Requirement: Start here
The Start here section SHALL show a short ordered list of steps that opens the first exercises (breathing for 1 minute, breathing for 3 minutes, a short timer) and SHALL NOT track which steps were done.

#### Scenario: Open a step
- **WHEN** the user taps a step
- **THEN** the matching exercise opens with its settings filled in

### Requirement: Optional cues
Haptic pulses, tones and spoken phase cues SHALL each be optional and chosen by the user. Tones SHALL be produced by the app itself. Spoken cues SHALL use the text-to-speech voice of the device in the app language and SHALL only use a voice that works without a network. When no such voice exists for the language, the setting SHALL say that spoken cues are not available and the exercise SHALL work with text only.

#### Scenario: Spoken cues in Dutch
- **WHEN** the app language is Dutch, spoken cues are on and an offline Dutch voice is installed
- **THEN** the phases are spoken in Dutch

#### Scenario: No offline voice
- **WHEN** spoken cues are on but no offline voice exists for the app language
- **THEN** nothing is spoken and the setting explains that spoken cues are not available

### Requirement: Screen during an exercise
The screen SHALL stay on during an exercise or timer. Controls SHALL fade out after a few seconds and return when the screen is touched. The secure-screen setting SHALL apply as everywhere in the app.

#### Scenario: Controls return on touch
- **WHEN** the controls have faded out and the user touches the screen
- **THEN** the controls are shown again

### Requirement: Undisturbed during an exercise
While an exercise, timer or recording is running, the system SHALL NOT show dialogs, banners or prompts of its own; any that arise SHALL be shown after the exercise ends. Time spent in an exercise SHALL NOT start the app lock. Leaving the screen, sending the app to the background or an incoming call SHALL stop the exercise.

#### Scenario: Message held back
- **WHEN** the app has a message to show while an exercise runs
- **THEN** it is shown after the exercise ends and not during it

#### Scenario: App sent to the background
- **WHEN** the user switches to another app during an exercise
- **THEN** the exercise stops and its sound and haptics stop

### Requirement: Original state restored
When an exercise, timer or recording ends or is left, the system SHALL release the keep-screen-on flag, audio focus, the audio player, the tone output, the speech engine and the vibrator, so the phone is as it was before. The system SHALL NOT change the volume, the ringer mode, Do Not Disturb or the screen brightness at any time.

#### Scenario: After an exercise
- **WHEN** an exercise ends
- **THEN** audio from other apps that was paused or lowered by it resumes and the screen is no longer kept on by the app

#### Scenario: Settings untouched
- **WHEN** an exercise runs with the phone on silent or in Do Not Disturb
- **THEN** the phone stays in that mode before, during and after the exercise

### Requirement: Safety note
The system SHALL show a short "Before you start" note the first time the area is opened, SHALL show a one-line reminder before a pattern that holds the breath, and SHALL offer the full text under "About these exercises". The text SHALL be neutral, available in English and Dutch, and SHALL NOT claim a health benefit.

#### Scenario: First open
- **WHEN** the area is opened for the first time
- **THEN** the note is shown and, once acknowledged, is not shown again at later opens

#### Scenario: Breath hold reminder
- **WHEN** the user starts box breathing or 4-7-8
- **THEN** the one-line reminder is shown before the exercise starts

#### Scenario: Read the full text
- **WHEN** the user opens "About these exercises"
- **THEN** the full text is shown in the app language

### Requirement: Content provenance and credit
Every text, image and sound of the area SHALL be either written or produced for the app, or taken from work under an open licence that allows the use made of it (including bundling, and translation or adaptation where the app does that). Recordings taken from other work SHALL be used unchanged unless the licence allows changes, and the app SHALL remain free of charge and of advertising while it contains recordings whose licence forbids commercial use. The system SHALL NOT include material whose licence is unknown, unconfirmed or does not allow that use. Tones and bells SHALL be generated by the app. For each item taken from other work, the app SHALL show the source, the author and the licence with a link to the original on a Credits screen, and the repository documentation SHALL record the same together with the licence version.

#### Scenario: Credit for outside material
- **WHEN** an exercise is taken from an openly licensed project
- **THEN** the Credits screen names the teacher, the project and the licence (including its version) and links to the original and to the licence text, and the exercise screen shows the credit too

#### Scenario: Unconfirmed licence
- **WHEN** a candidate source has no confirmed licence version or its terms forbid the intended use
- **THEN** it is not included in the app

#### Scenario: Texts reviewed
- **WHEN** the change is reviewed before release
- **THEN** each text, image and sound is confirmed as original or matched to a recorded licence

### Requirement: Nothing about the practice is stored
The system SHALL NOT store or log which exercises were done, when, for how long, how often or with what result. The only values kept are the user's display choices for the area (last duration, cues on or off, custom counts, whether the safety note was acknowledged).

#### Scenario: Finish a session
- **WHEN** an exercise or timer ends
- **THEN** no entry about it is written to the database, the preferences or the device log

#### Scenario: Backup contents
- **WHEN** an encrypted backup is created
- **THEN** it contains no information about mindfulness sessions
