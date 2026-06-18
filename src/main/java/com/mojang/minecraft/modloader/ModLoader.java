package com.mojang.minecraft.modloader;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.tile.Tile;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ForgeModLoader — loads .jar mods from the "mods" folder next to the game working dir.
 */
public class ModLoader {

    private static ModLoader instance;

    /** All successfully loaded mods. */
    private final List<IMod> mods = new ArrayList<>();

    /** All tiles registered by mods (id → Tile). Complementary to Tile.tiles[]. */
    private final List<Tile> modTiles = new ArrayList<>();

    private ModLoader() {}

    // ─── Singleton ────────────────────────────────────────────────────────────

    public static ModLoader getInstance() {
        if (instance == null) {
            instance = new ModLoader();
        }
        return instance;
    }

    // ─── Bootstrap ────────────────────────────────────────────────────────────

    public static void init() {
        ModLoader loader = getInstance();
        loader.loadMods();
    }

    private void loadMods() {
        File modsDir = new File("mods");
        if (!modsDir.exists()) {
            modsDir.mkdirs();
            log("Created mods directory: " + modsDir.getAbsolutePath());
        }

        File[] jars = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            log("No mods found in " + modsDir.getAbsolutePath());
            return;
        }

        for (File jar : jars) {
            try {
                loadJar(jar);
            } catch (Exception e) {
                log("[ERROR] Failed to load " + jar.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        log("Loaded " + mods.size() + " mod(s).");
    }

    private void loadJar(File jar) throws Exception {
        log("Loading: " + jar.getName());

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jar.toURI().toURL()},
                getClass().getClassLoader()
        );

        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.endsWith(".class") || name.contains("$")) continue;

                String className = name.replace('/', '.').replace(".class", "");

                try {
                    Class<?> clazz = classLoader.loadClass(className);

                    if (IMod.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                        IMod mod = (IMod) clazz.getDeclaredConstructor().newInstance();
                        mods.add(mod);

                        log("  Found mod: " + mod.getName() + " v" + mod.getVersion() + " [" + mod.getModId() + "]");
                        mod.onLoad(this);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                } catch (Exception e) {
                    log("  [WARN] Could not instantiate " + className + ": " + e.getMessage());
                }
            }
        }
    }

    // ─── Registration API ─────────────────────────────────────────────────────

    public void registerTile(Tile tile) {
        if (tile.id < 10 || tile.id > 255) {
            throw new IllegalArgumentException("Mod tile id must be in range [10..255], got " + tile.id);
        }
        if (Tile.tiles[tile.id] != null) {
            throw new IllegalArgumentException("Tile id " + tile.id + " is already registered by "
                    + Tile.tiles[tile.id].getClass().getName());
        }
        modTiles.add(tile);
        log("  Registered tile id=" + tile.id + " class=" + tile.getClass().getSimpleName());
    }

    // ─── Event dispatch (Пофикшенные и рабочие хуки) ─────────────────────────

    public void dispatchTick(com.mojang.minecraft.level.Level level) {
        for (IMod mod : mods) {
            try {
                mod.onTick(level);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onTick: " + e.getMessage());
            }
        }
    }

    public void dispatchTileDestroy(Tile tile, com.mojang.minecraft.level.Level level, int x, int y, int z) {
        for (IMod mod : mods) {
            try {
                mod.onTileDestroy(tile, level, x, y, z);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onTileDestroy: " + e.getMessage());
            }
        }
    }

    public void dispatchTilePlace(Tile tile, com.mojang.minecraft.level.Level level, int x, int y, int z) {
        for (IMod mod : mods) {
            try {
                mod.onTilePlace(tile, level, x, y, z);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onTilePlace: " + e.getMessage());
            }
        }
    }

    public void dispatchLevelGenerated(com.mojang.minecraft.level.Level level) {
        for (IMod mod : mods) {
            try {
                mod.onLevelGenerated(level);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onLevelGenerated: " + e.getMessage());
            }
        }
    }

    public void dispatchTileRandomTick(com.mojang.minecraft.level.Level level, int x, int y, int z, Tile tile) {
        for (IMod mod : mods) {
            try {
                mod.onTileRandomTick(level, x, y, z, tile);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onTileRandomTick: " + e.getMessage());
            }
        }
    }

    public void dispatchPlayerJump(Player player) {
        for (IMod mod : mods) {
            try {
                mod.onPlayerJump(player);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onPlayerJump: " + e.getMessage());
            }
        }
    }

    public void dispatchEntitySpawn(Zombie zombie, com.mojang.minecraft.level.Level level) {
        for (IMod mod : mods) {
            try {
                mod.onEntitySpawn(zombie, level);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onEntitySpawn: " + e.getMessage());
            }
        }
    }

    public void dispatchKeyPress(int key) {
        for (IMod mod : mods) {
            try {
                mod.onKeyPress(key);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onKeyPress: " + e.getMessage());
            }
        }
    }

    public void dispatchEntityTick(Zombie zombie, com.mojang.minecraft.level.Level level) {
        for (IMod mod : mods) {
            try {
                mod.onEntityTick(zombie, level);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onEntityTick: " + e.getMessage());
            }
        }
    }

    public void dispatchEntityRemove(Zombie zombie, com.mojang.minecraft.level.Level level) {
        for (IMod mod : mods) {
            try {
                mod.onEntityRemove(zombie, level);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onEntityRemove: " + e.getMessage());
            }
        }
    }

    public void dispatchPlayerTick(Player player, com.mojang.minecraft.level.Level level) {
        for (IMod mod : mods) {
            try {
                mod.onPlayerTick(player, level);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onPlayerTick: " + e.getMessage());
            }
        }
    }

    public void dispatchPlayerDestroyTile(Tile tile, com.mojang.minecraft.level.Level level, int x, int y, int z) {
        // Прокидываем в стандартный метод уничтожения блоков
        dispatchTileDestroy(tile, level, x, y, z);
    }

    public void dispatchPlayerPlaceTile(Tile tile, com.mojang.minecraft.level.Level level, int x, int y, int z) {
        // Прокидываем в стандартный метод установки блоков
        dispatchTilePlace(tile, level, x, y, z);
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public List<IMod> getMods() {
        return Collections.unmodifiableList(mods);
    }

    public List<Tile> getModTiles() {
        return Collections.unmodifiableList(modTiles);
    }

    private static void log(String msg) {
        System.out.println("[ModLoader] " + msg);
    }
}
