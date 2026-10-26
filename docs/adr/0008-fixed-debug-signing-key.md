# 8. Fixed Debug Signing Key for Sideloaded APKs

- **Date:** 2026-09-29
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Releases are debug-signed APKs built by GitHub Actions and sideloaded onto phones. The project had no signing configuration, so Gradle signed each build with the default `~/.android/debug.keystore` of whichever machine built it. A GitHub runner creates a fresh keystore on every run, and a developer PC has yet another one. Android refuses to update an installed app when the signature differs, so every new APK (and every `installDebug` from a PC over a CI-installed APK) failed with a signature mismatch. The only way forward was to uninstall first, which deletes the on-device database.

## Decision

Commit a dedicated keystore, `app/debug.keystore`, and point the `debug` signing config in `app/build.gradle.kts` at it (alias `androiddebugkey`, password `android`, valid for 100 years). Local and CI builds now produce the same signature, so a new APK installs over an older one and keeps the user's data.

## Release policy

Releases are **personal, sideloaded builds**: debug-signed with this public key and still `debuggable`. They are not store-grade and the README says so. Each release carries a SHA-256 file (`health-journal-<tag>-debug.apk.sha256`) next to the APK, so a downloader can check that the file is intact. The checksum comes from the same release, so it detects a damaged download, not a hostile release.

**Trigger for a private release keystore:** switch to a non-debuggable release build signed with a key held in CI secrets before any of these happens: the app is offered through a store, it is shared beyond people who know the project, or a third party depends on the signature to trust an update. That change needs a new ADR, because installs signed with this key cannot be updated in place and need the one-time export, uninstall and restore described below.

## Consequences

### Positive
- Sideloaded updates install in place; no data loss.
- No CI secrets to manage.

### Negative / Trade-offs
- The key is public, so anyone could sign an APK that Android accepts as an update to an installed copy. This is acceptable for a personal, non-store debug build. If the app is ever distributed more widely, switch to a private release keystore held in CI secrets.
- **One-time reinstall:** an app installed with an older, differently signed APK (v1.4.3 and earlier) must be uninstalled once before the first build signed with this key can be installed. Export data as CSV first.
