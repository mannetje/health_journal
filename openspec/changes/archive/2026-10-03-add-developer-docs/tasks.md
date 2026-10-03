## 0. Decisions (approved)
- [x] 0.1 English only, plain markdown in `docs/dev/` first
- [x] 0.2 Scope of the first pass: getting started, architecture tour, life of an entry, how-to guides, conventions, testing, releases, `CONTRIBUTING.md`

## 1. Pages
- [x] 1.1 `docs/dev/README.md` index
- [x] 1.2 `getting-started.md`
- [x] 1.3 `architecture.md` (layers, dependency rule, diagram)
- [x] 1.4 `life-of-an-entry.md` (blood pressure from screen to database and CSV)
- [x] 1.5 How-to: add a metric, add a translation, change a range label, change the database
- [x] 1.6 `conventions.md`, `testing.md`, `releases.md`

## 2. Contribution
- [x] 2.1 `CONTRIBUTING.md`
- [x] 2.2 `.github/PULL_REQUEST_TEMPLATE.md`, bug report and feature request issue templates

## 3. Accuracy and cleanup
- [x] 3.1 `scripts/check-docs.sh` and a CI step
- [x] 3.2 Remove the stale `doc/` folder
- [x] 3.3 Link from the README

## 4. Wrap-up
- [x] 4.1 `openspec validate add-developer-docs --strict`
- [x] 4.2 Apply the delta spec on archive
