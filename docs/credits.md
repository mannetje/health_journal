# Credits

Outside material in the app, with its author, source and licence. The same list is shown in the app on the Mindfulness Credits screen ("Bronnen" in Dutch), which links to the originals and to the licence texts. The source code of the project is under the Apache License 2.0 (`LICENSE`, `NOTICE`); the material below is not covered by it and stays under its own licence.

## Recorded exercises

All three are by Peter Morgan, from the [Free Mindfulness Project](https://www.freemindfulness.org/download), and are used unchanged (not re-encoded, trimmed or mixed). The files are in `app/src/main/res/raw/`.

| Exercise | File | Length | SHA-256 |
|----------|------|--------|---------|
| Three Step Breathing Space | `fmp_three_step_breathing_space.mp3` | 3:34 | `03bd1eb861934eb5c9bdc953f564e63a1f3c14388b22779ab744cd5cf3c8febb` |
| Three minute mindfulness of sounds | `fmp_mindfulness_of_sounds.mp3` | 3:02 | `ed67f06bf51425e84d8dabd6d4ded0966a3f809baa94152e026105def9893a7b` |
| Mountain meditation | `fmp_mountain_meditation.mp3` | 8:12 | `3fa1069e7af1e1b01a4c7487e1b1ee5d5e088561cc7c7480a03554631ea9a2ac` |

Licence: [Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported](https://creativecommons.org/licenses/by-nc-sa/3.0/), as stated on the project's download page when the files were added (2026-10-21). Because of the NonCommercial term the app stays free of charge and without advertising while it contains these recordings.

## Made for the app

The texts (English and Dutch), the pictures and animations, and the tones and bells (generated in code) are made for this app and are under the licence of the project.

The data lives in `GuidedExerciseCatalogue` ([`GuidedExercise.kt`](../domain/src/main/kotlin/nl/healthjournal/domain/model/mindfulness/GuidedExercise.kt)); a new recording is added there with its credit and to this file in the same change.
