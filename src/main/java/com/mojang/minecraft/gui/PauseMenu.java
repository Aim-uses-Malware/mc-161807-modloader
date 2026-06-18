package com.mojang.minecraft.gui;

import com.mojang.minecraft.Minecraft;
import org.lwjgl.opengl.Display;

public class PauseMenu extends GuiScreen {
    private final Minecraft mc;

    public PauseMenu(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public void render() {
        super.render(); // Рендерим затемнение фона

        // Тут должен быть твой FontRenderer, если он есть в RubyDung.
        // Если готового вывода текста на экран нет, можно использовать простую отрисовку квадратов-кнопок.
        // Давай нарисуем рамки кнопок через OpenGL для наглядности:
        
        drawButton(width / 2 - 100, height / 2 - 40, 200, 20, "Back to Game");
        drawButton(width / 2 - 100, height / 2 - 10, 200, 20, "Options...");
        drawButton(width / 2 - 100, height / 2 + 20, 200, 20, "Quit Game");
    }

    private void drawButton(int x, int y, int w, int h, String text) {
        // Простейшая отрисовка кнопки (серый квадрат)
        org.lwjgl.opengl.GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
        org.lwjgl.opengl.GL11.glVertex2f(x, y);
        org.lwjgl.opengl.GL11.glVertex2f(x + w, y);
        org.lwjgl.opengl.GL11.glVertex2f(x + w, y + h);
        org.lwjgl.opengl.GL11.glVertex2f(x, y + h);
        org.lwjgl.opengl.GL11.glEnd();
        
        // Знаю, что у тебя в этой версии Майна вывод текста идет через текстуру шрифта default.png.
        // Если у тебя есть класс Font.java или FontRenderer.java, вызови его тут:
        // mc.font.draw(text, x + 10, y + 5, 0xFFFFFF);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) { // Левый клик мыши
            // Кнопка "Back to Game"
            if (mouseX >= width / 2 - 100 && mouseX <= width / 2 + 100 && mouseY >= height / 2 - 40 && mouseY <= height / 2 - 20) {
                mc.currentScreen = null; // Закрываем меню
                org.lwjgl.input.Mouse.setGrabbed(true); // Возвращаем прицел мыши в игру
            }
            
            // Кнопка "Quit Game"
            if (mouseX >= width / 2 - 100 && mouseX <= width / 2 + 100 && mouseY >= height / 2 + 20 && mouseY <= height / 2 + 40) {
                System.exit(0);
            }
        }
    }
}
