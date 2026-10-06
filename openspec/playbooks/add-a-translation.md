# Playbook: add or change a translation

Detail: [How to add or change a translation](../../docs/dev/how-to/add-a-translation.md).

## Steps
1. Add the key to `res/values/strings.xml` and to every `res/values-<lang>/strings.xml`.
2. Escape `<` as `&lt;` and keep placeholders identical across languages.
3. Never build user text in a ViewModel from a literal; use `UiText`.

## Done when
- `./gradlew test` passes (placeholder and label string tests).
- The screen was checked in each language.
