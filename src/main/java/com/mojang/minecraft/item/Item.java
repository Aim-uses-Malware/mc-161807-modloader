package com.mojang.minecraft.item;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.level.Level;

/**
 * Базовый класс предмета (Item).
 *
 * Items живут в Inventory, могут быть заданы модом через ModLoader.registerItem().
 * В отличие от Tile, Item — это то, что игрок держит в руке или хранит в инвентаре,
 * но не размещает в мире (хотя можно добавить логику placeable → Tile).
 */
public class Item {

    /** Глобальный реестр всех предметов. ID 0 зарезервирован. */
    public static final Item[] items = new Item[4096];

    // Стандартные предметы (id 256+ чтобы не пересекаться с Tile 0-255)
    public static final Item stick   = new Item(256, "Stick");
    public static final Item flint   = new Item(257, "Flint");
    public static final Item sword   = new Item(258, "Sword")  { @Override public boolean isTool(){ return true; } };
    public static final Item pickaxe = new Item(259, "Pickaxe"){ @Override public boolean isTool(){ return true; } };

    public final int    id;
    public final String name;

    /** Максимальное количество в стаке. */
    public int maxStackSize = 64;

    /** Прочность инструмента (0 = нет прочности). */
    public int maxDurability = 0;

    public Item(int id, String name) {
        if (id <= 0 || id >= items.length)
            throw new IllegalArgumentException("Item id out of range: " + id);
        if (items[id] != null)
            throw new IllegalArgumentException("Item id " + id + " already occupied by " + items[id].name);
        this.id   = id;
        this.name = name;
        items[id] = this;
    }

    // ─── Callbacks ────────────────────────────────────────────────────────────

    /**
     * Вызывается при использовании предмета (ПКМ/Использовать).
     *
     * @param stack  Стак предмета в руке
     * @param player Игрок
     * @param level  Мир
     * @return true — предмет «потреблён» (уменьшить количество)
     */
    public boolean onUse(ItemStack stack, Player player, Level level) {
        return false;
    }

    /**
     * Вызывается каждый тик пока предмет в руке.
     */
    public void onHeld(ItemStack stack, Player player, Level level) {}

    /**
     * Вызывается при уничтожении блока этим предметом.
     * По умолчанию уменьшает прочность.
     */
    public void onBlockBroken(ItemStack stack) {
        if (maxDurability > 0) {
            stack.damage++;
            if (stack.damage >= maxDurability) {
                stack.count = 0; // Сломан
            }
        }
    }

    public boolean isTool()  { return false; }
    public boolean isFood()  { return false; }
    public boolean isArmor() { return false; }

    @Override
    public String toString() {
        return name + "#" + id;
    }
}
