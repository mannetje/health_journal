# Playbook: release

Detail: [Releases](../../docs/dev/releases.md).

## Steps
1. Move `Unreleased` in `CHANGELOG.md` to `## [X.Y.Z] - date`.
2. Bump `versionCode` and `versionName`; update README download links.
3. Run `./gradlew test`; try the app on a device.
4. Commit `chore: release X.Y.Z`, push, tag `vX.Y.Z`, push the tag.
5. Archive finished OpenSpec changes and rewrite any TBD Purpose line.

## Done when
- The GitHub Release exists with the debug APK and the changelog notes.
- CI is green.
