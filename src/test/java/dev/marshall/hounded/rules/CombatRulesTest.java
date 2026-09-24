package dev.marshall.hounded.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marshall.hounded.config.Settings;
import dev.marshall.hounded.game.Role;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CombatRulesTest {

    private static Settings.Rules rules(boolean runnerCanAttackHunters, boolean friendlyFire) {
        return new Settings.Rules(false, runnerCanAttackHunters, friendlyFire, true, 300);
    }

    @ParameterizedTest(name = "{0} hits {1}, runners may attack: {2}, friendly fire: {3} -> allowed: {4}")
    @CsvSource({
        "RUNNER, HUNTER, true,  false, true",
        "RUNNER, HUNTER, false, false, false",
        "RUNNER, HUNTER, false, true,  false",
        "HUNTER, RUNNER, false, false, true",
        "HUNTER, RUNNER, true,  false, true",
        "HUNTER, HUNTER, true,  false, false",
        "HUNTER, HUNTER, true,  true,  true",
        "RUNNER, RUNNER, true,  false, false",
        "RUNNER, RUNNER, false, true,  true",
    })
    void decidesWhoMayHurtWhom(
            Role attacker, Role victim, boolean runnerCanAttackHunters, boolean friendlyFire, boolean allowed) {
        assertEquals(allowed, CombatRules.allowsDamage(attacker, victim, rules(runnerCanAttackHunters, friendlyFire)));
    }
}
