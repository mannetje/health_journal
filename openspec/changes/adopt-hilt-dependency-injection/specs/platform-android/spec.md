## ADDED Requirements

### Requirement: Dependencies are provided by Hilt
The `:app` module SHALL obtain its dependencies from Hilt, with ViewModels created through `@HiltViewModel` and no hand-written ViewModel factory. The `:domain` and `:data` modules SHALL NOT depend on Hilt, Dagger or `javax.inject`.

#### Scenario: A dependency is missing
- **WHEN** a ViewModel requires a type that no module provides
- **THEN** the build SHALL fail, and the app SHALL NOT fail at run time

#### Scenario: The domain stays plain
- **WHEN** a class in `:domain` or `:data` is added or changed
- **THEN** it SHALL NOT import Hilt, Dagger or `javax.inject` types
