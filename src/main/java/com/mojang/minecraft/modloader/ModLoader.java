package com.mojang.minecraft.modloader;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.biome.Biome;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.crafting.CraftingManager;
import com.mojang.minecraft.item.Item;
import com.mojang.minecraft.item.ItemStack;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.modloader.event.EventBus;
import com.mojang.minecraft.modloader.event.Events;
import com.mojang.minecraft.modloader.sound.SoundManager;
import com.mojang.minecraft.structure.Structure;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ModLoader v2 — полная система загрузки модов для mc-161807 pre-classic.
 *
 * Возможности:
 *  - Загрузка .jar из папки mods/
 *  - Регистрация блоков (Tile), предметов (Item), рецептов, биомов, структур, звуков
 *  - Event Bus — typed events для модов
 *  - Хуки: tick, render (pre/post/HUD), entity, player, level, key
 *  - Поддержка 3D-моделей через modloader.model.OBJLoader / ModelPart
 *  - Конфиги (config/<modId>.properties)
 *  - Метаданные (ModMetadata + modinfo.json в jar)
 *  - Многоуровневый логгер
 */
public class ModLoader {

    private static ModLoader instance;

    /** Все успешно загруженные моды. */
    private final List<IMod> mods = new ArrayList<>();

    /** Метаданные модов по modId. */
    private final Map<String, ModMetadata> metaMap = new LinkedHashMap<>();

    /** Зарегистрированные блоки (id → Tile). */
    private final List<Tile> modTiles = new ArrayList<>();

    /** Зарегистрированные предметы (id → Item). */
    private final List<Item> modItems = new ArrayList<>();

    /** Система крафта. */
    private final CraftingManager craftingManager = new CraftingManager();

    /** Структуры для генерации. entry = (структура, вероятность). */
    private final List<Map.Entry<Structure, Float>> structures = new ArrayList<>();

    /** Конфиги по modId. */
    private final Map<String, ModConfig> configs = new HashMap<>();

    private ModLoader() {}

    // ─── Singleton ────────────────────────────────────────────────────────────

    public static ModLoader getInstance() {
        if (instance == null) instance = new ModLoader();
        return instance;
    }

    public static void init() {
        getInstance().loadMods();
    }

    // ─── Bootstrap ────────────────────────────────────────────────────────────

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

        // Сортируем по имени для детерминированного порядка загрузки
        Arrays.sort(jars, Comparator.comparing(File::getName));

        for (File jar : jars) {
            try {
                loadJar(jar);
            } catch (Exception e) {
                log("[ERROR] Failed to load " + jar.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        log("=== ModLoader v2: " + mods.size() + " mod(s) loaded ===");
        for (IMod mod : mods) {
            log("  ✓ " + mod.getName() + " v" + mod.getVersion() + " [" + mod.getModId() + "]");
        }
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

                        // Проверяем дубликаты modId
                        if (metaMap.containsKey(mod.getModId())) {
                            log("  [SKIP] Duplicate modId: " + mod.getModId());
                            continue;
                        }

                        mods.add(mod);
                        ModMetadata meta = mod.getMetadata();
                        meta.sourceFile = jar.getName();
                        metaMap.put(mod.getModId(), meta);

                        log("  Found mod: " + mod.getName() + " v" + mod.getVersion()
                            + " [" + mod.getModId() + "]"
                            + (meta.author.isEmpty() ? "" : " by " + meta.author));
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

    /** Регистрирует кастомный блок. ID должен быть в диапазоне [10..255]. */
    public void registerTile(Tile tile) {
        if (tile.id < 10 || tile.id > 255)
            throw new IllegalArgumentException("Tile id must be [10..255], got " + tile.id);
        if (Tile.tiles[tile.id] != null)
            throw new IllegalArgumentException("Tile id " + tile.id + " already taken by " + Tile.tiles[tile.id].getClass().getName());
        modTiles.add(tile);
        log("  Registered tile id=" + tile.id + " (" + tile.getClass().getSimpleName() + ")");
    }

    /** Регистрирует кастомный предмет. ID должен быть в диапазоне [256..4095]. */
    public void registerItem(Item item) {
        if (item.id < 256 || item.id >= Item.items.length)
            throw new IllegalArgumentException("Item id must be [256..4095], got " + item.id);
        modItems.add(item);
        log("  Registered item id=" + item.id + " (" + item.name + ")");
    }

    /** Регистрирует структуру мира. spawnChance — вероятность на чанк. */
    public void registerStructure(Structure structure, float spawnChance) {
        structure.spawnChance = spawnChance;
        structures.add(new AbstractMap.SimpleEntry<>(structure, spawnChance));
        log("  Registered structure: " + structure.name + " (chance=" + spawnChance + ")");
    }

    /** Регистрирует биом. */
    public void registerBiome(Biome biome) {
        Biome.register(biome);
    }

    /** Регистрирует звук. path — путь к .wav в classpath или файловой системе. */
    public void registerSound(String name, String path) {
        SoundManager.register(name, path);
    }

    /** Получает (или создаёт) конфиг мода. */
    public ModConfig getConfig(String modId) {
        return configs.computeIfAbsent(modId, ModConfig::new);
    }

    /** Доступ к системе крафта для регистрации рецептов. */
    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    // ─── Render hooks ─────────────────────────────────────────────────────────

    public void dispatchRenderPre(float partialTicks) {
        EventBus.post(new Events.RenderPreEvent(partialTicks));
        for (IMod mod : mods) {
            try { mod.onRenderPre(partialTicks); }
            catch (Exception e) { log("[ERROR] " + mod.getModId() + " onRenderPre: " + e.getMessage()); }
        }
    }

    public void dispatchRenderPost(float partialTicks) {
        EventBus.post(new Events.RenderPostEvent(partialTicks));
        for (IMod mod : mods) {
            try { mod.onRenderPost(partialTicks); }
            catch (Exception e) { log("[ERROR] " + mod.getModId() + " onRenderPost: " + e.getMessage()); }
        }
    }

    public void dispatchRenderHud(int w, int h, float partialTicks) {
        EventBus.post(new Events.RenderHudEvent(w, h, partialTicks));
        for (IMod mod : mods) {
            try { mod.onRenderHud(w, h, partialTicks); }
            catch (Exception e) { log("[ERROR] " + mod.getModId() + " onRenderHud: " + e.getMessage()); }
        }
    }

    // ─── Event dispatch ───────────────────────────────────────────────────────

    public void dispatchTick(Level level) {
        for (IMod mod : mods) safe(mod, "onTick", () -> mod.onTick(level));
    }

    public void dispatchTileDestroy(Tile tile, Level level, int x, int y, int z) {
        EventBus.post(new Events.TileBreakEvent(tile, level, x, y, z));
        for (IMod mod : mods) safe(mod, "onTileDestroy", () -> mod.onTileDestroy(tile, level, x, y, z));
    }

    public void dispatchTilePlace(Tile tile, Level level, int x, int y, int z) {
        EventBus.post(new Events.TilePlaceEvent(tile, level, x, y, z));
        for (IMod mod : mods) safe(mod, "onTilePlace", () -> mod.onTilePlace(tile, level, x, y, z));
    }

    public void dispatchLevelGenerated(Level level) {
        EventBus.post(new Events.LevelGeneratedEvent(level));
        // Расставить структуры при генерации
        placeStructures(level);
        for (IMod mod : mods) safe(mod, "onLevelGenerated", () -> mod.onLevelGenerated(level));
    }

    public void dispatchLevelLoaded(Level level) {
        EventBus.post(new Events.LevelLoadedEvent(level));
        for (IMod mod : mods) safe(mod, "onLevelLoaded", () -> mod.onLevelLoaded(level));
    }

    public void dispatchTileRandomTick(Level level, int x, int y, int z, Tile tile) {
        EventBus.post(new Events.TileRandomTickEvent(level, x, y, z, tile));
        for (IMod mod : mods) safe(mod, "onTileRandomTick", () -> mod.onTileRandomTick(level, x, y, z, tile));
    }

    public void dispatchPlayerJump(Player player) {
        EventBus.post(new Events.PlayerJumpEvent(player));
        for (IMod mod : mods) safe(mod, "onPlayerJump", () -> mod.onPlayerJump(player));
    }

    public void dispatchEntitySpawn(Zombie zombie, Level level) {
        EventBus.post(new Events.EntitySpawnEvent(zombie, level));
        for (IMod mod : mods) safe(mod, "onEntitySpawn", () -> mod.onEntitySpawn(zombie, level));
    }

    public void dispatchKeyPress(int key) {
        EventBus.post(new Events.KeyPressEvent(key));
        for (IMod mod : mods) safe(mod, "onKeyPress", () -> mod.onKeyPress(key));
    }

    public void dispatchEntityTick(Zombie zombie, Level level) {
        EventBus.post(new Events.EntityTickEvent(zombie, level));
        for (IMod mod : mods) safe(mod, "onEntityTick", () -> mod.onEntityTick(zombie, level));
    }

    public void dispatchEntityRemove(Zombie zombie, Level level) {
        EventBus.post(new Events.EntityRemoveEvent(zombie, level));
        for (IMod mod : mods) safe(mod, "onEntityRemove", () -> mod.onEntityRemove(zombie, level));
    }

    public void dispatchPlayerTick(Player player, Level level) {
        EventBus.post(new Events.PlayerTickEvent(player, level));
        for (IMod mod : mods) safe(mod, "onPlayerTick", () -> mod.onPlayerTick(player, level));
    }

    // Алиасы для совместимости
    public void dispatchPlayerDestroyTile(Tile tile, Level level, int x, int y, int z) {
        dispatchTileDestroy(tile, level, x, y, z);
    }
    public void dispatchPlayerPlaceTile(Tile tile, Level level, int x, int y, int z) {
        dispatchTilePlace(tile, level, x, y, z);
    }

    // ─── Structure placement ──────────────────────────────────────────────────

    private void placeStructures(Level level) {
        if (structures.isEmpty()) return;
        Random rand = new Random();
        int placed = 0;

        // Сканируем поверхность
        for (int x = 0; x < level.width; x += 16) {
            for (int z = 0; z < level.height; z += 16) {
                for (Map.Entry<Structure, Float> entry : structures) {
                    if (rand.nextFloat() < entry.getValue()) {
                        // Случайная позиция внутри чанка
                        int px = x + rand.nextInt(Math.min(16, level.width - x));
                        int pz = z + rand.nextInt(Math.min(16, level.height - z));
                        entry.getKey().placeOnSurface(level, px, pz, rand);
                        placed++;
                    }
                }
            }
        }
        if (placed > 0) log("Placed " + placed + " structure(s) during world gen.");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void safe(IMod mod, String hook, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log("[ERROR] " + mod.getModId() + " threw in " + hook + ": " + e.getMessage());
        }
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public List<IMod>             getMods()            { return Collections.unmodifiableList(mods); }
    public List<Tile>             getModTiles()        { return Collections.unmodifiableList(modTiles); }
    public List<Item>             getModItems()        { return Collections.unmodifiableList(modItems); }
    public CraftingManager        getCrafting()        { return craftingManager; }
    public Map<String, ModMetadata> getAllMeta()       { return Collections.unmodifiableMap(metaMap); }
    public ModMetadata            getMeta(String id)   { return metaMap.get(id); }
    public int                    getModCount()        { return mods.size(); }

    private static void log(String msg) {
        System.out.println("[ModLoader] " + msg);
    }
}
