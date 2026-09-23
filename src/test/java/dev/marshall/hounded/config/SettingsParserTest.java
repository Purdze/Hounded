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
        values.put(ConfigKeys.HEADSTART_DEFAULT_SECONDS, 45);
        values.put(ConfigKeys.COMPASS_UPDATE_MODE, "manual");
        values.put(ConfigKeys.COMPASS_UPDATE_INTERVAL_TICKS, 10);
        values.put(ConfigKeys.COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS, true);
        values.put(ConfigKeys.RULES_FREEZE_WHEN_LOOKED_AT, true);
        values.put(ConfigKeys.RULES_RUNNER_CAN_ATTACK_HUNTERS, false);
        values.put(ConfigKeys.RULES_FRIENDLY_FIRE, true);
        values.put(ConfigKeys.DISPLAY_MODE, "Scoreboard");
        values.put(ConfigKeys.DISPLAY_SHOW_DISTANCE, false);
        values.put(ConfigKeys.QUICK_START_GUIDE, false);
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
        assertEquals(10, result.warnings().size());
    }

    @Test
    void invalidValuesFallBackToDefaultsWithAWarningNamingTheKey() {
        Map<String, Object> values = validValues();
        values.put(ConfigKeys.COMPASS_UPDATE_INTERVAL_TICKS, 0);
        values.put(ConfigKeys.HEADSTART_DEFAULT_SECONDS, "soon");
        values.put(ConfigKeys.DISPLAY_MODE, "hologram");
        values.put(ConfigKeys.RULES_FRIENDLY_FIRE, "yes please");

        SettingsParser.Result result = parser.parse(values);

        assertEquals(
                Settings.DEFAULTS.compassUpdateIntervalTicks(),
                result.settings().compassUpdateIntervalTicks());
        assertEquals(
                Settings.DEFAULTS.defaultHeadstartSeconds(), result.settings().defaultHeadstartSeconds());
        assertEquals(Settings.DEFAULTS.displayMode(), result.settings().displayMode());
        assertEquals(Settings.DEFAULTS.friendlyFire(), result.settings().friendlyFire());
        assertEquals(4, result.warnings().size());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains(ConfigKeys.DISPLAY_MODE)));
    }

    @Test
    void zeroHeadstartIsAllowed() {
        Map<String, Object> values = validValues();
        values.put(ConfigKeys.HEADSTART_DEFAULT_SECONDS, 0);
        assertEquals(0, parser.parse(values).settings().defaultHeadstartSeconds());
    }
}
