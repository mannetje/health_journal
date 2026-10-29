## Why

People cannot see which version of the app they have installed, and there is no way from the app to read what changed in it. The version is needed when reporting a problem or checking for an update, and the changelog already lists every change per version.

## What Changes

- **Profile tab**: a new "About" section at the end shows "Version X.Y.Z" and a "What's new" link.
- The link opens `CHANGELOG.md` on GitHub in the browser, scrolled to the section of the installed version. When the changelog has no section for that version (a development build), the link opens the top of the file.
- The section anchor is worked out at build time from the heading in `CHANGELOG.md` (`## [1.6.3] - 2026-10-28` becomes `#163---2026-10-28`) and stored in a `BuildConfig` field. Nothing has to be kept in step by hand at release time, because the release process already adds the heading in the same commit that bumps `versionName`.
- No network code in the app: the browser handles the link. No new permission, no database change.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `profile`: the Profile screen shows the installed version and a link to the changelog.

## Impact

- `app/build.gradle.kts` — `BuildConfig` enabled; `CHANGELOG_ANCHOR` derived from `CHANGELOG.md`.
- `app/src/main/java/nl/healthjournal/app/ui/profile/` — new `AboutSection.kt` and `ChangelogLink.kt` (URL builder), `ProfileScreen.kt` shows the section.
- `app/src/main/java/nl/healthjournal/app/MainActivity.kt` — passes the version name to the screen.
- `strings.xml` (EN and NL), `CHANGELOG.md`, `README.md`, `docs/dev`, `docs/screenshots.md`.
