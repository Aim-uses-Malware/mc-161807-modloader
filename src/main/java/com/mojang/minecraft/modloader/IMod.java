package com.mojang.minecraft.modloader;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.tile.Tile;

/**
 * Главный интерфейс мода.
 *
 * Реализуй этот интерфейс в своём классе и упакуй в .jar → ModLoader найдёт автоматически.
 *
 * Минимальная реализация:
 * <pre>
 *   public class MyMod implements IMod {
 *       public String getModId()   { return "mymod"; }
 *       public String getName()    { return "My Mod"; }
 *       public String getVersion() { return "1.0.0"; }
 *       public void onLoad(ModLoader loader) {
 *           System.out.println("Hello from MyMod!");
 *       }
 *   }
 * </pre>
 *
 * Дополнительные поля через ModMetadata (необязательно):
 * <pre>
 *   public ModMetadata getMetadata() {
 *       return new ModMetadata(getModId(), getName(), getVersion(),
 *           "Описание", "Автор", "https://example.com");
 *   }
 * </pre>
 */
public interface IMod {

    // ─── Обязательные метаданные ───────────────────────────────────────────────

    String getModId();
    String getName();
    String getVersion();

    /**
     * Вызывается ОДИН РАЗ при загрузке мода.
     * Здесь регистрируй блоки, предметы, рецепты, звуки, структуры.
     */
    void onLoad(ModLoader loader);

    // ─── Игровые хуки (опциональные) ──────────────────────────────────────────

    /** Каждый игровой тик (20/сек). */
    default void onTick(Level level) {}

    /** Блок уничтожен. */
    default void onTileDestroy(Tile tile, Level level, int x, int y, int z) {}

    /** Блок поставлен. */
    default void onTilePlace(Tile tile, Level level, int x, int y, int z) {}

    /** Мир сгенерирован. */
    default void onLevelGenerated(Level level) {}

    /** Случайный тик блока. */
    default void onTileRandomTick(Level level, int x, int y, int z, Tile tile) {}

    /** Игрок прыгнул. */
    default void onPlayerJump(Player player) {}

    /** Зомби заспавнился. */
    default void onEntitySpawn(Zombie zombie, Level level) {}

    /** Клавиша нажата. */
    default void onKeyPress(int key) {}

    /** Тик зомби. */
    default void onEntityTick(Zombie zombie, Level level) {}

    /** Зомби удалён. */
    default void onEntityRemove(Zombie zombie, Level level) {}

    /** Тик игрока. */
    default void onPlayerTick(Player player, Level level) {}

    // ─── Расширенные хуки v2 ───────────────────────────────────────────────────

    /**
     * Хук рендера — вызывается ПЕРЕД рендером мира.
     * Можно добавлять кастомные GL-вызовы.
     *
     * @param partialTicks Остаток тика для интерполяции
     */
    default void onRenderPre(float partialTicks) {}

    /**
     * Хук рендера — вызывается ПОСЛЕ рендера мира, перед HUD.
     */
    default void onRenderPost(float partialTicks) {}

    /**
     * Хук HUD — вызывается при рисовании интерфейса (ortho-камера).
     *
     * @param screenWidth  Ширина экрана в HUD-единицах
     * @param screenHeight Высота экрана в HUD-единицах
     */
    default void onRenderHud(int screenWidth, int screenHeight, float partialTicks) {}

    /**
     * Мир загружен из level.dat.
     */
    default void onLevelLoaded(Level level) {}

    /**
     * Мир сохраняется. Вернуть false — отменить сохранение.
     */
    default boolean onLevelSave(Level level) { return true; }

    /**
     * Расширенная метаинформация о моде (необязательно).
     * Переопредели для установки description, author, website.
     */
    default com.mojang.minecraft.modloader.ModMetadata getMetadata() {
        return new com.mojang.minecraft.modloader.ModMetadata(getModId(), getName(), getVersion());
    }
}
