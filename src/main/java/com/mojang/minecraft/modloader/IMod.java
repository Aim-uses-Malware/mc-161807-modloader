package com.mojang.minecraft.modloader;

import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.tile.Tile;

/**
 * Base interface for all mods.
 * Every mod .jar must contain exactly one class implementing this interface.
 *
 * Minimal example mod:
 * <pre>
 * public class MyMod implements IMod {
 *     public String getModId()      { return "mymod"; }
 *     public String getName()       { return "My Mod"; }
 *     public String getVersion()    { return "1.0"; }
 *     public void onLoad(ModLoader loader) {
 *         // register tiles, subscribe events, etc.
 *     }
 * }
 * </pre>
 */
public interface IMod {

    /** Unique identifier, e.g. "mymod". Must be lowercase, no spaces. */
    String getModId();

    /** Human-readable name shown in the mod list. */
    String getName();

    /** Version string, e.g. "1.0.0". */
    String getVersion();

    /**
     * Called once after the mod is loaded.
     * Register tiles / hooks here.
     *
     * @param loader The ModLoader instance for registration helpers
     */
    void onLoad(ModLoader loader);

    /**
     * Called every game tick (20 times/sec).
     * Override for per-tick logic.
     */
    default void onTick(Level level) {}

    /**
     * Called just before a tile is destroyed by the player.
     *
     * @param tile The tile being destroyed
     * @param level Current level
     * @param x Tile X
     * @param y Tile Y
     * @param z Tile Z
     */
    default void onTileDestroy(Tile tile, Level level, int x, int y, int z) {}

    /**
     * Called just after a tile is placed by the player.
     *
     * @param tile The tile that was placed
     * @param level Current level
     * @param x Tile X
     * @param y Tile Y
     * @param z Tile Z
     */
    default void onTilePlace(Tile tile, Level level, int x, int y, int z) {}
}
