# How to change a range label

A *range label* is the text and colour shown next to a BMI, blood pressure or glucose value, for example "High blood pressure · from 140/90". Labels are sensitive: they must read as information, never as a diagnosis. This guide explains the rule and where the code lives.

## The rule

The authoritative text is the [range-labels spec](../../../openspec/specs/health-metrics/range-labels/spec.md). In short:

- A label reads **"name · range"**.
- It **never names a medical condition** (diabetes, prediabetes, hypertension and similar) and never gives advice, a warning or an instruction.
- The thresholds follow the **NHG** standards first. Other Dutch sources (Diabetes Fonds, DVN, Hartstichting) may inform wording but never override an NHG threshold. The reasoning is in [ADR 0005](../../adr/0005-dutch-nhg-guidelines.md).
- Colours come from one **neutral ramp** (a darker step is a higher band, never a verdict). See [ADR 0006](../../adr/0006-health-trend-visualizations.md).
- The source and a not-a-diagnosis note are reachable from every screen that shows a label ("About these ranges").

**Waist circumference is the exception.** Its labels follow the wording of the Dutch authority (Voedingscentrum, in line with NHG): Healthy, Increased risk, High risk, with the sex-specific range. Do not neutralise them; change them only when the authority's wording changes. See the [waist circumference spec](../../../openspec/specs/health-metrics/waist-circumference/spec.md).

## Where the code lives

| What | File |
|------|------|
| The limits (blood pressure, BMI, glucose) | `domain/src/main/kotlin/nl/healthjournal/domain/model/nhg/` |
| Which range is shown for a glucose context | `app/src/main/java/nl/healthjournal/app/ui/nhg/GlucoseRanges.kt` |
| Label text | `app/src/main/java/nl/healthjournal/app/ui/nhg/NhgLabels.kt` and the `nhg_*` strings in both `strings.xml` files |
| Colours | `app/src/main/java/nl/healthjournal/app/ui/nhg/NhgColors.kt` and `ui/theme/Color.kt` |
| Source sheet | `app/src/main/java/nl/healthjournal/app/ui/nhg/RangeSourceNote.kt` |

## Changing a limit

A limit is a medical-guideline statement, so a limit change needs a source.

1. Open an OpenSpec change that names the source and the exact text, and the affected capabilities (for example `health-metrics/blood-pressure`).
2. Change the classifier in `domain/…/model/nhg/` and its tests. Test both sides of every boundary.
3. If a glucose or blood pressure limit is shown in text, change the range in `GlucoseRanges.kt` and the strings. `GlucoseRangesTest` checks that the shown limits agree with the classifier.
4. Update ADR 0005 with the new source and date.
5. Update the README and the CHANGELOG.

## Renaming a band

Bands are stored by name. If you rename or merge band names, **old rows must still be readable**: add a read mapping like `NhgBloodPressureCategory.fromStoredName`, test every old name, and note in the CHANGELOG if an older app version can no longer read new data. See [Change the database](change-the-database.md).

## Changing wording or colours

1. Edit the `nhg_*` strings in **both** languages.
2. Run `./gradlew test`. `RangeLabelStringsTest` fails on condition names and mismatched placeholders.
3. Check the screen in light and dark theme and at a larger font size. Label text needs a contrast of at least 4.5:1.
