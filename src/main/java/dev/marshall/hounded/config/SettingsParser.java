package dev.marshall.hounded.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Turns raw config values into {@link Settings}. Pure Java so validation is unit-testable. Bad or
 * missing values fall back to {@link Settings#DEFAULTS} and produce a warning instead of failing,
 * so a typo never stops the plugin from loading.
 */
public final class SettingsParser {

    /** @param warnings console-facing descriptions of every value that was replaced by a default */
    public record Result(Settings settings, List<String> warnings) {
        public Result {
            Objects.requireNonNull(settings, "settings");
            warnings = List.copyOf(warnings);
        }
    }

    /** @param values flattened config, keyed by dotted path (Bukkit's {@code getValues(true)}) */
    public Result parse(Map<String, ?> values) {
        Objects.requireNonNull(values, "values");
        Reader reader = new Reader(values);
        Settings defaults = Settings.DEFAULTS;
        Settings settings = new Settings(
                reader.intAtLeast(ConfigKeys.HEADSTART_DEFAULT_SECONDS, 0, defaults.defaultHeadstartSeconds()),
                reader.enumValue(ConfigKeys.COMPASS_UPDATE_MODE, CompassUpdateMode.class, defaults.compassUpdateMode()),
                reader.intAtLeast(ConfigKeys.COMPASS_UPDATE_INTERVAL_TICKS, 1, defaults.compassUpdateIntervalTicks()),
                reader.bool(
                        ConfigKeys.COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS, defaults.disableCompassInNetherForHunters()),
                reader.bool(ConfigKeys.RULES_FREEZE_WHEN_LOOKED_AT, defaults.freezeWhenLookedAt()),
                reader.bool(ConfigKeys.RULES_RUNNER_CAN_ATTACK_HUNTERS, defaults.runnerCanAttackHunters()),
                reader.bool(ConfigKeys.RULES_FRIENDLY_FIRE, defaults.friendlyFire()),
                reader.enumValue(ConfigKeys.DISPLAY_MODE, DisplayMode.class, defaults.displayMode()),
                reader.bool(ConfigKeys.DISPLAY_SHOW_DISTANCE, defaults.showDistance()),
                reader.bool(ConfigKeys.QUICK_START_GUIDE, defaults.showQuickStartGuide()));
        return new Result(settings, reader.warnings);
    }

    private static final class Reader {
        private final Map<String, ?> values;
        private final List<String> warnings = new ArrayList<>();

        Reader(Map<String, ?> values) {
            this.values = values;
        }

        int intAtLeast(String key, int minimum, int fallback) {
            Optional<Object> raw = raw(key, fallback);
            if (raw.isEmpty()) {
                return fallback;
            }
            if (raw.get() instanceof Integer number && number >= minimum) {
                return number;
            }
            return invalid(key, raw.get(), "a whole number >= " + minimum, fallback);
        }

        boolean bool(String key, boolean fallback) {
            Optional<Object> raw = raw(key, fallback);
            if (raw.isEmpty()) {
                return fallback;
            }
            if (raw.get() instanceof Boolean flag) {
                return flag;
            }
            return invalid(key, raw.get(), "true or false", fallback);
        }

        <E extends Enum<E>> E enumValue(String key, Class<E> type, E fallback) {
            Optional<Object> raw = raw(key, fallback);
            if (raw.isEmpty()) {
                return fallback;
            }
            String name = raw.get().toString().trim().toUpperCase(Locale.ROOT);
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equals(name)) {
                    return constant;
                }
            }
            String allowed = String.join(
                    ", ",
                    Arrays.stream(type.getEnumConstants())
                            .map(constant -> constant.name().toLowerCase(Locale.ROOT))
                            .toList());
            return invalid(key, raw.get(), "one of: " + allowed, fallback);
        }

        private Optional<Object> raw(String key, Object fallback) {
            Object value = values.get(key);
            if (value == null) {
                warnings.add("'%s' is missing, using default %s".formatted(key, fallback));
            }
            return Optional.ofNullable(value);
        }

        private <T> T invalid(String key, Object value, String expected, T fallback) {
            warnings.add("'%s' is '%s' but must be %s, using default %s".formatted(key, value, expected, fallback));
            return fallback;
        }
    }
}
