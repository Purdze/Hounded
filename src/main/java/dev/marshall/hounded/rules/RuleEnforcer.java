package dev.marshall.hounded.rules;

import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.Role;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

/**
 * Applies the {@code rules} section during a round. Everything is decided on demand from the
 * current state, so there is nothing to clean up per player.
 */
public final class RuleEnforcer {
    static final double GAZE_MAX_ANGLE_DEGREES = 15;
    static final double GAZE_MAX_DISTANCE = 64;

    private final GameSession session;
    private final ConfigService configService;
    private final Server server;
    private final BiPredicate<Player, Player> lineOfSight;

    /** @param lineOfSight whether the first player can see the second past blocks */
    public RuleEnforcer(
            GameSession session, ConfigService configService, Server server, BiPredicate<Player, Player> lineOfSight) {
        this.session = Objects.requireNonNull(session, "session");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.server = Objects.requireNonNull(server, "server");
        this.lineOfSight = Objects.requireNonNull(lineOfSight, "lineOfSight");
    }

    /** Whether the rules forbid this hit, including shots from a player's projectile. */
    public boolean blocksDamage(Entity damager, Entity victim) {
        if (!(victim instanceof Player hit)) {
            return false;
        }
        Optional<Role> attackerRole =
                attackingPlayer(damager).flatMap(attacker -> session.roleInRound(attacker.getUniqueId()));
        Optional<Role> victimRole = session.roleInRound(hit.getUniqueId());
        if (attackerRole.isEmpty() || victimRole.isEmpty()) {
            return false;
        }
        return !CombatRules.allowsDamage(
                attackerRole.get(), victimRole.get(), configService.settings().rules());
    }

    /** Whether a runner still in the round is looking at this hunter, so they may not move. */
    public boolean isHeldByGaze(Player hunter) {
        if (!configService.settings().rules().freezeWhenLookedAt()
                || session.state() != GameState.RUNNING
                || !session.isPlaying(hunter.getUniqueId(), Role.HUNTER)) {
            return false;
        }
        Vector3 target = toVector3(hunter.getBoundingBox().getCenter());
        return session.remainingRunners().stream()
                .map(server::getPlayer)
                .filter(Objects::nonNull)
                .filter(runner -> runner.getWorld().equals(hunter.getWorld()))
                // The angle test is cheap; line of sight traces blocks, so it goes last.
                .anyMatch(runner -> isLookingAt(runner, target) && lineOfSight.test(runner, hunter));
    }

    private static boolean isLookingAt(Player runner, Vector3 target) {
        Location eye = runner.getEyeLocation();
        return GazeGeometry.isLookingAt(
                toVector3(eye.toVector()),
                toVector3(eye.getDirection()),
                target,
                GAZE_MAX_ANGLE_DEGREES,
                GAZE_MAX_DISTANCE);
    }

    private static Optional<Player> attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player);
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return Optional.of(shooter);
        }
        return Optional.empty();
    }

    private static Vector3 toVector3(Vector vector) {
        return new Vector3(vector.getX(), vector.getY(), vector.getZ());
    }
}
