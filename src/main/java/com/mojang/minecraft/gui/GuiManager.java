package com.mojang.minecraft.modloader.gui;

/**
 * Manages the currently open GUI screen.
 *
 * Minecraft calls GuiManager.render() and GuiManager.pollInput() each frame/tick.
 * Opening a screen pauses the game (mouse released, input rerouted here).
 */
public class GuiManager {

    private static GuiScreen activeScreen = null;
    private static Runnable  mouseUngrabCallback  = null;
    private static Runnable  mouseRegrabCallback  = null;

    /**
     * Register callbacks so GuiManager can release/grab the mouse when opening/closing screens.
     * Call once during Minecraft.init().
     */
    public static void registerCallbacks(Runnable ungrab, Runnable regrab) {
        mouseUngrabCallback = ungrab;
        mouseRegrabCallback = regrab;
    }

    /** Open a new screen. Releases mouse. */
    public static void openScreen(GuiScreen screen, int width, int height) {
        activeScreen = screen;
        screen.open(width, height);
        if (mouseUngrabCallback != null) mouseUngrabCallback.run();
    }

    /** Close active screen. Regrabs mouse. */
    public static void closeScreen() {
        activeScreen = null;
        if (mouseRegrabCallback != null) mouseRegrabCallback.run();
    }

    public static boolean isOpen() {
        return activeScreen != null;
    }

    public static GuiScreen getActiveScreen() {
        return activeScreen;
    }

    /** Called each render frame. */
    public static void render(float partialTicks) {
        if (activeScreen != null) activeScreen.render(partialTicks);
    }

    /** Called each tick for input polling. */
    public static void pollInput() {
        if (activeScreen != null) activeScreen.pollInput();
    }
}
