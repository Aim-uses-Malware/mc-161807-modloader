package com.mojang.minecraft.modloader.gui;

import com.mojang.minecraft.modloader.ModLoader;
import com.mojang.minecraft.modloader.api.ModMetadata;

import java.util.List;

import static com.mojang.minecraft.modloader.gui.GuiRenderer.*;

/**
 * Main Mods list screen.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────────┐
 *   │                   MODS (N loaded)                   │
 *   │ ┌─────────────────┐  ┌──────────────────────────┐  │
 *   │ │  [mod list]     │  │  Detail panel            │  │
 *   │ │                 │  │  Name / Version / Author │  │
 *   │ │                 │  │  Description             │  │
 *   │ │                 │  │  Config / Website        │  │
 *   │ └─────────────────┘  └──────────────────────────┘  │
 *   │         [ Enable/Disable ]   [ Config ]   [ Done ]  │
 *   └─────────────────────────────────────────────────────┘
 */
public class GuiModList extends GuiScreen {

    // Layout constants
    private static final int LIST_X     = 10;
    private static final int LIST_W     = 180;
    private static final int ROW_H      = 32;
    private static final int DETAIL_X   = 200;
    private static final int PANEL_PAD  = 8;

    private List<ModMetadata> mods;
    private int selectedIndex = 0;
    private int scrollOffset  = 0;

    private int listY;
    private int listH;
    private int visibleRows;

    // Button hit boxes (computed in init)
    private int btnDoneX, btnDoneY, btnDoneW = 80, btnDoneH = 20;
    private int btnToggleX, btnToggleY, btnToggleW = 100, btnToggleH = 20;
    private int btnConfigX, btnConfigY, btnConfigW = 80, btnConfigH = 20;

    @Override
    protected void init() {
        mods = ModLoader.getInstance().getModMetadata();
        listY = 36;
        listH = screenHeight - listY - 36;
        visibleRows = listH / ROW_H;

        int bottomY = screenHeight - 28;
        btnToggleX = LIST_X;
        btnToggleY = bottomY;
        btnConfigX = btnToggleX + btnToggleW + 6;
        btnConfigY = bottomY;
        btnDoneX   = screenWidth - btnDoneW - LIST_X;
        btnDoneY   = bottomY;
    }

    @Override
    protected void drawScreen(float partialTicks) {
        // Panel backgrounds
        fillRect(tessellator, LIST_X, listY, LIST_W, listH, COLOR_PANEL);
        fillRect(tessellator, DETAIL_X, listY, screenWidth - DETAIL_X - 10, listH, COLOR_PANEL);

        // Title
        drawTitle("MODS (" + mods.size() + " loaded)");

        // ── Mod list rows ────────────────────────────────────────────────────
        int maxVisible = visibleRows;
        for (int i = 0; i < maxVisible; i++) {
            int idx = i + scrollOffset;
            if (idx >= mods.size()) break;

            ModMetadata meta = mods.get(idx);
            int ry = listY + i * ROW_H;

            // Row highlight
            boolean selected = (idx == selectedIndex);
            fillRect(tessellator, LIST_X + 1, ry + 1, LIST_W - 2, ROW_H - 2,
                    selected ? 0xFF2A4A6A : 0xFF1A1A1A);

            // Enabled indicator dot
            int dot = meta.enabled ? COLOR_GREEN : COLOR_RED;
            fillRect(tessellator, LIST_X + 4, ry + 11, 6, 10, dot);

            // Name
            int nameColor = meta.enabled ? COLOR_WHITE : COLOR_GRAY;
            GuiRenderer.drawString(tessellator, meta.name,
                    LIST_X + 14, ry + 5, nameColor, 1f);

            // Version
            GuiRenderer.drawString(tessellator, "v" + meta.version,
                    LIST_X + 14, ry + 17, COLOR_DARK_GRAY, 1f);
        }

        // Scrollbar
        GuiRenderer.drawScrollbar(tessellator, LIST_X + LIST_W + 2, listY,
                listH, mods.size(), maxVisible, scrollOffset);

        // ── Detail panel ─────────────────────────────────────────────────────
        if (!mods.isEmpty() && selectedIndex < mods.size()) {
            ModMetadata m = mods.get(selectedIndex);
            int dx = DETAIL_X + PANEL_PAD;
            int dy = listY + PANEL_PAD;

            GuiRenderer.drawString(tessellator, m.name,     dx, dy,       COLOR_YELLOW, 2f);
            dy += 20;
            GuiRenderer.drawString(tessellator, "Version: " + m.version,  dx, dy, COLOR_WHITE,  1f); dy += 12;
            GuiRenderer.drawString(tessellator, "Author:  " + m.author,   dx, dy, COLOR_WHITE,  1f); dy += 12;
            GuiRenderer.drawString(tessellator, "File:    " + m.sourceFile, dx, dy, COLOR_GRAY, 1f); dy += 12;
            GuiRenderer.drawString(tessellator, "Status:  " + (m.enabled ? "Enabled" : "Disabled"),
                    dx, dy, m.enabled ? COLOR_GREEN : COLOR_RED, 1f);
            dy += 16;

            // Description (word-wrapped by newline)
            if (!m.description.isEmpty()) {
                dy += 4;
                GuiRenderer.drawString(tessellator, m.description, dx, dy, COLOR_GRAY, 1f);
                dy += 12;
            }

            if (!m.website.isEmpty()) {
                GuiRenderer.drawString(tessellator, m.website, dx, dy, 0xFF5599FF, 1f);
            }
        }

        // ── Buttons ──────────────────────────────────────────────────────────
        boolean hasMod = !mods.isEmpty() && selectedIndex < mods.size();
        boolean enabled = hasMod && mods.get(selectedIndex).enabled;

        String toggleLabel = (hasMod && enabled) ? "Disable" : "Enable";
        GuiRenderer.drawButton(tessellator,
                btnToggleX, btnToggleY, btnToggleW, btnToggleH,
                toggleLabel, mouseX, mouseY, hasMod);

        GuiRenderer.drawButton(tessellator,
                btnConfigX, btnConfigY, btnConfigW, btnConfigH,
                "Config", mouseX, mouseY, hasMod);

        GuiRenderer.drawButton(tessellator,
                btnDoneX, btnDoneY, btnDoneW, btnDoneH,
                "Done", mouseX, mouseY, true);
    }

    @Override
    protected void mouseClicked(int mx, int my) {
        // List click
        if (mx >= LIST_X && mx < LIST_X + LIST_W
                && my >= listY && my < listY + listH) {
            int row = (my - listY) / ROW_H + scrollOffset;
            if (row >= 0 && row < mods.size()) {
                selectedIndex = row;
            }
            return;
        }

        // Toggle button
        if (mx >= btnToggleX && mx < btnToggleX + btnToggleW
                && my >= btnToggleY && my < btnToggleY + btnToggleH
                && !mods.isEmpty()) {
            ModMetadata m = mods.get(selectedIndex);
            m.enabled = !m.enabled;
            return;
        }

        // Config button
        if (mx >= btnConfigX && mx < btnConfigX + btnConfigW
                && my >= btnConfigY && my < btnConfigY + btnConfigH
                && !mods.isEmpty()) {
            ModMetadata m = mods.get(selectedIndex);
            GuiManager.openScreen(new GuiModConfig(m), screenWidth, screenHeight);
            return;
        }

        // Done
        if (mx >= btnDoneX && mx < btnDoneX + btnDoneW
                && my >= btnDoneY && my < btnDoneY + btnDoneH) {
            GuiManager.closeScreen();
        }
    }

    @Override
    protected void mouseScrolled(int delta) {
        scrollOffset -= Integer.signum(delta);
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, mods.size() - visibleRows)));
    }
}
