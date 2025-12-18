package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.WaypointManager;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.world.WorldUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class WaypointOverlay implements Wrapper, ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color textColor;
    private Color bgColor;
    private Color accent;

    public WaypointOverlay() {
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        this.textColor = theme.getTextColor();
        Color bg = theme.getBackgroundColor();
        this.bgColor = bg == null ? new Color(30, 30, 30, 170) : bg;
        this.accent = theme.getAccentColor();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;

        int winW = mc.getWindow().getScaledWidth();
        int winH = mc.getWindow().getScaledHeight();

        for (WaypointManager.Waypoint w : WaypointManager.list()) {
            Vec3d screen = WorldUtils.getPosition(w.pos.add(0, 1.8, 0));
            // Keep only behind-camera check; allow far-plane values (z can be > 1)
            if (!(screen.z > 0)) continue;

            String label = w.name;
            int meters = (int) Math.floor(mc.player.getPos().distanceTo(w.pos));
            String meta = meters + "m";

            float nameSize = 8.5f;
            float metaSize = 8.0f;
            float padX = 4f, padY = 3f;
            float nameW = Fonts.BOLD.getWidth(label, nameSize);
            float metaW = Fonts.BOLD.getWidth(meta, metaSize);
            float width = Math.max(nameW, metaW) + padX * 2f;
            float height = Fonts.BOLD.getHeight(nameSize) + Fonts.BOLD.getHeight(metaSize) + padY * 3f;
            float x = (float) screen.x - width / 2f;
            float y = (float) screen.y - height - 8f;

            // Clamp to screen so distant points don't disappear off-screen
            x = Math.max(5f, Math.min(x, winW - width - 5f));
            y = Math.max(5f, Math.min(y, winH - height - 5f));

            // backdrop
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x + 2, y + 2, width, height, 3f, new Color(0, 0, 0, 70));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x, y, width, height, 3f, bgColor);

            // small marker dot below label
            Render2D.drawRoundedRect(e.getContext().getMatrices(), Math.max(3f, Math.min((float) screen.x, winW - 3f)) - 2f, Math.max(3f, Math.min((float) screen.y, winH - 3f)) - 2f, 4f, 4f, 2f, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));

            // name (top)
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(nameSize), label, x + padX, y + padY, new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 255));
            // distance (bottom)
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(metaSize), meta, x + padX, y + padY + Fonts.BOLD.getHeight(nameSize) + 2f, new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 220));
        }
    }

    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
    }
}