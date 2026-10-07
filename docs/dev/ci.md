# Continuous integration

Every push and pull request is checked by GitHub Actions. This page lists what runs, when, what you see, and how to run the same checks on your own machine first. The workflow files are in `.github/workflows/`.

## Overview

| Workflow | File | Runs on | What it does | Blocks a merge? |
|----------|------|---------|--------------|-----------------|
| **Android CI & Build APK** | `.github/workflows/android.yml` | push and pull request to `master`, tags `v*`, manual | Checks the docs, runs the unit tests, measures coverage, builds the debug APK, publishes a release on a tag | Yes (the check is called "Build & Test Android APK") |
| **Android Lint** | `.github/workflows/lint.yml` | push and pull request to `master`, manual | Runs Android Lint on the app. Lint errors fail the check, warnings do not | Yes, once you make it a required check |
| **OpenSpec** | `.github/workflows/specs.yml` | push and pull request that change `openspec/**`, manual | Validates all specs and open changes in strict mode | Yes, once you make it a required check |
| **CodeQL** | `.github/workflows/codeql.yml` | push and pull request to `master`, every Monday | Static security analysis of the Kotlin code | Findings show under Security, Code scanning |
| **Dependabot** | `.github/dependabot.yml` | every Monday | Opens pull requests for newer dependency and action versions | No, it only proposes changes |

"Blocks a merge" depends on branch protection, which is a GitHub setting and not part of the repository. See [Recommended GitHub settings](#recommended-github-settings).

## Android CI & Build APK

The main workflow. In order:

1. **Check developer docs** runs `scripts/check-docs.sh`: relative links must resolve and source paths named in `docs/dev` must exist.
2. **Run Tests** runs `./gradlew test` for all three modules ([Testing](testing.md)).
3. **Unit test coverage** runs JaCoCo for `domain`, `data` and `app`. This step never fails the build.
4. **Code statistics and coverage summary** runs `scripts/job-summary.sh`, which writes a lines-of-code table (cloc) and the line coverage per module to the run summary page.
5. **Upload coverage reports** attaches the HTML and XML reports to the run as the `coverage-reports` artifact (kept 14 days).
6. **Build Debug APK** runs `./gradlew assembleDebug`.
7. On a normal run the APK is uploaded as the `health-journal-debug-apk` artifact, so you can install a build of any pull request.
8. On a **tag** `vX.Y.Z` the APK is renamed, the release notes are taken from `CHANGELOG.md` and a GitHub Release is published ([Releases](releases.md)).

Where to look: open the run in the **Actions** tab. The summary page at the top shows the code statistics and the coverage table. Artifacts are at the bottom.

## Android Lint

Runs `./gradlew :app:lintDebug`. The HTML report is uploaded as an artifact on every run, also when lint fails. Lint reports errors (they fail the check) and warnings (they do not). Fix an error in the code. Do not silence it unless you can explain why it is a false positive; if you do, put the suppression next to the code with a comment.

## OpenSpec

Runs `openspec validate --all --strict` with a pinned version, but only when a pull request or push changes something under `openspec/`. It protects the rule that every spec uses SHALL or MUST with WHEN and THEN scenarios ([Conventions](conventions.md)).

## CodeQL

Analyses the Java and Kotlin code for security problems, on every push and pull request and once a week (Monday) so new rules are applied to old code. It builds the app by hand (`assembleDebug`) because the project uses Kotlin.

Results are under **Security, Code scanning** in the repository. A pull request that adds a problem gets an annotation on the changed line. CodeQL is configured in this workflow file; do not also switch on the "default setup" in the repository settings, because the two conflict.

## Dependabot

Every Monday Dependabot looks for newer versions of two things and opens grouped pull requests:

- **Gradle dependencies**, in three groups so that tied libraries move together: Kotlin, KSP, Compose and Room; the other AndroidX libraries; and the test libraries. Major version bumps are ignored on purpose: they are reviewed and done by hand.
- **GitHub Actions** used by the workflows, in one group. The workflows pin an action to a major version tag.

Commit messages start with `chore(deps)` or `chore(ci)`.

How to handle a Dependabot pull request:

1. Wait for the checks. A green build, tests, lint and CodeQL on the pull request branch is the safety net.
2. Read the release notes in the description for anything that touches how you use the library or action.
3. For an app dependency, install the APK artifact and try the app on a device.
4. Merge. Dependabot rebases its other open pull requests by itself.

Dependabot never merges anything on its own, and it cannot see new versions of libraries outside Gradle, such as the pinned OpenSpec version in `.github/workflows/specs.yml`. Check that one by hand now and then.

## Run the same checks locally

Run these before you push. They are what CI runs.

```bash
bash scripts/check-docs.sh                       # docs links and paths
npx --no-install openspec validate --all --strict  # specs, when you touched openspec/
./gradlew test                                   # unit tests, all modules
./gradlew :app:lintDebug                         # Android Lint
./gradlew assembleDebug                          # the APK builds
```

The coverage tasks are listed in [Testing](testing.md#coverage). CodeQL has no useful local equivalent; read its annotations on the pull request.

## Recommended GitHub settings

These are repository settings that only an admin can change, so they are not in the workflows. Suggested for `master`:

- Branch protection or a ruleset that requires the checks **Build & Test Android APK** and **Android Lint** to pass before merging a pull request. Do not require the OpenSpec check: it does not run when `openspec/` is untouched, and a required check that never runs blocks the merge.
- **Secret scanning** and **push protection**, so a key or token that is committed by accident is caught.
- Leave the CodeQL default setup off (see above).

## Adding or changing a workflow

- Keep the same triggers: pull request and push to `master`, plus `workflow_dispatch` so you can start it by hand.
- Use `ubuntu-latest`, JDK 21 (Temurin) and the Gradle cache as the existing workflows do.
- Give a job only the permissions it needs.
- A new check that may be noisy starts with `continue-on-error: true` until a run is clean; then remove the line so it counts.
- Add the workflow to the table at the top of this page.
