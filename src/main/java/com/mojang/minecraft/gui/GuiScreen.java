package com.mojang.minecraft.gui;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;

public class GuiScreen {
    protected int width;
    protected int height;

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void render() {
        // Задний полупрозрачный фон (затемнение игры при паузе)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.0F, 0.0F, 0.0F, 0.5F); // Черный с 50% прозрачностью
        
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(width, 0);
        glVertex2f(width, height);
        glVertex2f(0, height);
        glEnd();
        glDisable(GL_BLEND);
    }

    public void handleInput() {
        while (Mouse.next()) {
            if (Mouse.getEventButtonState()) {
                int mouseX = Mouse.getEventX() * this.width / Display.getWidth();
                int mouseY = this.height - Mouse.getEventY() * this.height / Display.getHeight() - 1;
                mouseClicked(mouseX, mouseY, Mouse.getEventButton());
            }
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) {}
    
    public void onMenuClose() {}
}
