package com.mojang.minecraft.modloader.event;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.tile.Tile;

/**
 * Все стандартные события игры.
 *
 * Мод подписывается так:
 * <pre>
 *   EventBus.subscribe(Events.TileBreakEvent.class, e -> {
 *       System.out.println("Сломан блок: " + e.tile + " на " + e.x + "," + e.y + "," + e.z);
 *       // e.setCancelled(true); — не сломать блок
 *   });
 * </pre>
 */
public final class Events {

    private Events() {}

    // ──────────────────────────────────────────── PLAYER ─────────────────────

    /** Вызывается каждый тик игрока. */
    public static class PlayerTickEvent extends ModEvent {
        public final Player player;
        public final Level level;
        public PlayerTickEvent(Player player, Level level) {
            this.player = player; this.level = level;
        }
    }

    /** Игрок прыгает. */
    public static class PlayerJumpEvent extends ModEvent {
        public final Player player;
        public PlayerJumpEvent(Player player) { this.player = player; }
    }

    /** Игрок умирает (y < -100). Cancellable — отмена сохраняет жизнь. */
    public static class PlayerDeathEvent extends CancellableEvent {
        public final Player player;
        public PlayerDeathEvent(Player player) { this.player = player; }
    }

    /** Игрок нажимает клавишу. */
    public static class KeyPressEvent extends ModEvent {
        public final int key;
        public KeyPressEvent(int key) { this.key = key; }
    }

    // ──────────────────────────────────────────── TILES ──────────────────────

    /** Игрок ломает блок. Cancellable. */
    public static class TileBreakEvent extends CancellableEvent {
        public final Tile tile;
        public final Level level;
        public final int x, y, z;
        public TileBreakEvent(Tile tile, Level level, int x, int y, int z) {
            this.tile = tile; this.level = level;
            this.x = x; this.y = y; this.z = z;
        }
    }

    /** Игрок ставит блок. Cancellable. */
    public static class TilePlaceEvent extends CancellableEvent {
        public final Tile tile;
        public final Level level;
        public final int x, y, z;
        public TilePlaceEvent(Tile tile, Level level, int x, int y, int z) {
            this.tile = tile; this.level = level;
            this.x = x; this.y = y; this.z = z;
        }
    }

    /** Случайный тик блока. */
    public static class TileRandomTickEvent extends ModEvent {
        public final Level level;
        public final int x, y, z;
        public final Tile tile;
        public TileRandomTickEvent(Level level, int x, int y, int z, Tile tile) {
            this.level = level; this.x = x; this.y = y; this.z = z; this.tile = tile;
        }
    }

    // ──────────────────────────────────────────── ENTITIES ───────────────────

    /** Зомби заспавнился. */
    public static class EntitySpawnEvent extends ModEvent {
        public final Zombie entity;
        public final Level level;
        public EntitySpawnEvent(Zombie entity, Level level) {
            this.entity = entity; this.level = level;
        }
    }

    /** Зомби удалён. */
    public static class EntityRemoveEvent extends ModEvent {
        public final Zombie entity;
        public final Level level;
        public EntityRemoveEvent(Zombie entity, Level level) {
            this.entity = entity; this.level = level;
        }
    }

    /** Тик зомби. */
    public static class EntityTickEvent extends ModEvent {
        public final Zombie entity;
        public final Level level;
        public EntityTickEvent(Zombie entity, Level level) {
            this.entity = entity; this.level = level;
        }
    }

    // ──────────────────────────────────────────── WORLD ──────────────────────

    /** Мир был сгенерирован. */
    public static class LevelGeneratedEvent extends ModEvent {
        public final Level level;
        public LevelGeneratedEvent(Level level) { this.level = level; }
    }

    /** Мир загружен из файла. */
    public static class LevelLoadedEvent extends ModEvent {
        public final Level level;
        public LevelLoadedEvent(Level level) { this.level = level; }
    }

    /** Мир сохраняется. Cancellable. */
    public static class LevelSaveEvent extends CancellableEvent {
        public final Level level;
        public LevelSaveEvent(Level level) { this.level = level; }
    }

    // ──────────────────────────────────────────── RENDER ─────────────────────

    /**
     * Вызывается каждый кадр перед рендером сцены.
     * Можно добавить свои GL-вызовы.
     */
    public static class RenderPreEvent extends ModEvent {
        public final float partialTicks;
        public RenderPreEvent(float partialTicks) { this.partialTicks = partialTicks; }
    }

    /**
     * Вызывается каждый кадр ПОСЛЕ рендера сцены (перед HUD).
     */
    public static class RenderPostEvent extends ModEvent {
        public final float partialTicks;
        public RenderPostEvent(float partialTicks) { this.partialTicks = partialTicks; }
    }

    /**
     * Вызывается при рендере HUD (в ortho-режиме).
     */
    public static class RenderHudEvent extends ModEvent {
        public final int screenWidth;
        public final int screenHeight;
        public final float partialTicks;
        public RenderHudEvent(int w, int h, float pt) {
            this.screenWidth = w; this.screenHeight = h; this.partialTicks = pt;
        }
    }
}
