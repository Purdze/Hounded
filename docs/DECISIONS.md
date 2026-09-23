# Decisions

Newest first within each section. Record the date, the choice, and why.

## Versions (2026-09-23)

All looked up on 2026-09-23, not from memory.

| Item | Choice | Source / reason |
|---|---|---|
| Minecraft / Paper target | **26.2** | Newest Paper line with STABLE builds (fill.papermc.io v3 API). 26.3 only has ALPHA builds. |
| Paper API | `io.papermc.paper:paper-api:26.2.build.129-stable` | Latest stable build in `repo.papermc.io` maven-metadata. The docs example uses `26.2.build.+`; we pin an exact build so builds are reproducible. Bump deliberately. |
| Repository | `https://repo.papermc.io/repository/maven-public/` | docs.papermc.io/paper/dev/project-setup |
| Java | **25** (Gradle toolchain) | docs.papermc.io/paper/getting-started: "26.1+ → Java 25". Project-setup page uses `JavaLanguageVersion.of(25)`. |
| Gradle | **9.7.1** (wrapper) | docs.papermc.io does not name a Gradle version, so we use the current release from services.gradle.org. Gradle itself may run on JDK 21+; the toolchain supplies JDK 25 for compiling and testing. |
| `api-version` | `'26.2'` | Minimum version we support, not a target. docs.papermc.io/paper/dev/plugin-yml lists valid values 1.13–26.2. We compile against 26.2, so we don't claim anything older. |
| JUnit | 5.14.4 (BOM) | Latest 5.x on Maven Central. CLAUDE.md names JUnit 5. |
| Formatter | Spotless 8.10.2 + palantir-java-format 2.99.0 | Approved by owner 2026-09-23. `./gradlew build` runs `spotlessCheck` and fails on violations; run `./gradlew spotlessApply` to fix. |

Compiler runs with `-Xlint:all -Werror` so warnings don't pile up.

## Descriptor and commands

- **`plugin.yml`, not `paper-plugin.yml`.** Required by CLAUDE.md so a Spigot listing stays possible later.
- **Commands via Brigadier** (`getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, …)` in `onEnable`). The Paper docs show this from the main plugin class. Their bootstrap-based variant needs `paper-plugin.yml`, which we don't use. Therefore `plugin.yml` has no `commands:` block. **Verified 2026-09-23** on Paper 26.2 build 129: registering from `onEnable` works with a `plugin.yml` plugin.
- **No Folia.** `folia-supported` is not declared.

## Architecture

- **Roles live in `game/` (`Roster`), not `role/`.** `GameSession` needs roles for win conditions, and `game/` must not depend on other packages. `role/` is reserved for team handling that builds on `game/`.
- **`GameSession` returns `TransitionResult`** (`Changed` / `Unchanged` / `Rejected(reason)`) instead of throwing. Commands map each `RejectionReason` to a message key.
- **Time is injected** (`java.time.Clock`) so headstart and the hunt timer are testable. The plugin calls `tick()` periodically.
- **Roles are locked during a round.** Changing roles mid-round would silently change who can win. Revisit if users want late-joining hunters.
- **`round/` package (not in the original CLAUDE.md layout).** `RoundService` is the one Bukkit-side place that turns `GameSession` results into effects: broadcasts, the headstart timer, and the reset to the lobby after a round ends. Commands and listeners call it, so neither contains game flow.
- **Removing a role is role-specific.** `/hounded runner remove X` fails with a message if X is a hunter, instead of silently unassigning them.
- **Role names are message keys** (`role.name.runner`, `role.name.runners`, …) so translations don't depend on English pluralisation.
- **Unknown player / missing permission** are handled by Brigadier (vanilla error, and admin branches hidden), so there are no message keys for them.
- **Existing `messages.yml` files are never overwritten.** A key only falls back to the bundled text when it's missing entirely, so a changed default doesn't reach an existing file. Revisit before the first release, e.g. with a config version and a migration notice.
- **Runner death = eliminated for the round.** Hunters win when every runner is eliminated. Eliminated runners respawn normally and stay in survival; a spectator switch is still to be decided. A runner quitting does **not** eliminate them yet. That still needs a decision (e.g. a grace period).
- **Dimensions are an enum (OVERWORLD / NETHER / END).** Assumes one world of each per server. Custom/extra worlds are out of scope for v1.
- **Portal memory = where the runner last left each dimension.** `TargetResolver` points a hunter in dimension D at the runner if they share D. Otherwise it points at the runner's last exit portal in D. If neither is known, it returns `NoData`, and the caller points at D's spawn and tells the hunter why.
- **Config parsing is pure** (`SettingsParser` takes a flat map). Invalid values fall back to defaults with a console warning naming the key, so a typo never prevents loading.
- **Reload is all-or-nothing.** `ConfigService.reload()` swaps settings and messages together only if both files load. Otherwise it keeps the previous ones and logs why.
- **Keys are enums** (`ConfigKey`, `MessageKey`), each listing every path once. Tests iterate `values()`, so there is no second list to keep in sync.
- **Messages:** Missing keys in the user's `messages.yml` fall back to the bundled English. A key missing everywhere renders as the key name, logged once. A test checks every key exists and is strict-valid MiniMessage.
- **paper-api is also a `testImplementation`** so tests can load the bundled YAML and render MiniMessage. It's the same allowed dependency, not a new one.

## Compass (open questions)

Planned approach: `CompassMeta` lodestone target with `setLodestoneTracked(false)` (no real lodestone block). **Not verified on 26.2 yet.** The questions are listed on `CompassItem#pointAt`. Record the answers here once tested.
