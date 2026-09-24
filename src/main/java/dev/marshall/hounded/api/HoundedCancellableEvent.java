package dev.marshall.hounded.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/** The cancel flag shared by Hounded's pre-events. Each event still declares its own handler list. */
abstract sealed class HoundedCancellableEvent extends Event implements Cancellable
        permits HoundedRoundStartEvent, HoundedRoleChangeEvent {
    private boolean cancelled;

    /** @return whether a plugin has cancelled what this event announces */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /** @param cancel true to stop what this event announces from happening */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
