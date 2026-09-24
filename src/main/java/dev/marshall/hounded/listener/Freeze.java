package dev.marshall.hounded.listener;

import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerMoveEvent;

/** Shared by the headstart freeze and the looked-at freeze: {@code frozen} says who is held. */
final class Freeze {

    private Freeze() {}

    /** Undoes a change of position if the player may not move. */
    static void holdPositionIfFrozen(PlayerMoveEvent event, Predicate<Player> frozen) {
        if (event.hasChangedPosition() && frozen.test(event.getPlayer())) {
            holdPosition(event);
        }
    }

    static void cancelIfFrozen(Cancellable event, Entity actor, Predicate<Player> frozen) {
        if (actor instanceof Player player && frozen.test(player)) {
            event.setCancelled(true);
        }
    }

    /** Keeps the new view direction: cancelling the whole move would also lock the camera. */
    private static void holdPosition(PlayerMoveEvent event) {
        Location held = event.getFrom().clone();
        held.setYaw(event.getTo().getYaw());
        held.setPitch(event.getTo().getPitch());
        event.setTo(held);
    }
}
