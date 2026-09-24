package dev.marshall.hounded.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
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

    private ThrownPotion splashPotionThrownBy(PlayerMock thrower, PotionType type) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(type);
        item.setItemMeta(meta);
        ThrownPotion potion = thrower.getWorld().spawn(thrower.getLocation(), ThrownPotion.class);
        potion.setItem(item);
        potion.setShooter(thrower);
        return potion;
    }

    /** How strongly a splash from {@code thrower} reaches {@code victim}, after the rules. */
    private double splashIntensity(PlayerMock thrower, PotionType type, PlayerMock victim) {
        PotionSplashEvent event = new PotionSplashEvent(
                splashPotionThrownBy(thrower, type), victim, null, null, new HashMap<>(Map.of(victim, 1.0)));
        fixture.server().getPluginManager().callEvent(event);
        return event.getIntensity(victim);
    }

    /** Whether a lingering cloud from {@code thrower} still affects {@code victim}, after the rules. */
    private boolean cloudReaches(PlayerMock thrower, PotionType type, PlayerMock victim) {
        AreaEffectCloud cloud = thrower.getWorld().spawn(thrower.getLocation(), AreaEffectCloud.class);
        cloud.setBasePotionType(type);
        cloud.setSource(thrower);
        List<LivingEntity> affected = new ArrayList<>(List.of(victim));
        fixture.server().getPluginManager().callEvent(new AreaEffectCloudApplyEvent(cloud, affected));
        return affected.contains(victim);
    }

    @Test
    void harmfulPotionsFollowTheAttackRulesButHelpfulOnesAlwaysWork() {
        startRound();

        assertEquals(0, splashIntensity(hunter, PotionType.POISON, otherHunter));
        assertEquals(1, splashIntensity(hunter, PotionType.HEALING, otherHunter));
        assertEquals(1, splashIntensity(hunter, PotionType.POISON, runner));
        assertFalse(cloudReaches(hunter, PotionType.POISON, otherHunter));
        assertTrue(cloudReaches(hunter, PotionType.REGENERATION, otherHunter));
        assertTrue(cloudReaches(hunter, PotionType.POISON, runner));
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
