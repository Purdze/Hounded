# Hounded - Manhunt

[![Build](https://github.com/Purdze/Hounded/actions/workflows/build.yml/badge.svg)](https://github.com/Purdze/Hounded/actions/workflows/build.yml)

A manhunt game mode for Paper: one or more speedrunners try to beat the game while hunters track them with a compass that works in every dimension. Includes a timer and distance display, PlaceholderAPI placeholders and an events API for other plugins.

**[Read the wiki](https://github.com/Purdze/Hounded/wiki)** for setup, commands, configuration, placeholders, translations and the developer API.

## Quick start
Needs Paper 1.21.4 to 26.2 and Java 21 or newer. Put the jar from the [releases](https://github.com/Purdze/Hounded/releases) in `plugins/`, restart, then:
```
/hounded runner add <player>
/hounded hunter add <player>
/hounded start 30
```
That starts a round with a 30 second headstart. See the [Server Setup Guide](https://github.com/Purdze/Hounded/wiki/Server-Setup-Guide) for more.

Hounded uses [bStats](https://bstats.org/docs/server-owners) for anonymous usage stats, such as server counts and versions. Turn it off for all plugins in `plugins/bStats/config.yml`.

## Development

### Building
```
./gradlew build
```
Needs a JDK 25 that Gradle can find (Gradle itself can run on 21+). The plugin is compiled against the Paper 1.21.4 API for Java 21, and tested against Paper 26.2. The jar goes to `build/libs/`. Formatting is enforced: run `./gradlew spotlessApply` if the build complains.

### Local test server
```
./gradlew runServer
```
Builds the plugin and starts a Paper 26.2 server in `test-server/` with it loaded (via [run-paper](https://github.com/jpenilla/run-task)). `./gradlew runServerOldest` does the same with Paper 1.21.4 in `test-server-1.21.4/`, to check the oldest supported version; it needs a JDK 21. Type server commands in the same terminal and `stop` to shut down. Starting either server accepts the Minecraft EULA (see `build.gradle.kts`). Neither server folder is committed.

### Tests
`./gradlew test` runs plain unit tests for the game logic, plus [MockBukkit](https://github.com/MockBukkit/MockBukkit) tests that load the plugin on a simulated server and drive commands, deaths and the headstart timer.

## Bugs and translations
Report bugs and share translated `messages.yml` files in the [issues](https://github.com/Purdze/Hounded/issues).

## License
Hounded is licensed under the [GNU General Public License v3.0](LICENSE). You may use, change and share it. If you publish a modified version, you must publish its source under the same license.
