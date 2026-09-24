package dev.marshall.hounded.listener;

import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/** Stops a player walking while still letting them look around. */
final class PositionLock {

    private PositionLock() {}

    /** Undoes a change of position if {@code frozen} says the player may not move. */
    static void holdIfFrozen(PlayerMoveEvent event, Predicate<Player> frozen) {
        if (event.hasChangedPosition() && frozen.test(event.getPlayer())) {
            holdPosition(event);
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
