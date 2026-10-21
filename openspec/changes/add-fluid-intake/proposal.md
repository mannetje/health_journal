# Add Fluid intake

## Why
Some people want to jot down what they drink during the day: because they were asked to keep a drinking list, because they limit fluids on a care team's instruction, or simply out of curiosity. Notes on paper or in a phone notes app are clumsy: every sip needs a number. The app has no place for it. The easiest possible log is a single tap per glass, cup or bottle, with the sizes the person uses at home, and a daily total to look at afterwards. A person who has a limit from their care team can enter that number themselves and see it next to the total. The app records and shows what is entered and sets no target of its own.

## What Changes
- **Fluid intake card on the Log tab** with three quick-add buttons (default "+1 glass", "+1 cup", "+1 bottle"), each adding one entry of the person's own size in ml at the current time with one tap, and a custom entry (millilitres 10 to 5000, date and time default now, optional 200-character comment). A plain line shows "Today: N ml" (the total of the local calendar day), or "Today: N of L ml" when the person has set a personal limit L. After a quick-add a short "Added 250 ml, Undo" message lets the person take the tap back.
- **Own sizes:** the three quick-add sizes and their short names are editable by the person (10 to 5000 ml each) and kept in preferences. Defaults are 150, 250 and 500 ml. They are a convenience, not a recommendation.
- **Optional personal limit:** a daily limit in ml (100 to 10000) that only the person sets, for example from a fluid restriction by their care team. The field is empty until the person fills it; the app never suggests, prefills or recommends a number. The limit is shown as plain numbers on the Today line and as one thin dashed line on the graph. Nothing changes colour, no alert or notification is sent, and no "over", "exceeded" or "remaining" wording or progress ring exists. The person can change or clear it at any time; this never alters stored entries.
- **History:** every tap is an entry; entries are listed in the merged timeline with edit and delete and a day-total row per day under the Fluid filter, plus a Fluid filter chip.
- **Graph view under the Fluid filter:** one bar per day with the day's total, with the shared range chips, one thin dashed line at the person's limit when set, and plain figures for the range: average per day, highest day and number of days logged. No goal, no ring, no colour threshold.
- **Export and backup:** CSV export and import (`timestamp,ml,comment`; entries only) and, per profile in the `.hjbackup` document, a `fluidEntries` list and a `fluidDailyLimitMl` value (number or null). The quick-add sizes are settings and are not in the backup.
- **Notice:** the Profile notice says the app records what you enter and does not assess it or set a target; a limit shown is the person's own number.
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `health-metrics/fluid-intake` (quick add, custom entry, validation, daily total, own sizes, personal limit set by the person, no target or assessment by the app)
- **Modified Capability:** `entry-management` (fluid entries and day totals in History, edit rules, delete)
- **Modified Capability:** `entry-comments` (the comment on custom fluid entries)
- **Modified Capability:** `health-trends` (daily-total bar view, limit line and figures)
- **Modified Capability:** `data-export` (fluid CSV export and import)
- **Modified Capability:** `data-backup` (fluid list and limit in the backup document)
- **Modified Capability:** `compliance` (no target set by the app; the person's own limit shown back as a number)
- **Modified Capability:** `privacy` (same protection as other entries, including the limit)
- **Modified Capability:** `localization` (text in English and Dutch)

## Impact
- **Database:** Room version bump with one migration that is the **next free number at implementation time (current database version + 1)**, so the change does not depend on the order in which features are built. It creates `fluid_intakes` (id, profile id, timestamp, ml, nullable comment, index on profile and timestamp) and `fluid_settings` (profile id as primary key, nullable `daily_limit_ml`). Existing tables are untouched, so old data stays readable and an upgrade only adds empty tables. A migration test opens a database of the previous version with data.
- **Stored data shape:** the backup document gains `fluidEntries` and `fluidDailyLimitMl` per profile; `schemaVersion` stays 1 (readers ignore unknown properties, older files restore with no entries and no limit). `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated.
- **Preferences:** three size values and three short names only; no health entries and no limit are kept in preferences.
- **Affected code:** `FluidIntake`, validation, daily grouping and statistics functions and use cases in `:domain` (JVM tests including empty range, day boundaries, limit set and cleared); entities, DAOs, mapper, migration, repository methods and CSV adapters in `:data`; Log card with quick-add, size and limit settings, History rows with day totals and the bar graph in `:app`.
- **Dependencies and permissions:** none. No notifications are added.
- **Docs:** README, CHANGELOG, `docs/dev/` pages, `docs/dev/zib-mapping.md`, screenshots (English and Dutch, light and dark), `docs/backup-format/`.

## Sources
Read (all verified through search result summaries or page fetches during drafting; pages that failed are listed below):
- Voedingscentrum, [Hoeveel water moet ik elke dag drinken?](https://www.voedingscentrum.nl/nl/thema/eetvragen/hoeveel-water-moet-ik-elke-dag-drinken-.aspx): general advice for adults and the fact that tea, coffee and milk count towards the total. Search summary only; the page itself was not fetched.
- Hospital leaflets on fluid restriction in heart failure (for example [Jeroen Bosch Ziekenhuis, vochtbeperking 1500 ml](https://www.jeroenboschziekenhuis.nl/behandelingen/hartfalen-voedingsadvies-zoutbeperking-en-vochtbeperking-van-1500-ml), [Viecuri](https://www.viecuri.nl/media/brochures/cardiologie/algemeen/hartfalen/27829.pdf), [Zaans Medisch Centrum](https://www.zaansmedischcentrum.nl/sites/default/assets/File/patientfolders/documents/628/8becc737-0a3d-47b6-bd55-13653a008fee/Document.pdf)): the personal limit is set by the person's own doctor and differs per person; soup, yoghurt and similar count as fluid in those leaflets; leaflets suggest measuring with a jug or a list. Search summaries.
- Thuisarts.nl, [hartfalen](https://www.thuisarts.nl/hartfalen/ik-heb-hartfalen): fetched; it gives no figure and no restriction, only "drink enough, spread over the day". Not used for any figure.
- Nictiz zib Vochtbalans / [FluidBalance](https://zibs.nl/wiki/FluidBalance-v1.0.1(2024EN)): found through search. It records total fluid intake and output over a period in ml for a care team's approach to drinking; it is a clinical building block. No zib for a person's own drinks, "Voeding" or "Drinken" was found. The zib pages and PDFs answered 503, so the element list was not read; the mapping must be checked before the final text.

Could not read: Voedingscentrum `drinken.aspx` and the older Q&A address (404); the zib PDF and wiki (503); the Hartstichting's own pages (not found by search, only hospital leaflets); the NHG-Standaard (login); no Thuisarts page on vochtbeperking was found.

Deliberately NOT copied: any recommended amount (the 1.5 to 2 litres of Voedingscentrum, the 1.5 or 2 litres of hospital restriction leaflets, extra amounts for heat, age or illness), any minimum or maximum as a default or hint, any wording that tells a person to drink more or less, and the "does it count as fluid" lists. The only numbers in the app are those the person types, including their own limit, or their own sizes.

## Non-goals
- No target, goal, reminder or "drink more" nudge set or suggested by the app; no progress ring, bar fill or percentage toward an amount; no hydration verdict and no advice, including when to drink or when to contact a care provider.
- The personal limit has no default, hint, example value or prefill, no alert, notification, colour change or verdict wording ("over", "exceeded", "remaining", "left"), and is not included in the CSV.
- No type of drink in version 1 (see planned later addition).
- No import from smart bottles, Bluetooth or Health Connect.
- No link to medication, the pillbox or any other metric, and no output (urine) recording.

## Planned later addition
An optional type of drink with the default "water", added by a later migration as a nullable column that reads as water when empty. It would carry no nutrition, caffeine or sugar framing. It is not in the version 1 data model, specs or tasks.

## Open questions
- Dutch short names for the three buttons ("Glas", "Kopje", "Fles") and the limit label ("Jouw limiet (door jou of je zorgteam ingesteld)") are for the final wording review.
- Whether non-drink fluids (soup, yoghurt) are logged is left to the person; the app does not say what counts.
