# Conventions

Short rules that keep the code and the docs consistent. When a rule has a reason, the reason is linked.

## Architecture

- `domain` is pure Kotlin: no Android, no Room, no Compose. Dependencies point inward: `app` and `data` depend on `domain`, never the reverse ([ADR 0002](../adr/0002-hexagonal-architecture.md)).
- One use case per class, with a single `operator fun invoke`.
- Wiring is a plain constructor call in `HealthJournalApp` ([ADR 0003](../adr/0003-dependency-minimization.md)). No DI framework.
- Prefer the Android SDK, AndroidX and official Kotlinx libraries. A new third-party dependency needs an ADR.

## Data

- Store metric units only. Units are a display setting ([ADR 0014](../adr/0014-units-presentation.md)).
- Never use destructive database migrations. See [Change the database](how-to/change-the-database.md).
- Invalid values must not be constructible: validate in the domain value object.

## Text and UI

- No hard-coded user-facing text. Strings go in English and Dutch together ([Add a translation](how-to/add-a-translation.md)).
- ViewModels expose `UiText`, not resolved strings ([ADR 0015](../adr/0015-localized-viewmodel-messages.md)).
- Layouts must survive long Dutch text and larger fonts ([ADR 0010](../adr/0010-responsive-dutch-ui-layout.md)).
- Touch targets are at least 48 dp and icons have a content description.
- Ask `isAppDarkTheme()` (in `ui/theme/Theme.kt`) whether dark is active, never `isSystemInDarkTheme()`, so the Light, Dark and System choice in Profile applies everywhere ([ADR 0017](../adr/0017-in-app-theme-choice.md)).
- The app is a logging tool, not a medical device. Text gives information only: no advice, no diagnosis, no condition names ([range labels](how-to/change-a-range-label.md)).

## Kotlin style

- Follow the official Kotlin style and the style of the file you are editing.
- Name things after the domain, not the technology (`BloodPressureReading`, not `BpDto`).
- Comment why, not what. Keep comments short.

## Commits

- Use a short prefix: `feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`.
- First line in the imperative, under about 72 characters. The body explains why.
- One logical change per commit.

## Architecture Decision Records

Write an ADR when you make a choice that is costly to reverse or that a newcomer would question: a new dependency, a storage format, a rule about wording. ADRs live in `docs/adr/`, numbered in order. Copy the structure of the latest one (context, decision, consequences) and link it from the README list. Do not rewrite an accepted ADR: add a new one that supersedes it.

## Specs (OpenSpec)

Specs in `openspec/specs/` say **what** the app does, ADRs say **why**, the code says **how**. A behaviour change goes through an OpenSpec change (proposal, tasks, delta specs) described in [CONTRIBUTING.md](../../CONTRIBUTING.md). Never name specific medicines in a spec.
