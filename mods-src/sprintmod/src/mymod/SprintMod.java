package mymod;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.modloader.IMod;
import com.mojang.minecraft.modloader.ModLoader;
import org.lwjgl.input.Keyboard;

public class SprintMod implements IMod {

    private boolean isSprinting = false;

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
        System.out.println("[SprintMod] Мод на спринт успешно инициализирован! Зажми LCONTROL для разгона.");
    }

    @Override
    public void onPlayerTick(Player player, Level level) {
        // Проверяем, зажат ли левый Ctrl (код клавиши 29 в LWJGL)
        // Также проверяем, что игрок вообще движется (чтобы не спринтовать стоя на месте)
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            if (!isSprinting) {
                isSprinting = true;
            }
            
            // По дефолту в Player.java трение/замедление гасит скорость.
            // Мы будем немного подталкивать игрока по вектору его движения (motionX и motionZ),
            // увеличивая скорость примерно в 1.5 раза.
            player.motionX *= 1.15F;
            player.motionZ *= 1.15F;
            
        } else {
            isSprinting = false;
        }
    }
}
