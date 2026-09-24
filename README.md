# Hounded - Manhunt

A manhunt game mode for Paper: one or more speedrunners try to beat the game while hunters track them with a compass that works in every dimension.

> **Version 1.0.0.** Full rounds, a tracking compass that works in every dimension, a timer and distance display, PlaceholderAPI placeholders and an events API for other plugins.

## Requirements
- Paper **26.2**
- Java **25**

## Install
1. Download `hounded-<version>.jar` (or build it, see below) and put it in your server's `plugins/` folder.
2. Start the server. Hounded creates `plugins/Hounded/config.yml` and `messages.yml`.
3. Optional: install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to use Hounded's placeholders in other plugins (scoreboards, tab lists, chat).

## Commands
All commands except `help` and `compass` need `hounded.admin`.

| Command | Description |
|---|---|
| `/hounded runner add\|remove <player>` | Make an online player a runner, or take the role away |
| `/hounded runner list\|clear` | List all runners, or remove them all |
| `/hounded hunter add\|remove <player>` | Same for hunters |
| `/hounded hunter list\|clear` | Same for hunters |
| `/hounded start [seconds]` | Start a round with an optional headstart (0 = none) |
| `/hounded stop` | Stop the current round |
| `/hounded compass` | Get your tracking compass back (hunters in a round, `hounded.compass`) |
| `/hounded reload` | Reload `config.yml` and `messages.yml` |
| `/hounded help` | Show help (also plain `/hounded`) |

## First run
Until the first round is started, admins get a short "how to start" guide in chat when they join. After that it's gone for good: the server remembers it in `plugins/Hounded/data.yml`. To see the guide again, delete that file. To turn it off, set `quick-start-guide: false`.

## How a round works
1. Add at least one runner and one hunter.
2. `/hounded start 30` gives runners 30 seconds, then the hunters are released. During the headstart, hunters are frozen: they can look around but can't move, mine, build, attack, use items or be hurt. They're blind too (both configurable).
3. Runners win when the ender dragon dies. Hunters win when every runner is out.
   - A runner who dies is out, and watches in spectator mode until the round ends (configurable).
   - A runner who leaves the server has 5 minutes (configurable) to come back, or they're out too.
4. The result is announced with the hunt time, and the game returns to the lobby. Spectating runners get their previous game mode back, and roles are kept for the next round.

Roles can't be changed during a round. Use `/hounded stop` first.

## The tracking compass
Every hunter gets a tracking compass when the round starts, and again after dying or rejoining. It's collected when the round ends, and it can't be dropped.
- **Where it points:** at the runner when you're in the same dimension. Otherwise it points at the portal they left your dimension through. In the Nether and the End it points properly, where a normal compass would spin. If there's no trail in your dimension yet, it points at spawn and tells you so.
- **Updates:** it updates by itself every `compass.update-interval-ticks`. In `manual` mode you right-click it to update.
- **More than one runner:** left-click switches to the next one.
- **Offline runners:** the compass keeps pointing where they were last seen.
- **During the headstart:** it doesn't point anywhere, so it can't give away where runners went.

## Display
During a round, everyone in the round sees a boss bar (or a scoreboard sidebar, see `display.mode`):
- **During the headstart:** a countdown until the hunters are released.
- **While hunting:** the hunt timer.
- **Hunters also see** how far away their runner is. If the runner is in another dimension, it shows which one and how far away their portal is.

Distances are horizontal blocks, like the X/Z on F3. The display is hidden outside a round. Changing `display.mode` with `/hounded reload` switches it immediately.

## Permissions
| Node | Default | Grants |
|---|---|---|
| `hounded.admin` | op | All admin commands |
| `hounded.compass` | everyone | `/hounded compass` (getting a replacement compass). Hunters get and use their compass without it. |

## Configuration (`config.yml`)
| Key | Default | Meaning |
|---|---|---|
| `headstart.default-seconds` | `30` | Headstart when `/hounded start` has no number. `0` = none. |
| `headstart.freeze-hunters` | `true` | Hunters can't move, mine, build, attack, use or drop items, or be hurt until released. |
| `headstart.blind-hunters` | `true` | Hunters are blind until released. |
| `compass.update-mode` | `auto` | `auto` updates on a timer; `manual` updates on right-click. |
| `compass.update-interval-ticks` | `20` | Auto-update interval (20 ticks = 1 s). Minimum 1. |
| `compass.disable-in-nether-for-hunters` | `false` | Turn off tracking while a hunter is in the Nether. |
| `rules.freeze-when-looked-at` | `false` | While hunting, a hunter can't walk while a runner is looking at them (within about 15° of the crosshair, clear view, up to 64 blocks). They can still look around, attack and use items. Nobody is hurt by it. |
| `rules.runner-can-attack-hunters` | `true` | Runners may damage hunters, including with arrows and other projectiles. |
| `rules.friendly-fire` | `false` | Players on the same side may damage each other, including with projectiles. |
| `rules.eliminated-runners-spectate` | `true` | Runners who are out watch in spectator mode until the round ends. |
| `rules.runner-rejoin-grace-seconds` | `300` | Seconds a runner who leaves has to come back before they're out. `0` = out at once. |
| `display.mode` | `bossbar` | `bossbar`, `scoreboard` or `none`. Switches live on `/hounded reload`. |
| `display.show-distance` | `true` | Show hunters the distance to their target. |
| `quick-start-guide` | `true` | Show admins a short start guide in chat until the first round is started. |

Invalid values fall back to the default. The console says which key was wrong.

## Messages (`messages.yml`)
Every player-facing text is in `messages.yml`, in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format, so you can recolour or translate it. Keys missing from your file fall back to the bundled English.

## Placeholders
With PlaceholderAPI installed, these work anywhere PlaceholderAPI does. Values are plain text, and empty when they don't apply to the player.

| Placeholder | Shows |
|---|---|
| `%hounded_role%` | The player's role (runner or hunter, from `messages.yml`) |
| `%hounded_state%` | The round state, e.g. "Hunt in progress" (`state.*` in `messages.yml`) |
| `%hounded_timer%` | Hunt time, e.g. `12:34` |
| `%hounded_headstart%` | Headstart time left |
| `%hounded_distance%` | For hunters: blocks to where their compass points |
| `%hounded_target%` | For hunters: the runner they're tracking |
| `%hounded_runners_left%` | Runners still in the round |

## For developers
Hounded fires Bukkit events, all on the main thread, from the `dev.marshall.hounded.api` package:

| Event | When | Cancellable |
|---|---|---|
| `HoundedRoundStartEvent` | Just before a round starts (teams, headstart) | yes |
| `HoundedRoleChangeEvent` | Just before a player's role changes (`from`/`to`) | yes |
| `HoundedRoundWinEvent` | A side won (`winner()`), with the hunt time and teams | no |
| `HoundedRoundStopEvent` | An admin stopped the round | no |
| `HoundedRoundEndEvent` | Parent of the two above: listen to it to hear about any end | no |

```java
@EventHandler
public void onWin(HoundedRoundWinEvent event) {
    getLogger().info(event.winner() + " won after " + event.huntTime().toMinutes() + " minutes");
}
```

`Role` is `dev.marshall.hounded.game.Role` (`RUNNER` or `HUNTER`). Add `softdepend: [Hounded]` to your `plugin.yml`.

## Building
```
./gradlew build
```
Needs a JDK 25 that Gradle can find (Gradle itself can run on 21+). The jar goes to `build/libs/`. Formatting is enforced: run `./gradlew spotlessApply` if the build complains.

## Local test server
```
./gradlew runServer
```
Builds the plugin and starts a Paper 26.2 server in `test-server/` with it loaded (via [run-paper](https://github.com/jpenilla/run-task)). Type server commands in the same terminal and `stop` to shut down. Starting it accepts the Minecraft EULA (see `build.gradle.kts`). `test-server/` isn't committed.

## Tests
`./gradlew test` runs plain unit tests for the game logic, plus [MockBukkit](https://github.com/MockBukkit/MockBukkit) tests that load the plugin on a simulated server and drive commands, deaths and the headstart timer.

## License
Hounded is licensed under the [GNU General Public License v3.0](LICENSE). You may use, change and share it. If you publish a modified version, you must publish its source under the same license.
