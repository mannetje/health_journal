# Developer Documentation

## Why
The project is growing from a Dutch personal tool into something outside contributors could join, first in the Netherlands, then Europe, then wider. Today the knowledge is spread over ADRs (why), OpenSpec specs (what) and the README (what the app is). Nothing explains how the code works or how to contribute, and there is no `CONTRIBUTING.md`. A stale `doc/` folder with a one-line Dutch placeholder also sits next to the real `docs/` folder.

Developer docs have two goals: let a newcomer learn the code and the architecture, and tell a contributor how to propose, build, test and submit a change. The model is narrative guides plus a first-change tutorial, with contributor guidance kept close to the code.

## What Changes
- **Language:** English only, for an international and European audience. The user-facing app stays English and Dutch.
- **`docs/dev/`:** getting started, an architecture tour, a walkthrough of one entry through every layer, how-to guides (add a metric, add a translation, change a range label, change the database), conventions, testing and the release process. ADRs and OpenSpec specs are linked, not copied.
- **`CONTRIBUTING.md`:** short entry point with the contribution flow (propose with OpenSpec, implement, test, document, changelog) and a pull request checklist.
- **GitHub templates:** pull request template, bug report and feature request issue templates.
- **Docs link check:** a small script, run in CI, that fails on broken relative links and on file paths in `docs/dev` that no longer exist.
- **Cleanup:** remove the stale `doc/` folder.
- **Plain markdown first.** A generated site (for example MkDocs Material on GitHub Pages) is a later option and needs no restructuring.

## Non-goals
- No translation of the developer docs.
- No code of conduct text in this change (use a standard one, decided separately).
- No API reference generation.

## Capabilities
- **Added Capability:** `developer-documentation`

## Impact
- Files: `docs/dev/**`, `CONTRIBUTING.md`, `.github/PULL_REQUEST_TEMPLATE.md`, `.github/ISSUE_TEMPLATE/*`, `scripts/check-docs.sh`, `.github/workflows/android.yml` (one step), README (link), removal of `doc/`.
- No app code, no version bump, no release.
