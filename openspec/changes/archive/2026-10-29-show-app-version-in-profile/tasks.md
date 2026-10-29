## 1. Build

- [x] 1.1 Enable `buildConfig` and add `CHANGELOG_ANCHOR` derived from the `CHANGELOG.md` heading of `versionName` (empty when absent)

## 2. UI

- [x] 2.1 Add `ChangelogLink.kt` with `changelogUrl(anchor)` and a unit test (with and without anchor)
- [x] 2.2 Add `AboutSection.kt` (version text, "What's new" link opening the URL) and show it at the end of `ProfileScreen`
- [x] 2.3 Pass the version name from `MainActivity`
- [x] 2.4 Add English and Dutch strings

## 3. Docs and checks

- [x] 3.1 CHANGELOG `[Unreleased]`, README feature line, docs/dev notes (releases: the heading must exist before the tag)
- [x] 3.2 Unit tests, lint and `scripts/check-docs.sh` pass
- [x] 3.3 Emulator pass with light and dark screenshots and a `docs/screenshots.md` entry
