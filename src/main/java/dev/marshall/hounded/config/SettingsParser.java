package dev.marshall.hounded.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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
                new Settings.Headstart(
                        reader.intAtLeast(
                                ConfigKey.HEADSTART_DEFAULT_SECONDS,
                                0,
                                defaults.headstart().defaultSeconds()),
                        reader.bool(
                                ConfigKey.HEADSTART_FREEZE_HUNTERS,
                                defaults.headstart().freezeHunters()),
                        reader.bool(
                                ConfigKey.HEADSTART_BLIND_HUNTERS,
                                defaults.headstart().blindHunters())),
                new Settings.Compass(
                        reader.enumValue(
                                ConfigKey.COMPASS_UPDATE_MODE,
                                defaults.compass().updateMode()),
                        reader.intAtLeast(
                                ConfigKey.COMPASS_UPDATE_INTERVAL_TICKS,
                                1,
                                defaults.compass().updateIntervalTicks()),
                        reader.bool(
                                ConfigKey.COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS,
                                defaults.compass().disableInNetherForHunters())),
                new Settings.Rules(
                        reader.bool(
                                ConfigKey.RULES_FREEZE_WHEN_LOOKED_AT,
                                defaults.rules().freezeWhenLookedAt()),
                        reader.bool(
                                ConfigKey.RULES_RUNNER_CAN_ATTACK_HUNTERS,
                                defaults.rules().runnerCanAttackHunters()),
                        reader.bool(
                                ConfigKey.RULES_FRIENDLY_FIRE, defaults.rules().friendlyFire()),
                        reader.bool(
                                ConfigKey.RULES_ELIMINATED_RUNNERS_SPECTATE,
                                defaults.rules().eliminatedRunnersSpectate()),
                        reader.intAtLeast(
                                ConfigKey.RULES_RUNNER_REJOIN_GRACE_SECONDS,
                                0,
                                defaults.rules().runnerRejoinGraceSeconds())),
                new Settings.Display(
                        reader.enumValue(
                                ConfigKey.DISPLAY_MODE, defaults.display().mode()),
                        reader.bool(
                                ConfigKey.DISPLAY_SHOW_DISTANCE,
                                defaults.display().showDistance())),
                reader.bool(ConfigKey.QUICK_START_GUIDE, defaults.showQuickStartGuide()));
        return new Result(settings, reader.warnings);
    }

    private static final class Reader {
        private final Map<String, ?> values;
        private final List<String> warnings = new ArrayList<>();

        Reader(Map<String, ?> values) {
            this.values = values;
        }

        int intAtLeast(ConfigKey key, int minimum, int fallback) {
            return read(
                    key,
                    fallback,
                    "a whole number >= " + minimum,
                    raw -> raw instanceof Integer number && number >= minimum ? Optional.of(number) : Optional.empty());
        }

        boolean bool(ConfigKey key, boolean fallback) {
            return read(
                    key,
                    fallback,
                    "true or false",
                    raw -> raw instanceof Boolean flag ? Optional.of(flag) : Optional.empty());
        }

        <E extends Enum<E>> E enumValue(ConfigKey key, E fallback) {
            E[] constants = fallback.getDeclaringClass().getEnumConstants();
            String allowed = String.join(
                    ", ",
                    Arrays.stream(constants)
                            .map(constant -> constant.name().toLowerCase(Locale.ROOT))
                            .toList());
            return read(key, fallback, "one of: " + allowed, raw -> {
                String name = raw.toString().trim().toUpperCase(Locale.ROOT);
                return Arrays.stream(constants)
                        .filter(constant -> constant.name().equals(name))
                        .findFirst();
            });
        }

        private <T> T read(ConfigKey key, T fallback, String expected, Function<Object, Optional<T>> convert) {
            Object raw = values.get(key.path());
            if (raw == null) {
                warnings.add("'%s' is missing, using default %s".formatted(key.path(), fallback));
                return fallback;
            }
            Optional<T> converted = convert.apply(raw);
            if (converted.isEmpty()) {
                warnings.add(
                        "'%s' is '%s' but must be %s, using default %s".formatted(key.path(), raw, expected, fallback));
            }
            return converted.orElse(fallback);
        }
    }
}
