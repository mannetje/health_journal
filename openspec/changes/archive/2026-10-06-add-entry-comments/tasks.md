## 0. Decisions to confirm
- [x] 0.1 Maximum length 200 characters (or 140 / 280) (approved)
- [x] 0.2 Single-line comment, line breaks become a space (approved)
- [x] 0.3 Ship before `add-medication-management` (database version 5), and move medication to version 6 (approved)
- [x] 0.4 History only, no trend-chart tooltip; chart tap-to-inspect is a separate later change (approved)

## 1. Domain
- [x] 1.1 `EntryComment` value object in `model/metrics/` (trim, blank is null, line breaks to a space, max 200, `ofOrNull`)
- [x] 1.2 Add `comment: EntryComment? = null` to `WeightEntry`, `BloodPressureEntry`, `GlucoseEntry`, `WaistCircumferenceEntry` and `ActivitySession`
- [x] 1.3 Record and update use cases for all five types take an optional comment
- [x] 1.4 Tests: limit boundary (200 and 201), blank, trim, line break, update clears the comment

## 2. Data
- [x] 2.1 Add `comment` (nullable) to the five entities
- [x] 2.2 Database version 5, `MIGRATION_4_5` with five `ALTER TABLE ... ADD COLUMN comment TEXT`, registered with every earlier migration (never destructive)
- [x] 2.3 Mapper both ways, including `null`
- [x] 2.4 CSV: RFC 4180 quoting on export, quote-aware row splitting on import, trailing `comment` column on the five files, older headers still accepted
- [x] 2.5 Libra import reads the `comments` column; over-limit Libra comments are clipped to the first 200 characters (standard CSV still skips with "Comment longer than 200 characters")
- [x] 2.6 Tests: mapper round trip, CSV with comma, quote and line break, older file without the column, over-limit row, Libra comment, migration (install release 1.5.0 over a new build on the emulator and check old rows)

## 3. App
- [x] 3.1 Check the database version of `add-medication-management` and renumber whichever change ships second
- [x] 3.2 Comment field with `n/200` counter on every logging tab, cleared after save, not required
- [x] 3.3 Comment in the five edit dialogs, pre-filled, unchanged text keeps the stored value
- [x] 3.4 History rows show the comment (two lines, ellipsis, no gap when empty)
- [x] 3.5 Strings in `values` and `values-nl` (label, counter, export note); no hard-coded text
- [x] 3.6 ViewModel tests: save with and without comment, edit, clear, over-limit input stops at 200
- [x] 3.7 Layout check in English and Dutch at the large font size (ADR 0010), light and dark (checked light and dark; Dutch at the large font not re-checked on the emulator)

## 4. Docs
- [x] 4.1 Archive this change into `openspec/specs` (new `entry-comments` spec, rewrite its TBD Purpose) after shipping
- [x] 4.2 ADR 0019 (optional per-entry comment, limit, CSV quoting), with a Mermaid diagram
- [x] 4.3 README (feature list, CSV table), CHANGELOG 1.5.1 with an Upgrade note about database version 5
- [x] 4.4 Update `docs/dev/how-to/add-a-metric.md` (comment step) and `change-the-database.md` (migration list and next version)
- [x] 4.5 Run `scripts/check-docs.sh` and `npx --no-install openspec validate --all --strict`
