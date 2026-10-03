# Contributing to Health Journal

Thank you for helping. Health Journal is a private, offline health logging app for Android. The documentation for developers is in [`docs/dev/`](docs/dev/README.md); start there to learn the code and the architecture.

Contributions in English are welcome; the app itself is in English and Dutch, and more languages are welcome ([Add a translation](docs/dev/how-to/add-a-translation.md)).

## Ground rules

- **Privacy first.** Data stays on the device. No analytics, no network calls with user data.
- **Information, not advice.** The app is not a medical device. Text never diagnoses, warns or advises ([conventions](docs/dev/conventions.md)).
- **Keep it small.** Prefer the platform and official libraries over new dependencies ([ADR 0003](docs/adr/0003-dependency-minimization.md)).

## How to contribute

1. **Open an issue first** for anything bigger than a small fix, so we can agree on the direction.
2. **Propose with OpenSpec** for a change in behaviour: a short `proposal.md`, a `tasks.md` and delta specs under `openspec/changes/<name>/`. Look at an archived change in `openspec/changes/archive/` for an example. Check it with:

   ```bash
   npx --no-install openspec validate <name> --strict
   ```

   Typo fixes, small bug fixes and doc changes do not need a proposal.
3. **Implement** following [Getting started](docs/dev/getting-started.md) and [Conventions](docs/dev/conventions.md).
4. **Test:** add tests and run `./gradlew test` ([Testing](docs/dev/testing.md)).
5. **Update the docs** that your change touches: README, ADRs, specs, strings in both languages, and a line under `Unreleased` in `CHANGELOG.md`.
6. **Open a pull request** against `master` and fill in the template.

## Pull request checklist

- [ ] `./gradlew test` passes
- [ ] New or changed behaviour has tests
- [ ] English and Dutch strings are both updated
- [ ] No condition names, advice or warnings in user-facing text
- [ ] Data is stored in metric units; database changes have a migration and no destructive fallback
- [ ] README, ADRs and OpenSpec specs are up to date
- [ ] CHANGELOG has an entry under `Unreleased`
- [ ] Docs checks pass: `bash scripts/check-docs.sh`

## Commit messages

Use a prefix (`feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`), an imperative first line, and a body that explains why.

## Reporting a security problem

Do not open a public issue for a vulnerability. Use GitHub's private vulnerability reporting on the repository's Security tab.

## Licence

By contributing you agree that your contribution is licensed under the project's licence.
