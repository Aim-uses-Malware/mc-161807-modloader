package com.mojang.minecraft.structure;

import com.mojang.minecraft.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Система структур — позволяет определить шаблон блоков и разместить его в мире.
 *
 * Регистрация через IMod:
 * <pre>
 *   Structure tower = new Structure("tower")
 *       .set(0,0,0, Tile.stoneBrick.id)
 *       .set(0,1,0, Tile.stoneBrick.id)
 *       .set(0,2,0, Tile.stoneBrick.id);
 *
 *   loader.registerStructure(tower, 0.001f); // вероятность на чанк
 * </pre>
 *
 * Размещение в мире:
 * <pre>
 *   structure.place(level, x, y, z);
 * </pre>
 */
public class Structure {

    public final String name;

    /** Список блоков: [dx, dy, dz, tileId] */
    private final List<int[]> blocks = new ArrayList<>();

    /** Вероятность появления за один чанк при генерации мира. */
    public float spawnChance = 0.005f;

    public Structure(String name) {
        this.name = name;
    }

    // ─── Builder API ──────────────────────────────────────────────────────────

    public Structure set(int dx, int dy, int dz, int tileId) {
        blocks.add(new int[]{dx, dy, dz, tileId});
        return this;
    }

    /** Добавляет вертикальный столб из одного типа блока. */
    public Structure pillar(int dx, int dz, int fromY, int toY, int tileId) {
        for (int y = fromY; y <= toY; y++) {
            set(dx, y, dz, tileId);
        }
        return this;
    }

    /** Добавляет горизонтальный слой (floor/ceiling). */
    public Structure layer(int dy, int x1, int z1, int x2, int z2, int tileId) {
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++)
                set(x, dy, z, tileId);
        return this;
    }

    // ─── Placement ────────────────────────────────────────────────────────────

    /**
     * Размещает структуру в мире с началом в (x, y, z).
     *
     * @param replaceAir Если false, не заменяет существующие блоки (только воздух)
     */
    public void place(Level level, int x, int y, int z, boolean replaceAir) {
        for (int[] block : blocks) {
            int bx = x + block[0];
            int by = y + block[1];
            int bz = z + block[2];
            int id = block[3];

            if (replaceAir || level.getTile(bx, by, bz) == 0) {
                level.setTile(bx, by, bz, id);
            }
        }
    }

    /** Размещает, заменяя все блоки включая непустые. */
    public void place(Level level, int x, int y, int z) {
        place(level, x, y, z, true);
    }

    /**
     * Пробует разместить структуру в заданной колонке, находя первый подходящий Y.
     */
    public void placeOnSurface(Level level, int x, int z, Random rand) {
        // Ищем верхний блок
        int y = level.depth - 1;
        while (y > 0 && level.getTile(x, y, z) == 0) y--;

        place(level, x, y + 1, z, true);
    }

    public List<int[]> getBlocks() {
        return java.util.Collections.unmodifiableList(blocks);
    }

    // ─── Predefined structures ────────────────────────────────────────────────

    /** Маленькая башня 3×7×3 из stoneBrick. */
    public static Structure smallTower() {
        Structure s = new Structure("small_tower");
        // Основание
        s.layer(0, 0, 0, 2, 2, 4); // stoneBrick id=4
        // Стены
        for (int y = 1; y <= 6; y++) {
            s.set(0, y, 0, 4);
            s.set(2, y, 0, 4);
            s.set(0, y, 2, 4);
            s.set(2, y, 2, 4);
        }
        // Крыша
        s.layer(7, 0, 0, 2, 2, 4);
        return s;
    }

    /** Маленький домик из wood. */
    public static Structure smallHut() {
        Structure s = new Structure("small_hut");
        int wood = 5;   // Tile.wood.id
        int dirt = 3;
        // Пол
        s.layer(0, 0, 0, 3, 3, dirt);
        // Стены
        for (int y = 1; y <= 3; y++) {
            for (int x = 0; x <= 3; x++) {
                s.set(x, y, 0, wood);
                s.set(x, y, 3, wood);
            }
            s.set(0, y, 1, wood);
            s.set(0, y, 2, wood);
            s.set(3, y, 1, wood);
            s.set(3, y, 2, wood);
        }
        // Крыша
        s.layer(4, 0, 0, 3, 3, wood);
        return s;
    }
}
