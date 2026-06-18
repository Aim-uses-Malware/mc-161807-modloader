package com.mojang.minecraft.modloader.gui;

import com.mojang.minecraft.Textures;
import com.mojang.minecraft.level.Tessellator;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

/**
 * Stateless 2D rendering helpers used by all ModLoader GUI screens.
 * Operates in screen-space (pixels, origin top-left).
 */
public class GuiRenderer {

    // ─── Colors ───────────────────────────────────────────────────────────────
    public static final int COLOR_BG        = 0xC0101010;
    public static final int COLOR_PANEL     = 0xFF1E1E1E;
    public static final int COLOR_BTN       = 0xFF555555;
    public static final int COLOR_BTN_HOVER = 0xFF777777;
    public static final int COLOR_BTN_DIS   = 0xFF333333;
    public static final int COLOR_WHITE     = 0xFFFFFFFF;
    public static final int COLOR_YELLOW    = 0xFFFFFF55;
    public static final int COLOR_RED       = 0xFFFF5555;
    public static final int COLOR_GREEN     = 0xFF55FF55;
    public static final int COLOR_GRAY      = 0xFFAAAAAA;
    public static final int COLOR_DARK_GRAY = 0xFF666666;

    // ─── Filled rectangle ─────────────────────────────────────────────────────

    /**
     * Draw a filled rectangle.
     *
     * @param x      Left edge (pixels)
     * @param y      Top edge (pixels)
     * @param w      Width
     * @param h      Height
     * @param color  ARGB packed color
     */
    public static void fillRect(Tessellator t, int x, int y, int w, int h, int color) {
        glDisable(GL_TEXTURE_2D);
        setColor(color);
        t.init();
        t.vertex(x,     y,     0);
        t.vertex(x,     y + h, 0);
        t.vertex(x + w, y + h, 0);
        t.vertex(x + w, y,     0);
        t.flush();
        glEnable(GL_TEXTURE_2D);
        glColor4f(1,1,1,1);
    }

    /**
     * Draw a rectangle outline (1 pixel border).
     */
    public static void drawRect(Tessellator t, int x, int y, int w, int h, int color) {
        fillRect(t, x,         y,         w, 1, color); // top
        fillRect(t, x,         y + h - 1, w, 1, color); // bottom
        fillRect(t, x,         y,         1, h, color); // left
        fillRect(t, x + w - 1, y,         1, h, color); // right
    }

    /** Gradient rectangle, top→bottom. */
    public static void fillGradient(int x, int y, int w, int h, int colorTop, int colorBottom) {
        glDisable(GL_TEXTURE_2D);
        glBegin(GL_QUADS);
        setColorGL(colorTop);
        glVertex2f(x,     y);
        glVertex2f(x + w, y);
        setColorGL(colorBottom);
        glVertex2f(x + w, y + h);
        glVertex2f(x,     y + h);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        glColor4f(1,1,1,1);
    }

    // ─── Bitmap font rendering ─────────────────────────────────────────────────

    /**
     * Render a string using Minecraft's char.png atlas (128×128, 16×16 glyph grid).
     * Each glyph is 8×8 pixels drawn at the given scale.
     *
     * @param text   Text to render
     * @param x      Screen X
     * @param y      Screen Y
     * @param color  ARGB color
     * @param scale  Pixel scale (1 = 8px tall, 2 = 16px, etc.)
     */
    public static void drawString(Tessellator t, String text, int x, int y, int color, float scale) {
        int texId = Textures.loadTexture("/char.png", GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, texId);
        glEnable(GL_TEXTURE_2D);

        // Shadow pass
        setColor(applyAlpha(color, 0x44));
        renderGlyphs(t, text, x + 1, y + 1, scale);

        // Main pass
        setColor(color);
        renderGlyphs(t, text, x, y, scale);

        glColor4f(1,1,1,1);
    }

    private static void renderGlyphs(Tessellator t, String text, int x, int y, float scale) {
        t.init();
        int cx = x;
        for (char c : text.toCharArray()) {
            int idx  = c & 0xFF;
            float u0 = (idx % 16) / 16.0f;
            float v0 = (idx / 16) / 16.0f;
            float u1 = u0 + 1.0f / 16.0f;
            float v1 = v0 + 1.0f / 16.0f;
            float s  = 8 * scale;
            t.vertexUV(cx,     y,     0, u0, v0);
            t.vertexUV(cx,     y + s, 0, u0, v1);
            t.vertexUV(cx + s, y + s, 0, u1, v1);
            t.vertexUV(cx + s, y,     0, u1, v0);
            cx += (int)(6 * scale);
        }
        t.flush();
    }

    /** Pixel width of a string at given scale. */
    public static int stringWidth(String text, float scale) {
        return (int)(text.length() * 6 * scale);
    }

    // ─── Button ───────────────────────────────────────────────────────────────

    /**
     * Draw a button with label. Returns true if the mouse cursor is hovering.
     */
    public static boolean drawButton(Tessellator t, int x, int y, int w, int h,
                                     String label, int mouseX, int mouseY,
                                     boolean enabled) {
        boolean hover = enabled
                && mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;

        int bg = !enabled ? COLOR_BTN_DIS : hover ? COLOR_BTN_HOVER : COLOR_BTN;
        fillRect(t, x, y, w, h, bg);
        drawRect(t, x, y, w, h, hover ? COLOR_WHITE : COLOR_DARK_GRAY);

        int textColor = !enabled ? COLOR_DARK_GRAY : hover ? COLOR_YELLOW : COLOR_WHITE;
        int tx = x + (w - stringWidth(label, 1f)) / 2;
        int ty = y + (h - 8) / 2;
        drawString(t, label, tx, ty, textColor, 1f);

        return hover;
    }

    // ─── Scrollbar ────────────────────────────────────────────────────────────

    /**
     * Draw a vertical scrollbar.
     *
     * @param x         Left x of the scrollbar rail
     * @param y         Top y
     * @param h         Total height of rail
     * @param total     Total number of items
     * @param visible   How many items are visible
     * @param scroll    Current scroll offset
     */
    public static void drawScrollbar(Tessellator t, int x, int y, int h,
                                     int total, int visible, int scroll) {
        fillRect(t, x, y, 6, h, COLOR_PANEL);
        if (total <= visible) return;
        int thumbH = Math.max(10, h * visible / total);
        int maxScroll = total - visible;
        int thumbY = y + (h - thumbH) * scroll / maxScroll;
        fillRect(t, x, thumbY, 6, thumbH, COLOR_GRAY);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static void setColor(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;
        glColor4f(r, g, b, a);
    }

    private static void setColorGL(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
    }

    private static int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
