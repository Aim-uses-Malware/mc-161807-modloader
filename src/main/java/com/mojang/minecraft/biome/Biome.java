package com.mojang.minecraft.biome;

import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.tile.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Биом — задаёт правила генерации мира на определённых участках.
 *
 * Стандартные биомы зарегистрированы в BiomeManager.
 * Мод может добавить свой биом:
 * <pre>
 *   BiomeManager.register(new MyDesertBiome(10));
 * </pre>
 *
 * Биомы влияют на:
 *  - Тип поверхностного блока
 *  - Цвет травы/воды
 *  - Декорации при генерации (деревья, структуры)
 */
public abstract class Biome {

    /** Реестр всех биомов. */
    private static final List<Biome> registry = new ArrayList<>();

    // Стандартные биомы
    public static final Biome PLAINS = new Biome("plains", 0x79b86e) {
        @Override public int getSurfaceTile() { return Tile.grass.id; }
        @Override public int getSubSurfaceTile() { return Tile.dirt.id; }
    };

    public static final Biome DESERT = new Biome("desert", 0xd4b36b) {
        @Override public int getSurfaceTile() { return Tile.dirt.id; }
        @Override public int getSubSurfaceTile() { return Tile.rock.id; }
        @Override public float getTreeChance() { return 0.0f; }
    };

    public static final Biome FOREST = new Biome("forest", 0x3d7a3d) {
        @Override public int getSurfaceTile() { return Tile.grass.id; }
        @Override public int getSubSurfaceTile() { return Tile.dirt.id; }
        @Override public float getTreeChance() { return 0.15f; }
    };

    static {
        register(PLAINS);
        register(DESERT);
        register(FOREST);
    }

    // ─── Biome instance ───────────────────────────────────────────────────────

    public final String name;
    public final int    grassColor;

    public Biome(String name, int grassColor) {
        this.name       = name;
        this.grassColor = grassColor;
    }

    /** ID блока поверхности. */
    public abstract int getSurfaceTile();

    /** ID блока под поверхностью. */
    public abstract int getSubSurfaceTile();

    /** Вероятность генерации дерева на клетку. */
    public float getTreeChance() { return 0.03f; }

    /** Генерирует декорации в данной колонке после создания мира. */
    public void decorate(Level level, int x, int z, Random rand) {
        // Базовая реализация — ничего
    }

    // ─── Registry ─────────────────────────────────────────────────────────────

    public static void register(Biome biome) {
        // Не дублируем
        for (Biome b : registry) {
            if (b.name.equals(biome.name)) {
                System.out.println("[BiomeManager] Biome already registered: " + biome.name);
                return;
            }
        }
        registry.add(biome);
        System.out.println("[BiomeManager] Registered biome: " + biome.name);
    }

    public static List<Biome> getAll() {
        return java.util.Collections.unmodifiableList(registry);
    }

    /**
     * Выбирает биом для данной координаты (по noise-значению).
     *
     * @param noise Значение 0.0-1.0
     */
    public static Biome forNoise(float noise) {
        int idx = (int) (noise * registry.size());
        idx = Math.max(0, Math.min(registry.size() - 1, idx));
        return registry.get(idx);
    }

    @Override
    public String toString() { return "Biome[" + name + "]"; }
}
