# Changelog

## [Unreleased]
### Added
- `/hounded` commands: help, runner/hunter add, remove, list, clear, start [seconds], stop, reload.
- Headstart countdown that releases the hunters, win detection (dragon kill or all runners dead), result broadcast with hunt time, automatic return to the lobby.
- Gradle (Kotlin DSL) project targeting Paper 26.2 / Java 25, with Spotless formatting enforced.
- `GameSession` state machine (lobby → headstart → running → ended), runner/hunter roles and win conditions.
- `TargetResolver`: decides where a hunter's compass points across dimensions.
- `config.yml` and `messages.yml` (MiniMessage) with validation and all-or-nothing reload.
- Stubs for `TrackingService` and `CompassItem`.
