# Self-hosted CI mirror (Forgejo and Woodpecker)

GitHub stays the source of truth and keeps its Actions. An optional Forgejo instance pulls a mirror of the repository, and Woodpecker runs the same checks from `.woodpecker/`. Nothing here replaces GitHub; it is a second runner. The compose file and host details of a particular setup are deliberately not part of this repository.

| GitHub workflow | Woodpecker file | Notes |
|-----------------|-----------------|-------|
| `android.yml` (docs, tests, coverage, debug APK) | `.woodpecker/ci.yaml` | One workflow, steps in order, so one Gradle daemon is enough |
| `lint.yml` | `.woodpecker/ci.yaml` (`lint` step) | No lint report upload; read the step log |
| `specs.yml` | `.woodpecker/specs.yaml` | Only when `openspec/**` changes |
| release job of `android.yml` | `.woodpecker/release.yaml` | On a `v*` tag; attaches the debug APK and its `.sha256` to the Forgejo release |
| `codeql.yml` | none | CodeQL is GitHub only |

`scripts/job-summary.sh` is not used: it writes to `GITHUB_STEP_SUMMARY`.

## Setup outline

1. Run Forgejo, the Woodpecker server and one Woodpecker agent (Docker backend, one workflow at a time).
2. Create a Forgejo OAuth2 application for Woodpecker, with `<woodpecker url>/authorize` as redirect URI.
3. Build the CI image on the agent host: `docker build -t health-journal-ci:1 -f .woodpecker/Dockerfile .woodpecker`. The pipelines use it with `pull: false`. Bump the tag when the Dockerfile changes.
4. In Forgejo create a pull mirror of the GitHub repository. In Woodpecker log in with Forgejo and activate the repository.
5. For `release.yaml`: create a Forgejo access token with scopes `write:repository` and `read:misc` and add it as the Woodpecker secret `forgejo_release_token`.

## Things to know

- **Mirror syncs may not trigger builds.** A pull mirror fetches in the background. Whether that fires the push webhook to Woodpecker is not guaranteed; test it with one commit. If it does not fire, use a push mirror or push to both remotes.
- **Releases on a mirror.** Forgejo may refuse a release on a pull-mirror repository. If so, delete `release.yaml`; tags are still built and tested.
- **Memory.** `gradle.properties` caps the Gradle daemon at 2 GB; plan the host accordingly.
- **Docker socket.** The agent needs it. Only let trusted users start pipelines and do not run pipelines for pull requests from forks.
- **SDK versions.** The image installs platform 36 and build-tools 36.0.0; change the Dockerfile and `compileSdk` together. The command-line tools download id in the Dockerfile ages; take the current one from the Android Studio download page.
