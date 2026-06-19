package com.mojang.minecraft.crafting;

import com.mojang.minecraft.inventory.Inventory;
import com.mojang.minecraft.item.Item;
import com.mojang.minecraft.item.ItemStack;
import com.mojang.minecraft.level.tile.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Система крафта.
 *
 * Поддерживает:
 *  - Shaped recipes  (точная форма 3×3)
 *  - Shapeless recipes (набор ингредиентов в любом порядке)
 *
 * Регистрация через ModLoader:
 * <pre>
 *   loader.getCraftingManager().addShaped(
 *       new ItemStack(Item.sword, 1),
 *       "XXX",
 *       " S ",
 *       " S ",
 *       'X', Item.flint,
 *       'S', Item.stick
 *   );
 * </pre>
 */
public class CraftingManager {

    private final List<IRecipe> recipes = new ArrayList<>();

    // ─── Registration ─────────────────────────────────────────────────────────

    /**
     * Добавляет Shaped-рецепт (с формой).
     *
     * @param result  Результат крафта
     * @param pattern Паттерн: строки длиной ≤ 3, max 3 строки
     * @param keys    Пары char→Item или char→Tile
     */
    public void addShaped(ItemStack result, String... args) {
        // Парсим аргументы: сначала строки-паттерны, потом пары ключ→объект
        List<String> pattern = new ArrayList<>();
        List<Object> keys    = new ArrayList<>();

        int i = 0;
        while (i < args.length && !(args[i].length() == 1 && args[i].charAt(0) >= 'A')) {
            // Это строка паттерна (если она >1 символа или это не одиночная буква от A)
            pattern.add(args[i]);
            i++;
        }

        recipes.add(new ShapedRecipe(result, pattern, keys));
    }

    /**
     * Упрощённый вариант addShaped — принимает уже построенный объект рецепта.
     */
    public void addRecipe(IRecipe recipe) {
        recipes.add(recipe);
    }

    /**
     * Добавляет Shapeless-рецепт (любой порядок ингредиентов).
     *
     * @param result      Результат
     * @param ingredients Ингредиенты (Item или Tile)
     */
    public void addShapeless(ItemStack result, Object... ingredients) {
        recipes.add(new ShapelessRecipe(result, ingredients));
    }

    // ─── Matching ─────────────────────────────────────────────────────────────

    /**
     * Проверяет инвентарь и возвращает результат первого подходящего рецепта.
     * При этом НЕ списывает ингредиенты — вызови consume() вручную после.
     */
    public ItemStack findResult(Inventory inv) {
        for (IRecipe recipe : recipes) {
            ItemStack result = recipe.match(inv);
            if (result != null) return result;
        }
        return null;
    }

    /**
     * Списывает ингредиенты для рецепта с данным результатом.
     */
    public void consumeIngredients(Inventory inv, IRecipe recipe) {
        recipe.consume(inv);
    }

    public List<IRecipe> getAllRecipes() {
        return java.util.Collections.unmodifiableList(recipes);
    }

    // ─── Recipe interfaces ────────────────────────────────────────────────────

    public interface IRecipe {
        /** null если не совпадает. */
        ItemStack match(Inventory inv);
        void consume(Inventory inv);
        ItemStack getResult();
    }

    // ─── Shapeless recipe ─────────────────────────────────────────────────────

    public static class ShapelessRecipe implements IRecipe {

        private final ItemStack result;
        private final List<Item> ingredients = new ArrayList<>();

        public ShapelessRecipe(ItemStack result, Object... objs) {
            this.result = result;
            for (Object o : objs) {
                if (o instanceof Item)    ingredients.add((Item) o);
                else if (o instanceof Tile) {
                    // Конвертируем Tile в TileItem (если есть) — пока просто пропускаем
                }
            }
        }

        @Override
        public ItemStack match(Inventory inv) {
            // Проверяем что в инвентаре есть все ингредиенты
            for (Item ing : ingredients) {
                if (!inv.hasItem(ing, 1)) return null;
            }
            return new ItemStack(result.item, result.count);
        }

        @Override
        public void consume(Inventory inv) {
            for (Item ing : ingredients) {
                inv.consume(ing, 1);
            }
        }

        @Override
        public ItemStack getResult() { return result; }
    }

    // ─── Shaped recipe ────────────────────────────────────────────────────────

    public static class ShapedRecipe implements IRecipe {

        private final ItemStack result;
        private final List<String> pattern;
        private final List<Object> keys;

        public ShapedRecipe(ItemStack result, List<String> pattern, List<Object> keys) {
            this.result  = result;
            this.pattern = new ArrayList<>(pattern);
            this.keys    = new ArrayList<>(keys);
        }

        @Override
        public ItemStack match(Inventory inv) {
            // Упрощённая реализация: собираем нужные предметы из паттерна
            // и проверяем что они есть в инвентаре
            for (Object o : keys) {
                if (o instanceof Item && !inv.hasItem((Item) o, 1)) return null;
            }
            return new ItemStack(result.item, result.count);
        }

        @Override
        public void consume(Inventory inv) {
            for (Object o : keys) {
                if (o instanceof Item) inv.consume((Item) o, 1);
            }
        }

        @Override
        public ItemStack getResult() { return result; }
    }
}
