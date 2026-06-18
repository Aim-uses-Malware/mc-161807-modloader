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
        // Проверяем, зажат ли левый Ctrl
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            // Если игрок движется (система приложила motion от нажатия W/A/S/D), 
            // мы даем ему небольшой пинок вперед по вектору его движения, 
            // но ограничиваем максимальный разгон, чтобы не улететь в космос.
            
            // Проверяем, что скорость не превышает разумный предел для спринта
            double currentSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
            
            if (currentSpeed > 0.01 && currentSpeed < 0.3) {
                // Аккуратно подталкиваем в сторону текущего движения
                player.motionX *= 1.08F;
                player.motionZ *= 1.08F;
            }
        }
    }
