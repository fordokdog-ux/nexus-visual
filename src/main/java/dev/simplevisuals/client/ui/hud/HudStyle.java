package dev.simplevisuals.client.ui.hud;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render2D;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public final class HudStyle {
    private HudStyle() {
    }

    public static Color alphaCap(Color c, int desiredAlpha) {
        int a = Math.max(0, Math.min(255, desiredAlpha));
        if (c == null) return new Color(0, 0, 0, a);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(c.getAlpha(), a));
    }

    public static Color scaleAlpha(Color c, float mul) {
        if (c == null) return new Color(0, 0, 0, 0);
        int a = Math.max(0, Math.min(255, Math.round(c.getAlpha() * mul)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static void drawCard(MatrixStack matrices, float x, float y, float w, float h, float radius, ThemeManager.Theme theme) {
        // Default: subtle filled card + thin border
        drawCard(matrices, x, y, w, h, radius, theme, 230, 140);
    }

    public static void drawCard(MatrixStack matrices, float x, float y, float w, float h, float radius, ThemeManager.Theme theme, int bgAlpha, int borderAlpha) {
        Color bg = alphaCap(ThemeManager.getInstance().getBackgroundColor(), bgAlpha);
        Color border = alphaCap(ThemeManager.getInstance().getBorderColor(), borderAlpha);
        if (bgAlpha > 0) {
            Render2D.drawRoundedRect(matrices, x, y, w, h, radius, bg);
        }
        Render2D.drawBorder(matrices, x, y, w, h, radius, 0.6f, 0.6f, border);
    }

    public static void drawInset(MatrixStack matrices, float x, float y, float w, float h, float radius, ThemeManager.Theme theme, int alpha) {
        Color fill = alphaCap(ThemeManager.getInstance().getSecondaryBackgroundColor(), alpha);
        Color border = alphaCap(ThemeManager.getInstance().getBorderColor(), Math.min(160, alpha));
        Render2D.drawRoundedRect(matrices, x, y, w, h, radius, fill);
        Render2D.drawBorder(matrices, x, y, w, h, radius, 0.6f, 0.6f, border);
    }
}
