# Add Mindfulness

## Why
The user wants a calm place in the app for slowing down: guided breathing exercises with a quiet visual, and a plain meditation timer. It sits next to the pillbox in the top bar, behind a lotus icon, and follows the rules of the rest of the app: offline, no accounts, nothing about a person's practice is stored. The brainstorm, the licence research and the decisions are in [docs/dev/mindfulness.md](../../../docs/dev/mindfulness.md).

## What Changes
- **Lotus button:** a 🪷 button beside the 💊 button in the top bar on every main tab. It opens the Mindfulness screens on top of the current tab, and back returns to that tab.
- **Breathe:** a list of breathing patterns, each with a short description, opening a full-screen exercise: box breathing (4-4-4-4), 4-7-8, physiological sigh (two short inhales, one long exhale), belly breathing (4 in, 6 out) and a custom pattern with the user's own counts. Durations of 1, 3 or 5 minutes. The user chooses the visual: a lotus whose petals open on the inhale and close on the exhale (the default), or a soft circle that grows on the inhale and shrinks on the exhale. Box breathing also shows a dot tracing a square. More visuals can be added later without changing the exercises. Each phase is named in text and counted down.
- **Guided exercises:** a short list of recorded mindfulness exercises from the [Free Mindfulness Project](https://www.freemindfulness.org/download), played offline from the app, next to the breathing patterns. The first selection is three exercises by the project's creator: Three Step Breathing Space (3:34), Three minute mindfulness of sounds (3:02) and Mountain meditation (8:12). The recordings are English. In the Dutch interface the titles, descriptions and controls are Dutch and each exercise says that the audio is in English. The catalogue is built per language, so Dutch exercises can be added later without changing the screens.
- **Meditate:** a timer with a start bell, an end bell and optional interval bells. Durations are chosen by the user.
- **Start here:** a short ladder for beginners that links the first exercises in order (1 minute breathing, 3 minutes breathing, a short timer).
- **Cues:** optional haptics, optional tones generated in code and optional spoken phase cues through the phone's own offline text-to-speech, in the app language (English or Dutch). Spoken cues are skipped when no offline voice exists for the language.
- **Undisturbed, then back to normal:** while an exercise, timer or recording runs, the app shows no dialogs, banners or prompts of its own, and the phone's own state is left as it was found: the screen-on flag, audio focus, the speech engine and the vibrator are released when the exercise ends or is left, and no system setting (volume, ringer mode, Do Not Disturb, brightness) is changed.
- **Safety note:** a short "Before you start" note the first time the area is opened, a one-line reminder on patterns that hold the breath, and the full text under "About these exercises". Neutral wording, no medical claims.
- **Content provenance:** texts, images and sounds are either written or made for the app, or taken from openly licensed work whose licence allows the use, with credit and a link to the original shown in the app and in the docs. The Free Mindfulness Project recordings are licensed Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported (stated on its download page); the exercises are used unchanged, credited to the named teacher and the project, and the licence is shown with them. Nothing is copied from work without such a licence.
- **Nothing is logged:** no session history, streaks, counters or totals.
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `mindfulness` (screens, patterns, timer, cues, safety note, content provenance and credit, nothing stored)
- **Modified Capability:** `platform-android` (lotus button, vibration permission, screen kept on during an exercise)
- **Modified Capability:** `privacy` (no practice data is stored or logged; spoken cues never use the network)
- **Modified Capability:** `localization` (mindfulness text in English and Dutch)

## Impact
- **Database:** none. Room stays at version 6.
- **Stored data shape:** unchanged. The only persisted values are display choices for the area (last used duration, cues on or off, custom counts, the acknowledged safety note) in the existing preferences, which are not exercise history. They are not added to the backup file in this change.
- **Affected code:** pattern model and timer in `:domain` (pure Kotlin); screens, the lotus and square drawing (Compose Canvas), tone generation, haptics and text-to-speech in `:app`; nothing in `:data`.
- **App size:** each bundled MP3 adds roughly 3 to 8 MB; the first selection is about 15 MB in total (check on the real files).
- **Dependencies:** none. Tones are generated in code, text-to-speech and vibration come from the platform.
- **Permissions:** `VIBRATE` (a normal permission, no prompt). No network permission.
- **Docs:** `docs/credits.md` and a credits page in the app and in `docs/` for any outside material; `docs/dev/mindfulness.md` (rewritten from the brainstorm to the real feature), `docs/dev/README.md`, `docs/dev/architecture.md`, `docs/dev/design.md` (screen map), README, CHANGELOG, in-app privacy notice, `openspec/specs/README.md`.

## Non-goals
- No session log, streaks, reminders or statistics (a daily reminder may come later and would be its own change).
- No Dutch recordings yet and no Dutch translation of the English recordings (a translation is an adaptation that falls under the share-alike clause and would need a script the project does not publish). Dutch exercises may be added later, from an openly licensed source or recorded by the maintainer.
- No commercial use: the app stays free, without advertising or payment, while it contains these recordings.
- No use of material whose licence is unknown, restrictive (for example no derivatives or no redistribution) or reference-only.
- No ACT-style "Strong feelings" section in this version. It needs stronger safety wording first and is a later change.
- No music, sleep stories, courses, accounts or streaming.
- No claims about health benefits and no advice.
