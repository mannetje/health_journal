## Why
An entry is a number and a time. The reason behind it ("after a run", "forgot my medicine", "new scale") is lost, and later the history cannot explain a spike. Other weight apps let you add a short comment to every entry and show it in the list. The journal should do the same for all entry types, kept on the device in the same database.

## What Changes
- **Optional comment on every entry:** weight, blood pressure, glucose, waist circumference and activity each get an optional free-text comment, entered when recording and editable afterwards.
- **Maximum length 200 characters.** Text is trimmed; an empty or blank comment means "no comment". The input shows a live counter and stops accepting text at the limit. The domain and import enforce the same limit, so a long file cannot bloat the database.
- **Shown in History:** a comment appears under the entry in the list (at most two lines, then an ellipsis) and in full in the edit dialog.
- **Same database:** a nullable `comment` column on the five entry tables, added by a real migration that keeps all rows. No new table, no new dependency.
- **CSV:** export and import gain a trailing `comment` column. Comments can contain commas and quotes, so CSV cells are now quoted per RFC 4180 when needed. Files without the column still import. The Libra weight import also reads its `comments` column.
- **Not in this change:** searching or filtering by comment, multi-line comments, attachments, and comments on medication intakes (those have their own note, see `add-medication-management`).

## Capabilities

### New Capabilities
- `entry-comments`: the optional comment on an entry, its limit, normalisation, input and display.

### Modified Capabilities
- `entry-management`: update semantics, the edit dialog and the untouched-fields rule cover the comment.
- `data-export`: a `comment` column in every metric file, CSV quoting, tolerant import, Libra comments.
- `platform-android`: the database moves from version 4 to 5 with a migration that adds the comment column.

## Impact
- `:domain`: `EntryComment` value object; the five entry models, record and update use cases take an optional comment.
- `:data`: five entities and the mapper, `MIGRATION_4_5`, CSV adapters (quoting, new column), Libra import.
- `:app`: a comment field in the logging form and the edit dialogs, display in `HistoryScreen`, strings in English and Dutch, ViewModel state.
- Docs: README, CHANGELOG (with an Upgrade note), ADR 0019, `docs/dev/how-to/add-a-metric.md` (comment step) and `change-the-database.md` (example list).
- **Version conflict to settle:** `add-medication-management` also plans `MIGRATION_4_5` (database version 5). Whichever ships first takes version 5 and the other becomes version 6 / `MIGRATION_5_6`. This change is small and is proposed to ship first, so the medication change would move to version 6 (see design).
