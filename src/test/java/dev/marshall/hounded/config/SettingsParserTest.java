package dev.marshall.hounded.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsParserTest {
    private final SettingsParser parser = new SettingsParser();

    private static Map<String, Object> validValues() {
        Map<String, Object> values = new HashMap<>();
        values.put(ConfigKey.HEADSTART_DEFAULT_SECONDS.path(), 45);
        values.put(ConfigKey.HEADSTART_FREEZE_HUNTERS.path(), false);
        values.put(ConfigKey.HEADSTART_BLIND_HUNTERS.path(), false);
        values.put(ConfigKey.COMPASS_UPDATE_MODE.path(), "manual");
        values.put(ConfigKey.COMPASS_UPDATE_INTERVAL_TICKS.path(), 10);
        values.put(ConfigKey.COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS.path(), true);
        values.put(ConfigKey.RULES_FREEZE_WHEN_LOOKED_AT.path(), true);
        values.put(ConfigKey.RULES_RUNNER_CAN_ATTACK_HUNTERS.path(), false);
        values.put(ConfigKey.RULES_FRIENDLY_FIRE.path(), true);
        values.put(ConfigKey.RULES_ELIMINATED_RUNNERS_SPECTATE.path(), false);
        values.put(ConfigKey.RULES_RUNNER_REJOIN_GRACE_SECONDS.path(), 60);
        values.put(ConfigKey.DISPLAY_MODE.path(), "Scoreboard");
        values.put(ConfigKey.DISPLAY_SHOW_DISTANCE.path(), false);
        values.put(ConfigKey.QUICK_START_GUIDE.path(), false);
        return values;
    }

    @Test
    void parsesValidValuesWithoutWarnings() {
        SettingsParser.Result result = parser.parse(validValues());

        assertEquals(
                new Settings(
                        new Settings.Headstart(45, false, false),
                        new Settings.Compass(CompassUpdateMode.MANUAL, 10, true),
                        new Settings.Rules(true, false, true, false, 60),
                        new Settings.Display(DisplayMode.SCOREBOARD, false),
                        false),
                result.settings());
        assertTrue(result.warnings().isEmpty(), () -> result.warnings().toString());
    }

    @Test
    void emptyConfigFallsBackToDefaultsAndWarnsPerKey() {
        SettingsParser.Result result = parser.parse(Map.of());

        assertEquals(Settings.DEFAULTS, result.settings());
        assertEquals(ConfigKey.values().length, result.warnings().size());
    }

    @Test
    void invalidValuesFallBackToDefaultsWithAWarningNamingTheKey() {
        Map<String, Object> values = validValues();
        values.put(ConfigKey.COMPASS_UPDATE_INTERVAL_TICKS.path(), 0);
        values.put(ConfigKey.HEADSTART_DEFAULT_SECONDS.path(), "soon");
        values.put(ConfigKey.HEADSTART_BLIND_HUNTERS.path(), "maybe");
        values.put(ConfigKey.DISPLAY_MODE.path(), "hologram");
        values.put(ConfigKey.RULES_FRIENDLY_FIRE.path(), "yes please");
        values.put(ConfigKey.RULES_RUNNER_REJOIN_GRACE_SECONDS.path(), -1);

        SettingsParser.Result result = parser.parse(values);
        Settings settings = result.settings();
        Settings defaults = Settings.DEFAULTS;

        assertEquals(
                defaults.compass().updateIntervalTicks(), settings.compass().updateIntervalTicks());
        assertEquals(defaults.headstart().defaultSeconds(), settings.headstart().defaultSeconds());
        assertEquals(defaults.headstart().blindHunters(), settings.headstart().blindHunters());
        assertEquals(defaults.display().mode(), settings.display().mode());
        assertEquals(defaults.rules().friendlyFire(), settings.rules().friendlyFire());
        assertEquals(
                defaults.rules().runnerRejoinGraceSeconds(), settings.rules().runnerRejoinGraceSeconds());
        assertEquals(6, result.warnings().size());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains(ConfigKey.DISPLAY_MODE.path())));
    }

    @Test
    void zeroHeadstartIsAllowed() {
        Map<String, Object> values = validValues();
        values.put(ConfigKey.HEADSTART_DEFAULT_SECONDS.path(), 0);
        assertEquals(0, parser.parse(values).settings().headstart().defaultSeconds());
    }
}
