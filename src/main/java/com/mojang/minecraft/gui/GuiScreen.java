package com.mojang.minecraft.modloader.gui;

import com.mojang.minecraft.level.Tessellator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import static org.lwjgl.opengl.GL11.*;

/**
 * Base class for all ModLoader GUI screens.
 *
 * Subclasses override:
 *   init()          — called once when screen opens, layout buttons here
 *   drawScreen()    — called every frame, draw custom content here
 *   mouseClicked()  — left-click event
 *   keyPressed()    — key press event
 *   onClose()       — cleanup when closing
 */
public abstract class GuiScreen {

    protected int screenWidth;
    protected int screenHeight;

    /** Tessellator shared across the screen, available after init(). */
    protected Tessellator tessellator;

    /** Current mouse position in screen-space. Updated each frame. */
    protected int mouseX;
    protected int mouseY;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public final void open(int width, int height) {
        this.screenWidth  = width;
        this.screenHeight = height;
        this.tessellator  = new Tessellator();
        init();
    }

    /** Called by Minecraft each frame while this screen is active. */
    public final void render(float partialTicks) {
        // Consume mouse position
        mouseX = Mouse.getX() * screenWidth  / getDisplayWidth();
        // LWJGL Y is bottom-up; flip
        mouseY = screenHeight - Mouse.getY() * screenHeight / getDisplayHeight();

        // Dim background
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GuiRenderer.fillRect(tessellator, 0, 0, screenWidth, screenHeight, GuiRenderer.COLOR_BG);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);

        drawScreen(partialTicks);
    }

    /** Poll input — call from Minecraft's tick loop while screen is open. */
    public final void pollInput() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();
                if (key == Keyboard.KEY_ESCAPE) {
                    onClose();
                    GuiManager.closeScreen();
                    return;
                }
                keyPressed(key, Keyboard.getEventCharacter());
            }
        }
        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                mouseClicked(mouseX, mouseY);
            }
            if (Mouse.getEventButton() == -1) {
                int scroll = Mouse.getEventDWheel();
                if (scroll != 0) mouseScrolled(scroll);
            }
        }
    }

    // ─── Overridable ──────────────────────────────────────────────────────────

    protected void init() {}
    protected abstract void drawScreen(float partialTicks);
    protected void mouseClicked(int mx, int my) {}
    protected void keyPressed(int key, char c) {}
    protected void mouseScrolled(int delta) {}
    protected void onClose() {}

    // ─── Helpers ──────────────────────────────────────────────────────────────

    protected void drawTitle(String title) {
        GuiRenderer.drawString(tessellator, title,
                (screenWidth - GuiRenderer.stringWidth(title, 2f)) / 2,
                10, GuiRenderer.COLOR_YELLOW, 2f);
    }

    protected void drawCenteredString(String text, int y, int color) {
        GuiRenderer.drawString(tessellator, text,
                (screenWidth - GuiRenderer.stringWidth(text, 1f)) / 2,
                y, color, 1f);
    }

    /** Translate LWJGL display size — approximate with screen size if unavailable. */
    private int getDisplayWidth()  { return Math.max(1, screenWidth); }
    private int getDisplayHeight() { return Math.max(1, screenHeight); }
}
