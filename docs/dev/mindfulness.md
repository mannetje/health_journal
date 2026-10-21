# Mindfulness (idea)

> **Status: brainstorm, nothing is built or specified yet.** This page collects the ideas, the open questions and the sources researched so far. When the scope is agreed it becomes an OpenSpec change (`add-mindfulness`) and this page is rewritten to describe the real feature, as [Backup and restore](backup.md) was.

A new area next to the pillbox, opened from a 🪷 (lotus) icon beside the 💊 icon in the top bar, with its own screens. It stays inside the rules of the app: offline (no `INTERNET` permission), no accounts, no session log, no logging of personal data, neutral figures only, English and Dutch for all text.

## 1. Ideas so far

| Section | Content |
|---------|---------|
| **Breathe** | Patterns with a calm animation, optional haptics and tones, durations of 1, 3 or 5 minutes: box breathing (4-4-4-4), 4-7-8, physiological sigh (two short inhales, one long exhale), belly breathing (about 4 in, 6 out), and a custom pattern with the user's own counts. The visual can be chosen: a lotus whose petals open and close (default) or a soft circle that grows and shrinks. Box breathing can show a dot tracing a square. |
| **Meditate** | A plain timer with start and end bell and optional interval bells. Short scripts written for the app: arriving, breath awareness, a short body scan, a three-minute breathing space. |
| **Strong feelings** (later version) | A small section of short, light exercises inspired by acceptance and commitment therapy (ACT) for moments of strong stress or emotion: 5-4-3-2-1 grounding, naming what you notice, leaves on a stream, a "feet on the floor, three slow breaths" anchor. Not in version 1; it needs stronger safety wording first (section 2). |
| **Guided exercises** | Recorded exercises from the Free Mindfulness Project, played offline: Three Step Breathing Space, Three minute mindfulness of sounds, Mountain meditation (all by the project creator). English audio with Dutch titles, descriptions and controls; Dutch exercises can be added later as more catalogue entries. |
| **Start here** | A small ladder for beginners (1 minute, then 3 minutes, then a short body scan). No courses, no accounts. |

Not planned: a session log (nothing about a user's practice is stored), sleep stories, music catalogues, teacher-led courses, streaming. They need content that cannot be licensed or produced here, and the app has no network access.

### Decisions

1. **Version 1** is Breathe and the Meditate timer, with the "Start here" ladder if it stays small. Own voice recordings come later.
2. **Spoken guidance** uses the phone's offline text-to-speech (English and Dutch) for now: no audio files and no licence question. Recordings made by the maintainer can replace it later.
3. **Daily reminder** comes later. It is easy to add once the medication reminder code is reused, and it is not needed to judge whether the feature works.
4. **ACT-style "Strong feelings" exercises** come in a later version, after the safety wording is agreed.

### Undisturbed, and back to normal

While an exercise runs the app shows nothing of its own (dialogs wait until it ends) and the app lock does not start. Everything the exercise takes (screen-on flag, audio focus, player, tones, speech, vibration) is released in one place at the end or when the screen is left, and no system setting is changed: no volume, ringer mode, Do Not Disturb or brightness. The medication reminders are not silenced by an exercise. Open point: whether the user wants them held back during an exercise; the proposal says no because a dose reminder matters more.

## 2. Safety note

Agreed wording (2026-10-21). Neutral, no benefit claims, no age, diagnosis or medicine names. The Dutch follows the tone of the medication notice ("je").

**1. First-open note** (shown once, acknowledged with one button, with a quiet "Read more" text link to text 3 and nothing else on the screen)

| | English | Dutch |
|---|---|---|
| Title | Before you start | Voordat je begint |
| Text | These are simple breathing and attention exercises. They are not medical advice or treatment. Go at your own pace and stop whenever you like. If you feel dizzy, short of breath or unwell, stop and breathe normally. Some exercises hold the breath. These may not suit everyone, for example with heart or lung conditions or during pregnancy. If you are unsure, ask your doctor first. | Dit zijn eenvoudige adem- en aandachtsoefeningen. Het is geen medisch advies en geen behandeling. Ga in je eigen tempo en stop wanneer je wilt. Voel je je duizelig, benauwd of onwel, stop dan en adem rustig normaal door. Bij sommige oefeningen houd je je adem even vast. Dat past niet bij iedereen, bijvoorbeeld bij hart- of longaandoeningen of tijdens een zwangerschap. Twijfel je, vraag het dan eerst aan je huisarts. |
| Button | I understand | Begrepen |
| Link | Read more | Meer lezen |

**2. One-line reminder** (before box breathing and 4-7-8; announced to a screen reader)

- EN: This exercise holds your breath briefly. Stop if you feel dizzy or unwell.
- NL: Bij deze oefening houd je even je adem vast. Stop als je je duizelig of onwel voelt.

**3. About these exercises** (full text, same menu as "About these ranges")

- EN: The breathing patterns are common, widely shared techniques. The app times them and shows the counts. It does not measure or judge your breathing or your health. Nothing about your practice is stored: no history, no streaks, no totals. The recorded exercises are by their teachers and shared under an open licence. See Credits. Pregnant, or with a heart or lung condition? Skip the breath-hold patterns, or check with your doctor first.
- NL: De ademhalingspatronen zijn gangbare, breed gedeelde technieken. De app telt de tijd en toont de aantallen. Hij meet of beoordeelt je ademhaling of gezondheid niet. Er wordt niets over je oefenen bewaard: geen geschiedenis, geen reeksen, geen totalen. De opgenomen oefeningen zijn van hun docenten en gedeeld onder een open licentie. Zie Bronnen. Ben je zwanger, of heb je een hart- of longaandoening? Sla de patronen met adem vasthouden dan over of overleg eerst met je huisarts.

**4. Recorded exercises** (one line on the player, because the audio is English)

- EN: Recording by [teacher]. Stop at any time. Not medical advice.
- NL: Opname van [docent]. Je kunt altijd stoppen. Geen medisch advies.

The credits screen is called "Credits" in English and "Bronnen" in Dutch.

For the "Strong feelings" exercises the note is stronger and neutral, for example: *If this makes things worse, stop. If you are in distress, contact your doctor or a local helpline.* It makes no promise of benefit. The wording is agreed before that section is built.

Texts in the app stay descriptive ("what it is for") and do not make medical claims. The sources below were not checked for their health claims.

## 3. Content and licences

| Need | Decision | Why |
|------|----------|-----|
| Exercise texts, English and Dutch | **Write our own**, in our own words, and release them under the project's licence | The techniques are generic; only the wording of other people's scripts is protected, and a script of our own leaves no licence trail. |
| Bells and breathing tones | **Generate in code** | No licence question, no extra APK size. |
| Ambient sound (optional, later) | **Freesound, CC0 only**, with the sound id, author and URL recorded in the repository | Licences differ per sound, so only CC0 is kept. |
| Guided audio | **Free Mindfulness Project recordings, unchanged and credited** (version 1); Dutch recordings later | Text-to-speech or later own recordings. The English exercises of the Free Mindfulness Project could be used if its licence allows it; for Dutch no open source has been found yet, and the design leaves room to add one later. |

### Sources of existing exercises

| Source | Language | What | Licence status |
|--------|----------|------|----------------|
| [Free Mindfulness Project](https://www.freemindfulness.org/) (original project; also listed on an [NHS trust page](https://www.elft.nhs.uk/sites/default/files/import-news/download)) | English | Guided meditations in the MBSR and MBCT tradition, among them body scans and breathing exercises | The project's [download page](https://www.freemindfulness.org/download) states that all audio files are licensed **Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported**, free to download and distribute non-commercially, and asks to credit the original sources. The recordings are audio only (no scripts), English only, 32 exercises from 3 to 47 minutes, each credited on the page to a teacher or centre. Conditions that follow: credit the teacher and the project and link back; no commercial use (the app stays free and without advertising); an adaptation such as a translation would have to use the same licence, so the recordings are used unchanged. Items attributed to university centres are skipped because their own sites publish the same recordings under stricter terms. The about page does not repeat the licence, so the use in an app is checked on the download page before each release; writing to the project is optional while the app is free and ad-free, and needed if that ever changes (draft below). |

The first selection is by the project creator: Three Step Breathing Space (3:34), Three minute mindfulness of sounds (3:02) and Mountain meditation (8:12). The three MP3 files are in `app/src/main/res/raw/`, unchanged (about 1, 1.5 and 7.9 MB, 10.4 MB in total).

A Dutch source can be added later in the same way (confirmed licence, credit and link to the original); none is chosen now.

### Sources for ACT-style exercises

No ACT exercise with a clear open licence was found, and no open-source app with ACT exercises on F-Droid or GitHub could be confirmed. The plan is to write short exercises in our own words (the techniques are widely known; only the wording of other people's scripts is protected) and not to copy the wording of Russ Harris's "dropping the anchor".

| Source | Language | What | Licence status |
|--------|----------|------|----------------|
| [Gillanders ACT audio exercises](https://contextualscience.org/files/ACT_Audio_Exercises_Guide_and_Disclaimer_0.pdf) | English | Free ACT audio, such as "Leaves on a stream" and "Calling out your struggle" | Custom permission: free to use, copy and share without asking for non-commercial use. Not Creative Commons. Comes with a safety disclaimer (listen to a recording in full before using it; not while driving). Reference only unless the terms are confirmed for an app. |

Other free ACT pages exist, but none states an open licence, so none is used or linked here.

Sites that claim "CC BY" for body scan scripts on unrelated mirror domains were found and are not trusted.

### Draft message to the project (not sent, optional)

> Hello Peter, I am building a free, open-source, offline health journal app for Android with a small mindfulness area. I would like to include three of your recordings from the Free Mindfulness Project download page unchanged (Three Step Breathing Space, Three minute mindfulness of sounds, Mountain meditation), credited to you and the project with a link and the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 licence shown in the app. The app has no advertising, no payment and no network access. Is that use fine with you, and is there anything you would like the credit to say? Do you know of Dutch recordings under an open licence? Kind regards

## 4. Inspiration

- [Box Breathing](https://f-droid.org/en/packages/dev.cosmoslab.boxbreathing/) (F-Droid, MIT): a dot traces the square, vibration only, no network.
- [Iremi Breathing App](https://f-droid.org/packages/eu.ziclaud.iremibreathingapp) (F-Droid, MIT): several techniques, optional voice guidance, data stays local.
- Commercial meditation apps in general (impressions only, none named or studied): short sessions, a clear path for beginners, plain language, credible teachers. Taken over: short sessions, a "start here" path, one calm screen per exercise. Not taken over: courses, subscriptions, gamification.

## 5. References

- Free Mindfulness Project (original project): <https://www.freemindfulness.org/>
- Free Mindfulness Project on an NHS trust page (licence summary): <https://www.elft.nhs.uk/sites/default/files/import-news/download>
- Gillanders ACT audio exercises, guide and disclaimer: <https://contextualscience.org/files/ACT_Audio_Exercises_Guide_and_Disclaimer_0.pdf>
- Freesound licence discussion (community summary, not the official text): <https://freesound.org/forum/legal-help-and-attribution-questions/41786/93246>
- Box Breathing app: <https://f-droid.org/en/packages/dev.cosmoslab.boxbreathing/>
- Iremi Breathing App: <https://f-droid.org/packages/eu.ziclaud.iremibreathingapp>
