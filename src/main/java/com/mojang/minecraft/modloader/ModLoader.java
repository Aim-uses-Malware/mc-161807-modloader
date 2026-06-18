package com.mojang.minecraft.modloader;

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
 *
 * Lifecycle:
 *   1. ModLoader.init() is called from Minecraft.init()
 *   2. Every .jar in ./mods/ is scanned for a class implementing IMod
 *   3. That class is instantiated and IMod.onLoad(this) is called
 *   4. Registered tiles / hooks become active for the rest of the session
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

    /**
     * Scan the "mods" directory, load every valid .jar, and call IMod.onLoad().
     * Call this from Minecraft.init() before the game loop starts.
     */
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

    /**
     * Open a .jar, find every class, look for ones that implement IMod,
     * instantiate them, and call onLoad().
     */
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

                // Only look at .class files, skip inner classes
                if (!name.endsWith(".class") || name.contains("$")) continue;

                // Convert path → class name
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
                    // Dependency classes in the jar — safe to skip
                } catch (Exception e) {
                    log("  [WARN] Could not instantiate " + className + ": " + e.getMessage());
                }
            }
        }
    }

    // ─── Registration API (used by mods in onLoad) ────────────────────────────

    /**
     * Register a custom tile.
     * The tile's id must be in range [10..255] to avoid conflicts with vanilla tiles (1–6).
     *
     * @param tile Tile instance to register (its id is set via the Tile(id, textureId) constructor)
     * @throws IllegalArgumentException if the id is already taken or out of range
     */
    public void registerTile(Tile tile) {
        if (tile.id < 10 || tile.id > 255) {
            throw new IllegalArgumentException("Mod tile id must be in range [10..255], got " + tile.id);
        }
        if (Tile.tiles[tile.id] != null) {
            throw new IllegalArgumentException("Tile id " + tile.id + " is already registered by "
                    + Tile.tiles[tile.id].getClass().getName());
        }
        // Tile constructor already stores itself in Tile.tiles[id]
        modTiles.add(tile);
        log("  Registered tile id=" + tile.id + " class=" + tile.getClass().getSimpleName());
    }

    // ─── Event dispatch (called from patched Minecraft/Level) ────────────────

    /** Dispatch tick event to all mods. */
    public void dispatchTick(com.mojang.minecraft.level.Level level) {
        for (IMod mod : mods) {
            try {
                mod.onTick(level);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onTick: " + e.getMessage());
            }
        }
    }

    /** Dispatch tile-destroy event to all mods. */
    public void dispatchTileDestroy(Tile tile, com.mojang.minecraft.level.Level level, int x, int y, int z) {
        for (IMod mod : mods) {
            try {
                mod.onTileDestroy(tile, level, x, y, z);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onTileDestroy: " + e.getMessage());
            }
        }
    }

    /** Dispatch tile-place event to all mods. */
    public void dispatchTilePlace(Tile tile, com.mojang.minecraft.level.Level level, int x, int y, int z) {
        for (IMod mod : mods) {
            try {
                mod.onTilePlace(tile, level, x, y, z);
            } catch (Exception e) {
                log("[ERROR] " + mod.getModId() + " threw in onTilePlace: " + e.getMessage());
            }
        }
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    /** Unmodifiable view of all loaded mods. */
    public List<IMod> getMods() {
        return Collections.unmodifiableList(mods);
    }

    /** Unmodifiable view of all mod-registered tiles. */
    public List<Tile> getModTiles() {
        return Collections.unmodifiableList(modTiles);
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private static void log(String msg) {
        System.out.println("[ModLoader] " + msg);
    }
}
