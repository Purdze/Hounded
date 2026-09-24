package dev.marshall.hounded.rules;

import dev.marshall.hounded.config.Settings;
import dev.marshall.hounded.game.Role;

/** Who may hurt whom during a round, per the {@code rules} section of the config. */
public final class CombatRules {

    private CombatRules() {}

    public static boolean allowsDamage(Role attacker, Role victim, Settings.Rules rules) {
        if (attacker == victim) {
            return rules.friendlyFire();
        }
        if (attacker == Role.RUNNER) {
            return rules.runnerCanAttackHunters();
        }
        return true;
    }
}
