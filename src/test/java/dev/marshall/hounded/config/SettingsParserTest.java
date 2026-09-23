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
        values.put(ConfigKey.COMPASS_UPDATE_MODE.path(), "manual");
        values.put(ConfigKey.COMPASS_UPDATE_INTERVAL_TICKS.path(), 10);
        values.put(ConfigKey.COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS.path(), true);
        values.put(ConfigKey.RULES_FREEZE_WHEN_LOOKED_AT.path(), true);
        values.put(ConfigKey.RULES_RUNNER_CAN_ATTACK_HUNTERS.path(), false);
        values.put(ConfigKey.RULES_FRIENDLY_FIRE.path(), true);
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
                        45,
                        CompassUpdateMode.MANUAL,
                        10,
                        true,
                        true,
                        false,
                        true,
                        DisplayMode.SCOREBOARD,
                        false,
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
        values.put(ConfigKey.DISPLAY_MODE.path(), "hologram");
        values.put(ConfigKey.RULES_FRIENDLY_FIRE.path(), "yes please");

        SettingsParser.Result result = parser.parse(values);

        assertEquals(
                Settings.DEFAULTS.compassUpdateIntervalTicks(),
                result.settings().compassUpdateIntervalTicks());
        assertEquals(
                Settings.DEFAULTS.defaultHeadstartSeconds(), result.settings().defaultHeadstartSeconds());
        assertEquals(Settings.DEFAULTS.displayMode(), result.settings().displayMode());
        assertEquals(Settings.DEFAULTS.friendlyFire(), result.settings().friendlyFire());
        assertEquals(4, result.warnings().size());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains(ConfigKey.DISPLAY_MODE.path())));
    }

    @Test
    void zeroHeadstartIsAllowed() {
        Map<String, Object> values = validValues();
        values.put(ConfigKey.HEADSTART_DEFAULT_SECONDS.path(), 0);
        assertEquals(0, parser.parse(values).settings().defaultHeadstartSeconds());
    }
}
