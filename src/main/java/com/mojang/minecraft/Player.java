package com.mojang.minecraft;

import com.mojang.minecraft.inventory.Inventory;
import com.mojang.minecraft.item.ItemStack;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.modloader.ModLoader;
import org.lwjgl.input.Keyboard;

public class Player extends Entity {

    /** Инвентарь игрока: хотбар + основная сетка. */
    public final Inventory inventory = new Inventory();

    /** Здоровье (0–20). */
    public int health    = 20;
    public int maxHealth = 20;

    /** Спринт. */
    public boolean isSprinting = false;

    /** Кратность прыжка (для двойного прыжка через мод). */
    public int jumpsLeft = 1;

    public Player(Level level) {
        super(level);
        this.heightOffset = 1.62f;
    }

    @Override
    public void onTick() {
        super.onTick();

        float forward  = 0.0F;
        float vertical = 0.0F;

        // Сброс позиции
        if (Keyboard.isKeyDown(19)) resetPosition(); // R

        // Движение
        if (Keyboard.isKeyDown(200) || Keyboard.isKeyDown(17)) forward--;   // W / Up
        if (Keyboard.isKeyDown(208) || Keyboard.isKeyDown(31)) forward++;   // S / Down
        if (Keyboard.isKeyDown(203) || Keyboard.isKeyDown(30)) vertical--;  // A / Left
        if (Keyboard.isKeyDown(205) || Keyboard.isKeyDown(32)) vertical++;  // D / Right

        // Спринт (Ctrl / двойной W упрощён до Ctrl)
        isSprinting = Keyboard.isKeyDown(29); // Left Ctrl

        // Прыжок
        if ((Keyboard.isKeyDown(57) || Keyboard.isKeyDown(219)) && (this.onGround || jumpsLeft > 0)) {
            this.motionY = 0.5F;
            if (!this.onGround) jumpsLeft--;
            ModLoader.getInstance().dispatchPlayerJump(this);
        }
        if (this.onGround) jumpsLeft = 1;

        // Смерть (падение в пустоту)
        if (this.y < -100.0F) {
            health = 0;
        }

        // Скорость со спринтом
        float speed = this.onGround ? (isSprinting ? 0.16F : 0.1F) : 0.02F;
        moveRelative(vertical, forward, speed);

        // Гравитация
        this.motionY -= 0.08D;

        move(this.motionX, this.motionY, this.motionZ);

        this.motionX *= 0.91F;
        this.motionY *= 0.98F;
        this.motionZ *= 0.91F;

        if (this.onGround) {
            this.motionX *= 0.7F;
            this.motionZ *= 0.7F;
        }
    }

    // ─── Инвентарь helpers ────────────────────────────────────────────────────

    /** Текущий предмет в руке (или null). */
    public ItemStack getHeldItem() {
        return inventory.getHeldItem();
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public void damage(int amount) {
        health = Math.max(0, health - amount);
    }
}
