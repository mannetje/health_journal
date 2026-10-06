## Context
Entries live in five tables (`weights`, `blood_pressures`, `glucoses`, `waist_circumferences`, `activities`) behind one `HealthLogRepositoryPort`. Each has an entry model in `:domain`, record and update use cases, a mapper, a CSV adapter and an edit dialog. The database is at version 4. CSV cells are currently split on plain commas, so a comma inside a value would break a row. Storage is metric and locale-independent (ADR 0014); ViewModels carry no translated text (ADR 0015).

## Goals / Non-Goals
**Goals:** one small, uniform optional comment on all five entry types; bounded size; safe migration; lossless CSV round trip; History shows it.
**Non-Goals:** search, filter, rich text, multi-line text, attachments, medication intake notes, sync.

## Decisions
- **Length limit 200 characters.** Enough for a sentence or two, small enough that a list of thousands of entries stays fast and a CSV stays small. Counted on the trimmed text. Enforced in three places: the `EntryComment` value object (rejects longer text), the input field (stops at 200, shows `n/200`), and import (a longer cell skips the row with a reason instead of being cut silently, so data is never changed without telling the user).
- **Normalisation:** trim leading and trailing whitespace; blank becomes `null`; line breaks are replaced by a single space, so the field is single-line and list rows stay compact. `EntryComment.ofOrNull(text)` returns `null` for blank input and throws for over-limit input.
- **Storage:** a nullable `comment TEXT` column on each of the five tables. Alternative considered: one `comments` table keyed by entry id. Rejected: it needs joins and five-way referential logic for one optional string, while a column keeps the migration trivial. No index (the content is never queried).
- **Migration `MIGRATION_4_5`:** five `ALTER TABLE ... ADD COLUMN comment TEXT` statements. Existing rows get `NULL`. Not destructive (see `change-the-database`). An older app version cannot read a version 5 database; downgrade is unsupported and noted as an Upgrade note.
- **Update semantics:** the comment is part of every update. Passing `null` clears it. The edit dialog pre-fills the stored comment; unchanged text keeps the stored value (same rule as other untouched fields).
- **Entry models:** add `val comment: EntryComment? = null` as the last property of each entry, so existing constructors and tests keep compiling.
- **CSV quoting (RFC 4180):** on export a cell is wrapped in double quotes when it contains a comma, a double quote or a line break, with inner quotes doubled. On import the row splitter understands quoted cells. This changes parsing for all CSV files, but is backward compatible: files without quotes parse as before.
- **CSV columns:** `comment` is appended as the last column of the weight, blood pressure, glucose, waist and activity files. Import reads it when the header has it and treats a missing column as no comment, so older files keep working. Example: `timestamp,weight_kg,bmi,comment`.
- **Libra:** the weight import maps the `comments` column to the comment, normalised; an over-limit comment skips the row with a reason, like other row errors.
- **UI:** the logging form gets a single-line "Comment (optional)" field with a counter, under the main input of each metric tab. In History the comment is shown below the value in a secondary text style, at most two lines with an ellipsis (as in the reference screenshots). The edit dialog shows the full text. Layout follows ADR 0010 (long Dutch strings, large fonts).
- **ADR 0019:** records the optional per-entry comment as a nullable column on each entry table, the 200-character limit and the CSV quoting change.
- **Ordering with `add-medication-management`:** that change plans database version 5. This change proposes to ship first as version 5. When it ships, the medication change is edited to version 6 / `MIGRATION_5_6` (its design, task 1.4 and platform-android delta) and its Upgrade note adjusted. If medication ships first instead, this change takes version 6 / `MIGRATION_5_6`; task 3.1 covers that.

## Risks / Trade-offs
- Personal notes are health data. They stay on the device like every other value, but export files contain them, so the export UI says that comments are included.
- Switching CSV parsing affects all imports. Mitigated by tests with and without quotes, and by keeping the semicolon-based Libra parser separate.
- Single-line only is a limit users may want lifted. A later change can raise it without a migration because the column is plain text.
- No search means comments are only readable by scrolling. Acceptable for now.

