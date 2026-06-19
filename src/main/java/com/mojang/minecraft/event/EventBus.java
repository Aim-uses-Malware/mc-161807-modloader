package com.mojang.minecraft.modloader.event;

import java.util.*;
import java.util.function.Consumer;

/**
 * Событийная шина (Event Bus) для модов.
 *
 * Позволяет модам подписываться на typed events и получать уведомления
 * без прямой зависимости от ModLoader.
 *
 * <pre>
 *   // Подписка
 *   EventBus.subscribe(PlayerTickEvent.class, e -> {
 *       if (e.player.y < -10) System.out.println("игрок в пропасти!");
 *   });
 *
 *   // Публикация (из движка или другого мода)
 *   EventBus.post(new PlayerTickEvent(player, level));
 *
 *   // Отмена события (если оно Cancellable)
 *   EventBus.subscribe(TilePlaceEvent.class, e -> {
 *       if (badSpot(e.x, e.y, e.z)) e.setCancelled(true);
 *   });
 * </pre>
 */
public class EventBus {

    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends ModEvent>, List<Consumer>> listeners = new HashMap<>();

    private EventBus() {}

    // ─── Registration ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static <T extends ModEvent> void subscribe(Class<T> eventType, Consumer<T> handler) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ModEvent> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<Consumer> list = listeners.get(eventType);
        if (list != null) list.remove(handler);
    }

    // ─── Dispatch ─────────────────────────────────────────────────────────────

    /**
     * Рассылает событие всем подписчикам.
     *
     * @return true если событие не было отменено (или не Cancellable)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends ModEvent> boolean post(T event) {
        List<Consumer> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer handler : list) {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    System.err.println("[EventBus] Handler threw for " + event.getClass().getSimpleName() + ": " + e.getMessage());
                }
                if (event instanceof CancellableEvent && ((CancellableEvent) event).isCancelled()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void clearAll() {
        listeners.clear();
    }
}
