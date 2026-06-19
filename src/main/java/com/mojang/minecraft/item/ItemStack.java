package com.mojang.minecraft.item;

/**
 * Стак предметов — Item + количество + урон/прочность.
 * Используется в Inventory.
 */
public class ItemStack {

    public final Item item;
    public int count;
    public int damage;

    public ItemStack(Item item, int count) {
        this.item  = item;
        this.count = count;
        this.damage = 0;
    }

    public ItemStack(Item item) {
        this(item, 1);
    }

    /** Возвращает true если стак пуст (count <= 0). */
    public boolean isEmpty() {
        return count <= 0;
    }

    /** Разделяет стак — берёт amt предметов, уменьшает текущий. */
    public ItemStack split(int amt) {
        int take = Math.min(amt, count);
        count -= take;
        return new ItemStack(item, take);
    }

    /** Могут ли эти два стака быть объединены? */
    public boolean canMergeWith(ItemStack other) {
        return other.item == this.item
            && other.damage == this.damage
            && item.maxStackSize > 1;
    }

    @Override
    public String toString() {
        return item.name + " x" + count + (damage > 0 ? " [dmg:" + damage + "]" : "");
    }
}
