package com.mojang.minecraft.inventory;

import com.mojang.minecraft.item.Item;
import com.mojang.minecraft.item.ItemStack;

/**
 * Инвентарь игрока: 9 слотов хотбара + 27 основных = 36 всего.
 * Слоты 0-8 — хотбар (hotbar), 9-35 — основной инвентарь.
 *
 * Интегрируется в Player через Player.inventory.
 */
public class Inventory {

    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE   = 27;
    public static final int TOTAL_SIZE  = HOTBAR_SIZE + MAIN_SIZE;

    /** Выбранный слот хотбара (0-8). */
    public int selectedSlot = 0;

    private final ItemStack[] slots = new ItemStack[TOTAL_SIZE];

    // ─── Hotbar ────────────────────────────────────────────────────────────────

    public ItemStack getHeldItem() {
        return slots[selectedSlot];
    }

    public void selectSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) this.selectedSlot = slot;
    }

    // ─── Basic operations ─────────────────────────────────────────────────────

    public ItemStack getSlot(int index) {
        return slots[index];
    }

    public void setSlot(int index, ItemStack stack) {
        slots[index] = (stack == null || stack.isEmpty()) ? null : stack;
    }

    /**
     * Добавляет предмет в инвентарь, пытаясь догрузить уже существующие стаки.
     *
     * @param stack Стак для добавления
     * @return Остаток (не вошедшие), или null если всё поместилось
     */
    public ItemStack addItem(ItemStack stack) {
        // Сначала пробуем дополнить существующие стаки
        for (int i = 0; i < TOTAL_SIZE && !stack.isEmpty(); i++) {
            ItemStack existing = slots[i];
            if (existing != null && existing.canMergeWith(stack)) {
                int space = existing.item.maxStackSize - existing.count;
                int transfer = Math.min(space, stack.count);
                existing.count += transfer;
                stack.count    -= transfer;
            }
        }
        // Затем — в пустые слоты
        for (int i = 0; i < TOTAL_SIZE && !stack.isEmpty(); i++) {
            if (slots[i] == null) {
                slots[i] = stack.split(Math.min(stack.count, stack.item.maxStackSize));
            }
        }
        return stack.isEmpty() ? null : stack;
    }

    /**
     * Удаляет указанное количество предмета из инвентаря.
     *
     * @param item  Тип предмета
     * @param count Количество
     * @return true если успешно списано
     */
    public boolean consume(Item item, int count) {
        int remaining = count;
        for (int i = 0; i < TOTAL_SIZE && remaining > 0; i++) {
            ItemStack s = slots[i];
            if (s != null && s.item == item) {
                int take = Math.min(s.count, remaining);
                s.count   -= take;
                remaining -= take;
                if (s.isEmpty()) slots[i] = null;
            }
        }
        return remaining == 0;
    }

    /**
     * Считает общее количество предмета в инвентаре.
     */
    public int countOf(Item item) {
        int total = 0;
        for (ItemStack s : slots) {
            if (s != null && s.item == item) total += s.count;
        }
        return total;
    }

    public boolean hasItem(Item item, int count) {
        return countOf(item) >= count;
    }
}
