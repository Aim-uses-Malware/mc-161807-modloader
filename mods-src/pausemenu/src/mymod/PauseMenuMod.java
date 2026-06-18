package mymod;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.modloader.IMod;
import com.mojang.minecraft.modloader.ModLoader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;

public class PauseMenuMod implements IMod {

    private boolean isPaused = false;

    @Override
    public String getModId() {
        return "pausemenumod";
    }

    @Override
    public String getName() {
        return "Pause Menu Mod";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad(ModLoader loader) {
        System.out.println("[PauseMenuMod] Мод на меню паузы успешно загружен! Нажми ESC для паузы.");
    }

    @Override
    public void onPlayerTick(Player player, Level level) {
        // Ловим нажатие ESC
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            // Чтобы кнопка не спамила при удержании, переключаем триггер
            while (Keyboard.next()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE && Keyboard.getEventKeyState()) {
                    isPaused = !isPaused;
                    
                    // Если встали на паузу — отпускаем мышь, иначе — забираем обратно в игру
                    Mouse.setGrabbed(!isPaused);
                }
            }
        }

        // Если игра на паузе — намертво стопим движение игрока
        if (isPaused) {
            player.motionX = 0;
            player.motionY = 0;
            player.motionZ = 0;

            // Рендерим менюшку прямо в тике игрока поверх экрана
            renderMenu();
            
            // Обрабатываем клики мышки по кнопкам меню
            handleMenuClicks();
        }
    }

    private void renderMenu() {
        int screenWidth = Display.getWidth() * 240 / Display.getHeight();
        int screenHeight = 240;

        // Переходим в 2D режим отрисовки поверх игры
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, screenWidth, screenHeight, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // 1. Рисуем темный задний фон на весь экран
        glColor4f(0.0F, 0.0F, 0.0F, 0.6F); // 60% затемнение
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(screenWidth, 0);
        glVertex2f(screenWidth, screenHeight);
        glVertex2f(0, screenHeight);
        glEnd();

        // Считаем координаты мыши для подсветки кнопок
        int mouseX = Mouse.getX() * screenWidth / Display.getWidth();
        int mouseY = screenHeight - Mouse.getY() * screenHeight / Display.getHeight() - 1;

        // 2. Рисуем кнопку "Back to Game" (по центру экрана)
        drawButton(screenWidth / 2 - 100, screenHeight / 2 - 20, 200, 20, mouseX, mouseY);

        // 3. Рисуем кнопку "Quit Game"
        drawButton(screenWidth / 2 - 100, screenHeight / 2 + 15, 200, 20, mouseX, mouseY);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);

        // Возвращаем матрицы OpenGL в исходное состояние для игры
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private void drawButton(int x, int y, int w, int h, int mouseX, int mouseY) {
        // Если навели курсор — кнопка светлеет
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            glColor4f(0.6F, 0.6F, 0.6F, 1.0F); // Светло-серый
        } else {
            glColor4f(0.3F, 0.3F, 0.3F, 1.0F); // Темно-серый
        }

        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
    }

    private void handleMenuClicks() {
        int screenWidth = Display.getWidth() * 240 / Display.getHeight();
        int screenHeight = 240;

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) { // Левый клик
                int mouseX = Mouse.getEventX() * screenWidth / Display.getWidth();
                int mouseY = screenHeight - Mouse.getEventY() * screenHeight / Display.getHeight() - 1;

                // Клик по "Back to Game"
                if (mouseX >= screenWidth / 2 - 100 && mouseX <= screenWidth / 2 + 100 && mouseY >= screenHeight / 2 - 20 && mouseY <= screenHeight / 2) {
                    isPaused = false;
                    Mouse.setGrabbed(true); // Возвращаем камеру в игру
                }

                // Клик по "Quit Game"
                if (mouseX >= screenWidth / 2 - 100 && mouseX <= screenWidth / 2 + 100 && mouseY >= screenHeight / 2 + 15 && mouseY <= screenHeight / 2 + 35) {
                    System.exit(0);
                }
            }
        }
    }
}
