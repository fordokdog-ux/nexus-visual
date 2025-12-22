package dev.simplevisuals.client.ui.clickgui.render;

import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Instance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

/**
 * Direct rendering wrapper - no batching, delegates to Render2D immediately.
 * This eliminates Tessellator conflicts that caused visual artifacts.
 */
public final class ClickGuiDraw {

    private static final ThreadLocal<Frame> FRAME = ThreadLocal.withInitial(Frame::new);

    private ClickGuiDraw() {}

    public static void begin(DrawContext ctx) {
        Frame f = FRAME.get();
        f.ctx = ctx;
        f.stack = ctx.getMatrices();
        f.active = true;
    }

    public static void end() {
        Frame f = FRAME.get();
        f.ctx = null;
        f.stack = null;
        f.active = false;
    }

    public static void flush() {
        // No-op: direct rendering, nothing to flush
    }

    public static void rect(float x, float y, float w, float h, Color color) {
        Frame f = FRAME.get();
        if (!f.active) return;
        Render2D.drawRect(f.stack, x, y, w, h, color);
    }

    public static void rect(float x, float y, float w, float h, int c1, int c2, int c3, int c4) {
        Frame f = FRAME.get();
        if (!f.active) return;
        // Use average color for gradient (simplified)
        Color avg = new Color(
            ((c1 >> 16 & 0xFF) + (c2 >> 16 & 0xFF) + (c3 >> 16 & 0xFF) + (c4 >> 16 & 0xFF)) / 4,
            ((c1 >> 8 & 0xFF) + (c2 >> 8 & 0xFF) + (c3 >> 8 & 0xFF) + (c4 >> 8 & 0xFF)) / 4,
            ((c1 & 0xFF) + (c2 & 0xFF) + (c3 & 0xFF) + (c4 & 0xFF)) / 4,
            ((c1 >> 24 & 0xFF) + (c2 >> 24 & 0xFF) + (c3 >> 24 & 0xFF) + (c4 >> 24 & 0xFF)) / 4
        );
        Render2D.drawRect(f.stack, x, y, w, h, avg);
    }

    public static void roundedRect(float x, float y, float w, float h, float r, Color color) {
        Frame f = FRAME.get();
        if (!f.active) return;
        Render2D.drawRoundedRect(f.stack, x, y, w, h, r, color);
    }

    public static void roundedBorder(float x, float y, float w, float h, float r, float thickness, Color color) {
        Frame f = FRAME.get();
        if (!f.active) return;
        Render2D.drawBorder(f.stack, x, y, w, h, r, thickness, thickness, color);
    }

    public static void shadow(float x, float y, float w, float h, float r, float spread, Color shadowColor) {
        // Intentionally no-op: user requested no shadows.
    }

    public static void text(Instance font, String text, float x, float y, Color color) {
        Frame f = FRAME.get();
        if (!f.active || f.stack == null) return;
        Render2D.drawFont(f.stack, font, text, x, y, color);
    }

    public static boolean isActive() {
        return FRAME.get().active;
    }

    private static final class Frame {
        DrawContext ctx;
        MatrixStack stack;
        boolean active;
    }
}
