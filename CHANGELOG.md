# Changelog

## [Unreleased]
### Added
- Gradle (Kotlin DSL) project targeting Paper 26.2 / Java 25, with Spotless formatting enforced.
- `GameSession` state machine (lobby → headstart → running → ended), runner/hunter roles and win conditions.
- `TargetResolver`: decides where a hunter's compass points across dimensions.
- `config.yml` and `messages.yml` (MiniMessage) with validation and all-or-nothing reload.
- Stubs for `TrackingService` and `CompassItem`.
