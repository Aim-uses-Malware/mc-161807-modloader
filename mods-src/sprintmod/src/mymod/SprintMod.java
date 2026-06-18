package mymod;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.modloader.IMod;
import com.mojang.minecraft.modloader.ModLoader;
import org.lwjgl.input.Keyboard;

public class SprintMod implements IMod {

    @Override
    public String getModId() {
        return "sprintmod";
    }

    @Override
    public String getName() {
        return "Sprint Mod";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad(ModLoader loader) {
        System.out.println("[SprintMod] Мод загружен! Зажми LCONTROL для бега.");
    }

    // Если твой ModLoader использует общий onTick вместо onPlayerTick,
    // то надо переписать под сигнатуру твоего ModLoader'а. 
    // Давай оставим пока onPlayerTick, но добавим жесткий буст к координатам,
    // если умножение motionX гасится фрикцией игры.
    @Override
    public void onPlayerTick(Player player, Level level) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            // Проверяем, что игрок вообще идет (нажал W/A/S/D)
            if (Math.abs(player.motionX) > 0.001 || Math.abs(player.motionZ) > 0.001) {
                // Вместо слабого умножения, давай принудительно подталкивать
                player.motionX *= 1.3F; 
                player.motionZ *= 1.3F;
            }
        }
    }
}
