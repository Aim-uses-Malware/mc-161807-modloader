package com.mojang.minecraft.modloader.event;

/**
 * Событие, которое можно отменить.
 * Движок проверяет isCancelled() и пропускает действие если true.
 */
public abstract class CancellableEvent extends ModEvent {

    private boolean cancelled = false;

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
