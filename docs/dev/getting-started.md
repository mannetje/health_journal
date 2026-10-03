# Getting started

## Prerequisites

- **JDK 21** (the Gradle build targets Java 21).
- **Android Studio** (current stable) or the Android SDK command-line tools. The project compiles against SDK 36, targets SDK 35 and supports Android 8.0 (API 26) and newer.
- An emulator or a device to run the app.
- **Node.js** is only needed for the OpenSpec command-line tool (`npx openspec ...`).

## Get the code

```bash
git clone https://github.com/mannetje/health_journal.git
cd health_journal
```

Open the folder in Android Studio and let Gradle sync. Android Studio writes `local.properties` with your SDK path; it is not committed.

## The three modules

| Module | What it is | Android? |
|--------|------------|----------|
| `domain` | Pure Kotlin: model, rules, use cases, ports | No |
| `data` | Room database and CSV adapters | Yes |
| `app` | Jetpack Compose screens and ViewModels | Yes |

See [Architecture](architecture.md) for how they relate.

## Build, test, run

Run these from the repository root. On Windows use `./gradlew` in PowerShell or `gradlew.bat` in `cmd`.

Run all unit tests (this is what CI runs):

```bash
./gradlew test
```

Build a debug APK:

```bash
./gradlew assembleDebug
```

Install it on a running emulator or connected device:

```bash
./gradlew installDebug
```

Test a single module, which is much faster while you work on the domain:

```bash
./gradlew :domain:test
```

The debug APK is signed with a fixed key committed in `app/debug.keystore`, so updates install over earlier debug builds (see [ADR 0008](../adr/0008-fixed-debug-signing-key.md)). Never use this key for anything but debug builds.

## Where things are

```text
domain/   pure Kotlin: model/, usecase/, port/
data/     local/ (Room), csv/, repository/, DataModule.kt
app/      HealthJournalApp.kt, MainActivity.kt, ui/, settings/, res/
docs/     adr/, dev/, screenshots/
openspec/ specs/ (what the system does), changes/ (proposed work)
```

## Next

Read [Architecture](architecture.md), then follow [Life of an entry](life-of-an-entry.md).
