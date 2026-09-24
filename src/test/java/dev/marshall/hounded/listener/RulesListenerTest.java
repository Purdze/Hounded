package dev.marshall.hounded.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.LivingEntitySimulation;

/** The attack rules through a real round and real damage events. */
class RulesListenerTest {
    private PluginFixture fixture;
    private PlayerMock hunter;
    private PlayerMock otherHunter;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        hunter = fixture.addAdmin("Hunter");
        otherHunter = fixture.server().addPlayer("OtherHunter");
        runner = fixture.server().addPlayer("Runner");
        hunter.performCommand("hounded hunter add OtherHunter");
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private void startRound() {
        fixture.startRound(hunter, 0, runner);
    }

    private static boolean isBlocked(PlayerMock victim, Entity damager) {
        return new LivingEntitySimulation(victim).simulateDamage(1, damager).isCancelled();
    }

    private Arrow arrowShotBy(PlayerMock shooter) {
        Arrow arrow = shooter.getWorld().spawn(shooter.getLocation(), Arrow.class);
        arrow.setShooter(shooter);
        return arrow;
    }

    @Test
    void byDefaultRunnersAndHuntersCanFightButNotTheirOwnSide() {
        startRound();

        assertFalse(isBlocked(hunter, runner));
        assertFalse(isBlocked(runner, hunter));
        assertTrue(isBlocked(otherHunter, hunter));
        assertTrue(isBlocked(otherHunter, arrowShotBy(hunter)));
    }

    @Test
    void runnersCanBeStoppedFromAttackingHunters() throws IOException {
        startRound();
        fixture.reloadWith(hunter, ConfigKey.RULES_RUNNER_CAN_ATTACK_HUNTERS, false);

        assertTrue(isBlocked(hunter, runner));
        assertTrue(isBlocked(hunter, arrowShotBy(runner)));
        assertFalse(isBlocked(runner, hunter));
    }

    @Test
    void friendlyFireCanBeAllowed() throws IOException {
        startRound();
        fixture.reloadWith(hunter, ConfigKey.RULES_FRIENDLY_FIRE, true);

        assertFalse(isBlocked(otherHunter, hunter));
    }

    @Test
    void rulesDoNotApplyOutsideARound() {
        assertFalse(isBlocked(otherHunter, hunter));
    }
}
