package mymod;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.modloader.IMod;
import com.mojang.minecraft.modloader.ModLoader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Готовый темплейт для создания новых модификаций.
 * Замени "TemplateMod" на название своего мода везде по тексту.
 */
public class TemplateMod implements IMod {

    // Уникальный ID твоего мода (используется для логов и конфигов)
    @Override
    public String getModId() {
        return "templatemod";
    }

    // Понятное название, которое будет видно в консоли
    @Override
    public String getName() {
        return "Template Blank Mod";
    }

    // Версия мода
    @Override
    public String getVersion() {
        return "1.0.0";
    }

    /**
     * Этот метод вызывается ОДИН РАЗ, когда ModLoader загружает твой .jar файл.
     * Здесь можно регистрировать блоки, текстуры или просто выводить лог в консоль.
     */
    @Override
    public void onLoad(ModLoader loader) {
        System.out.println("[" + getName() + "] Успешно инициализирован!");
    }

    /**
     * Этот метод вызывается КАЖДЫЙ ИГРОВОЙ ТИК (20 раз в секунду).
     * Сюда пишется вся основная логика: обработка кнопок, физика игрока, хуки.
     * 
     * @param player Объект игрока (можно менять скорость, позицию и т.д.)
     * @param level  Объект текущего мира игры
     */
    @Override
    public void onPlayerTick(Player player, Level level) {
        // ПРИМЕР ОБРАБОТКИ КЛАВИАТУРЫ:
        // if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
        //     // Твой код, если зажата клавиша F
        // }

        // ПРИМЕР ОБРАБОТКИ МЫШКИ:
        // if (Mouse.isButtonDown(0)) {
        //     // Твой код, если зажат левый клик
        // }
    }
}
