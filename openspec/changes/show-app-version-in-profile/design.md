## Context

`MainActivity` already reads `versionName` from the package manager. `CHANGELOG.md` lives on GitHub at `mannetje/health_journal`, and release headings look like `## [1.6.3] - 2026-10-28`.

## Goals / Non-Goals

**Goals:** show the installed version; open the changelog at that version.
**Non-Goals:** checking for updates, showing the changelog inside the app, any network access.

## Decisions

### D1: The anchor comes from `CHANGELOG.md` at build time

GitHub builds the heading anchor from the whole heading, date included (`[1.6.3] - 2026-10-28` gives `163---2026-10-28`). The app does not know the date. `app/build.gradle.kts` therefore reads the heading for `versionName` and writes the anchor into `BuildConfig.CHANGELOG_ANCHOR`; it is an empty string when no heading matches.

Rejected: linking to the release page (not the changelog the user asked for); storing the date next to `versionName` (a second value to keep in step at release time).

### D2: A pure URL builder

`changelogUrl(anchor)` returns the file URL, with `#anchor` only when the anchor is not blank. It is unit tested; the composable only opens the result with `LocalUriHandler`, as the other external links do.

## Risks / Trade-offs

- If a heading format ever changes, the link falls back to the top of the file; it never breaks.
- The link needs a browser and a connection; without them the system shows its own message.
