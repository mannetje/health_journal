## 0. Decisions
- [x] 0.1 Lotus 🪷 next to the pill button, own screens (requested 2026-10-21)
- [x] 0.2 Version 1: Breathe and Meditate timer, "Start here" if it stays small
- [x] 0.3 Spoken guidance with offline text-to-speech; own recordings later
- [x] 0.4 No session log; no reminder in this change; ACT-style exercises later
- [x] 0.5 Own texts, tones generated in code; openly licensed material (such as the Free Mindfulness Project) allowed from version 1 when its licence is confirmed and it is credited
- [x] 0.6 Spec review by the user before implementation starts (reviewed 2026-10-21, no changes asked; medication reminders stay unchanged during an exercise)
- [x] 0.6a Free Mindfulness Project licence found on its download page: Attribution-NonCommercial-ShareAlike 3.0 Unported; first selection: Three Step Breathing Space, Three minute mindfulness of sounds, Mountain meditation (all by the project creator); English audio with a Dutch interface; Dutch exercises later
- [ ] 0.6b Optional: write to the project (draft in `docs/dev/mindfulness.md`) only if the app is ever to be sold or carry advertising, or if the licence on the project download page changes. Before each release check that the page still states Attribution-NonCommercial-ShareAlike 3.0 Unported. Not needed while the app is free and the recordings are credited and unchanged
- [x] 0.6c Project licence chosen: Apache-2.0 (`LICENSE`, `NOTICE`, credit to the GitHub accounts marvinstorage and mannetje, no real name); covers the code only, the recordings keep their own licence
- [x] 0.7 Final wording of the safety note in English and Dutch agreed (2026-10-21, in `docs/dev/mindfulness.md`); credits screen is Credits / Bronnen

## 1. Domain (`:domain`)
- [x] 1.1 `BreathingPattern` and phases; built-in patterns; custom pattern validation
- [x] 1.2 `BreathingTimer` (elapsed time to phase, progress, seconds left, end)
- [x] 1.3a Guided exercise catalogue (id, audio language, duration, titles and descriptions per language, credit) with ordering by language; JVM tests
- [x] 1.3 Meditation timer with start, interval and end bell times
- [x] 1.4 JVM tests for all patterns, custom validation, timer boundaries and interval bells

## 2. App (`:app`)
- [ ] 2.1 Lotus button in the top bar and navigation to the area, back handling
- [x] 2.2 Area home with Breathe, Meditate, Start here, "About these exercises". Done: home, About link, `StartHereScreen` opening the setups with the step settings; Start here checked on the emulator
- [ ] 2.3 Exercise screen: lotus, circle and square drawing, visual choice kept in preferences, phase text, countdown, ready and done, fading controls, keep screen on, reduced-animation mode, TalkBack announcements
- [x] 2.3a Guided exercises list and player (play, pause, stop, offline, screen on, no log), language label, credit shown. Done: `GuidedListScreen`, `GuidedPlayerScreen` (MediaPlayer, audio focus, pauses when the app goes to the background); emulator check pending
- [x] 2.3b Three MP3 files added unchanged to `app/src/main/res/raw/` on 2026-10-21 (downloaded with the user's permission after checking the licence on the download page, which still states Attribution-NonCommercial-ShareAlike 3.0 Unported): `fmp_three_step_breathing_space.mp3` (3:34, 0.98 MB), `fmp_mindfulness_of_sounds.mp3` (3:02, 1.5 MB), `fmp_mountain_meditation.mp3` (8:12, 7.9 MB); about 10.4 MB in total, SHA-256 equal to the downloads. The player and catalogue (1.3a, 2.3a) still use them; the APK size is checked at the next build
- [x] 2.3c Exercise session object: acquire and release screen-on, audio focus, player, tones, speech and vibration in one place; stop on background, back or a call; hold back the app's own dialogs and the app lock during a session; JVM test with a fake session
- [x] 2.4 Meditation timer screen
- [x] 2.5 Tone generation, haptics, offline text-to-speech with the no-voice message
- [x] 2.6 Safety note (first open with a quiet Read more link, breath hold reminder, full text) with acknowledgement kept in preferences. Done: `SafetyNoteScreen`, `AboutExercisesScreen`, hold reminder on the breathing setup; checked on the emulator (note, Read more, Back, acknowledgement kept, About link, reminder shown for Box and gone for Belly)
- [x] 2.7 Display choices in preferences; nothing else stored or logged. Done: `MindfulnessPreferences`; checked on the emulator across a force-stop
- [x] 2.8 Own texts written for the app, strings in English and Dutch for every label, phase, cue, note and message (strings reviewed: keys match in both languages, no hard-coded texts)
- [x] 2.9 `VIBRATE` in the manifest; `PrivacyChecksTest` still green (no INTERNET, no logging)
- [ ] 2.10 Check on the emulator: each pattern, timer, both languages, light and dark, animations off, narrow phone

## 3. Docs
- [ ] 3.1 Rewrite `docs/dev/mindfulness.md` for the real feature; update `docs/dev/README.md`, `architecture.md`, `design.md`
- [ ] 3.2 README (key features, roadmap), CHANGELOG, in-app privacy notice
- [x] 3.3a `docs/credits.md` with teacher, source URL, licence, version and file name of each recording (plus file hashes)
- [x] 3.3 (checked on the emulator) Credits screen (English "Credits", Dutch "Bronnen") and a credits record in `docs/` with source, author, licence and version of every outside item; review against design decision 9
- [ ] 3.4 Merge the deltas into `openspec/specs` (mindfulness, privacy, platform-android, localization) and archive the change
- [ ] 3.5 `bash scripts/check-docs.sh` and `npx --no-install openspec validate --all --strict` pass
