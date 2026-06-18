package com.mojang.minecraft.modloader;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.tile.Tile;

public interface IMod {

    String getModId();
    String getName();
    String getVersion();
    void onLoad(ModLoader loader);

    default void onTick(Level level) {}
    default void onTileDestroy(Tile tile, Level level, int x, int y, int z) {}
    default void onTilePlace(Tile tile, Level level, int x, int y, int z) {}

    // ─── Новые хуки для модов ─────────────────────────────────────────────
    default void onLevelGenerated(Level level) {}
    default void onTileRandomTick(Level level, int x, int y, int z, Tile tile) {}
    default void onPlayerJump(Player player) {}
    default void onEntitySpawn(Zombie zombie, Level level) {}
    default void onKeyPress(int key) {}
    default void onEntityTick(Zombie zombie, Level level) {}
    default void onEntityRemove(Zombie zombie, Level level) {}
    default void onPlayerTick(Player player, Level level) {}
}
