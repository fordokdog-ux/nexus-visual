package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.ui.hud.HudStyle;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Instance;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayDeque;

import static dev.simplevisuals.client.util.Wrapper.mc;

public class KeystrokesHUD extends HudElement {

    private final ThemeManager themeManager;

    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
    private boolean prevLmbHeld;
    private boolean prevRmbHeld;

    public KeystrokesHUD() {
        super("Keystrokes");
        this.themeManager = ThemeManager.getInstance();
    }

    private static void trimOld(ArrayDeque<Long> clicks, long nowMs) {
        long cutoff = nowMs - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        var matrices = e.getContext().getMatrices();
        ThemeManager.Theme theme = themeManager.getCurrentTheme();

        Color text = themeManager.getTextColor();
        Color accent = themeManager.getAccentColor();

        float x = getX();
        float y = getY();

        float pad = 8f;
        float keyW = 22f;
        float keyH = 22f;
        float gap = 3f;
        float radius = 6f;

        float innerW = keyW * 3f + gap * 2f;
        float innerH = keyH * 3f + gap * 2f;
        float totalW = innerW + pad * 2f;
        float totalH = innerH + pad * 2f;

        setBounds(x, y, totalW, totalH);

        // No outer background/border for keystrokes

        boolean w = mc.options.forwardKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        long handle = mc.getWindow().getHandle();
        boolean lmbHeld = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        boolean rmbHeld = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;

        long now = System.currentTimeMillis();
        // Count clicks on rising edge (prevents double counting and avoids relying on mouse events)
        if (lmbHeld && !prevLmbHeld) leftClicks.addLast(now);
        if (rmbHeld && !prevRmbHeld) rightClicks.addLast(now);
        prevLmbHeld = lmbHeld;
        prevRmbHeld = rmbHeld;

        trimOld(leftClicks, now);
        trimOld(rightClicks, now);
        int lCps = leftClicks.size();
        int rCps = rightClicks.size();

        // Helper to draw a key block
        java.util.function.BiConsumer<Rect, Boolean> drawKey = (rect, pressed) -> {
            HudStyle.drawInset(matrices, rect.x, rect.y, rect.w, rect.h, radius, theme, 155);
            if (pressed) {
                Render2D.drawRoundedRect(matrices, rect.x, rect.y, rect.w, rect.h, radius, HudStyle.alphaCap(accent, 65));
            }
        };

        // Layout rects
        float ox = x + pad;
        float oy = y + pad;

        Rect wR = new Rect(ox + keyW + gap, oy, keyW, keyH);
        Rect aR = new Rect(ox, oy + keyH + gap, keyW, keyH);
        Rect sR = new Rect(ox + keyW + gap, oy + keyH + gap, keyW, keyH);
        Rect dR = new Rect(ox + (keyW + gap) * 2f, oy + keyH + gap, keyW, keyH);

        float mouseW = (innerW - gap) / 2f;
        Rect lmbR = new Rect(ox, oy + (keyH + gap) * 2f, mouseW, keyH);
        Rect rmbR = new Rect(ox + mouseW + gap, oy + (keyH + gap) * 2f, mouseW, keyH);

        drawKey.accept(wR, w);
        drawKey.accept(aR, a);
        drawKey.accept(sR, s);
        drawKey.accept(dR, d);
        drawKey.accept(lmbR, lmbHeld);
        drawKey.accept(rmbR, rmbHeld);

        // Labels
        float keyFont = 9f;
        Instance fontKey = Fonts.BOLD.getFont(keyFont);

        drawCentered(matrices, fontKey, "W", wR, text);
        drawCentered(matrices, fontKey, "A", aR, text);
        drawCentered(matrices, fontKey, "S", sR, text);
        drawCentered(matrices, fontKey, "D", dR, text);

        float mouseFont = 7.5f;
        Instance fontMouse = Fonts.REGULAR.getFont(mouseFont);

        drawMouseBlock(matrices, fontMouse, "LMB", lCps, lmbR, text);
        drawMouseBlock(matrices, fontMouse, "RMB", rCps, rmbR, text);

        super.onRender2D(e);
    }

    private static void drawCentered(MatrixStack matrices, Instance font, String text, Rect rect, Color color) {
        float w = font.getWidth(text);
        float h = font.getHeight();
        float tx = rect.x + (rect.w - w) / 2f;
        float ty = rect.y + (rect.h - h) / 2f + 0.25f;
        Render2D.drawFont(matrices, font, text, tx, ty, color);
    }

    private static void drawMouseBlock(MatrixStack matrices, Instance font, String label, int cps, Rect rect, Color color) {
        String txt = label + " " + cps;
        float w = font.getWidth(txt);
        float h = font.getHeight();
        float tx = rect.x + (rect.w - w) / 2f;
        float ty = rect.y + (rect.h - h) / 2f + 0.25f;
        Render2D.drawFont(matrices, font, txt, tx, ty, color);
    }

    private static final class Rect {
        final float x, y, w, h;

        Rect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
