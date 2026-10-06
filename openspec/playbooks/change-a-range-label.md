# Playbook: change a range label or limit

Detail: [How to change a range label](../../docs/dev/how-to/change-a-range-label.md).

## Required decisions
- Which metric: BMI, blood pressure and glucose use the neutral "name · range" form; waist keeps the authority's wording.
- Source and exact text for any limit change. No source, no change.
- If a band is renamed or merged: how old stored names are still read.

## Steps
1. Propose an OpenSpec change naming the source and the affected capabilities.
2. Change the classifier in `domain/` with tests on both sides of every boundary.
3. Update `GlucoseRanges.kt` and the `nhg_*` strings in both languages.
4. Update ADR 0005 with the source and date; update README and CHANGELOG.

## Done when
- `RangeLabelStringsTest` and `GlucoseRangesTest` pass.
- No condition name or advice appears in a neutral label.
- Checked in light and dark theme and at a large font size.
