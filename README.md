# Hounded – Manhunt

A manhunt game mode for Paper: one or more speedrunners try to beat the game while hunters track them with a compass that works in every dimension.

> **Status: early development (0.1.0-SNAPSHOT).** You can assign roles and play a full round: headstart, then a win when the dragon dies or every runner dies. The tracking compass is **not implemented yet**.

## Requirements
- Paper **26.2**
- Java **25**

## Install
1. Download `hounded-<version>.jar` (or build it, see below) and put it in your server's `plugins/` folder.
2. Start the server. Hounded creates `plugins/Hounded/config.yml` and `messages.yml`.
3. Optional: install PlaceholderAPI for placeholders (planned).

## Commands
All commands except `help` need `hounded.admin`. `add` and `remove` take an online player. `/hounded compass` arrives with the compass.

| Command | Description |
|---|---|
| `/hounded runner add\|remove\|list\|clear [player]` | Manage runners |
| `/hounded hunter add\|remove\|list\|clear [player]` | Manage hunters |
| `/hounded start [seconds]` | Start a round with an optional headstart (0 = none) |
| `/hounded stop` | Stop the current round |
| `/hounded reload` | Reload `config.yml` and `messages.yml` |
| `/hounded help` | Show help (also plain `/hounded`) |

## How a round works
1. Add at least one runner and one hunter.
2. `/hounded start 30` gives runners 30 seconds, then the hunters are released.
3. Runners win when the ender dragon dies. Hunters win when every runner has died once.
4. The result is announced with the hunt time, and the game returns to the lobby. Roles are kept for the next round.

Roles can't be changed during a round. Use `/hounded stop` first.

## Permissions
| Node | Default | Grants |
|---|---|---|
| `hounded.admin` | op | All admin commands |
| `hounded.compass` | everyone | Using the tracking compass |

## Configuration (`config.yml`)
| Key | Default | Meaning |
|---|---|---|
| `headstart.default-seconds` | `30` | Headstart when `/hounded start` has no number. `0` = none. |
| `compass.update-mode` | `auto` | `auto` updates on a timer; `manual` updates on right-click. |
| `compass.update-interval-ticks` | `20` | Auto-update interval (20 ticks = 1 s). Minimum 1. |
| `compass.disable-in-nether-for-hunters` | `false` | Turn off tracking while a hunter is in the Nether. |
| `rules.freeze-when-looked-at` | `false` | Hunters freeze while a runner looks at them. Nobody dies from it. |
| `rules.runner-can-attack-hunters` | `true` | Runners may damage hunters. |
| `rules.friendly-fire` | `false` | Players on the same side may damage each other. |
| `display.mode` | `bossbar` | `bossbar`, `scoreboard` or `none`. |
| `display.show-distance` | `true` | Show hunters the distance to their target. |
| `quick-start-guide` | `true` | Show admins a short start guide in chat. |

Invalid values fall back to the default. The console says which key was wrong.

## Messages (`messages.yml`)
Every player-facing text is in `messages.yml`, in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format, so you can recolour or translate it. Keys missing from your file fall back to the bundled English.

## Building
```
./gradlew build
```
Needs a JDK 25 that Gradle can find (Gradle itself can run on 21+). The jar goes to `build/libs/`. Formatting is enforced: run `./gradlew spotlessApply` if the build complains.

## Local test server
`test-server/` is a local Paper 26.2 server for development and isn't committed. To set one up, download the Paper 26.2 jar from papermc.io as `test-server/paper.jar`, accept the EULA in `test-server/eula.txt`, and add a start script that runs it with Java 25.
```
./gradlew deployToTestServer   # builds and copies the plugin to test-server/plugins/Hounded.jar
test-server\start.bat          # your local start script
```

## License
TBD.
