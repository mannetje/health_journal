# 8. Fixed Debug Signing Key for Sideloaded APKs

- **Date:** 2026-09-29
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Releases are debug-signed APKs built by GitHub Actions and sideloaded onto phones. The project had no signing configuration, so Gradle signed each build with the default `~/.android/debug.keystore` of whichever machine built it. A GitHub runner creates a fresh keystore on every run, and a developer PC has yet another one. Android refuses to update an installed app when the signature differs, so every new APK (and every `installDebug` from a PC over a CI-installed APK) failed with a signature mismatch. The only way forward was to uninstall first, which deletes the on-device database.

## Decision

Commit a dedicated keystore, `app/debug.keystore`, and point the `debug` signing config in `app/build.gradle.kts` at it (alias `androiddebugkey`, password `android`, valid for 100 years). Local and CI builds now produce the same signature, so a new APK installs over an older one and keeps the user's data.

## Consequences

### Positive
- Sideloaded updates install in place; no data loss.
- No CI secrets to manage.

### Negative / Trade-offs
- The key is public, so anyone could sign an APK that Android accepts as an update to an installed copy. This is acceptable for a personal, non-store debug build. If the app is ever distributed more widely, switch to a private release keystore held in CI secrets.
- **One-time reinstall:** an app installed with an older, differently signed APK (v1.4.3 and earlier) must be uninstalled once before the first build signed with this key can be installed. Export data as CSV first.
