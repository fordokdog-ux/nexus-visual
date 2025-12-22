package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.util.perf.Perf;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.modules.impl.render.UI;
import dev.simplevisuals.NexusVisual;

import java.awt.*;
import java.util.List;

public class PerfHUD extends HudElement {
    private static final float WIDTH = 180f;
    private static final float ROW_H = 10f;
    private static final float PADDING = 4f;

    public PerfHUD() {
        super("Perf");
    }

    private static Color withAlpha(Color c, int a) {
        if (c == null) return new Color(0, 0, 0, a);
        int cap = Math.max(0, Math.min(255, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(c.getAlpha(), cap));
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        var matrices = e.getContext().getMatrices();

        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
        Color bg = ThemeManager.getInstance().getBackgroundColor();
        Color text = ThemeManager.getInstance().getTextColor();
        Color dim = withAlpha(text, 180);

        Perf.endFrame();

        List<Perf.Row> rows = Perf.snapshot();
        int max = Math.min(10, rows.size());
        float h = ROW_H * (max + 1) + PADDING * 2;
        setBounds(getX(), getY(), WIDTH, h);

        Render2D.drawRoundedRect(matrices, getX(), getY(), WIDTH, h, 4f, bg);

        float x = getX() + PADDING;
        float y = getY() + PADDING;
        float size = 7.5f;
        Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), "Section", x, y, text);
        Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), "CPU ms % | GPU ms %", x + 82f, y, dim);
        y += ROW_H;

        for (int i = 0; i < max; i++) {
            var r = rows.get(i);
            String left = r.name;
            String right = String.format("%.2f %2.0f | %.2f %2.0f", r.cpuMs, r.cpuPct, r.gpuMs, r.gpuPct);
            Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), left, x, y, text);
            float rw = Fonts.REGULAR.getWidth(right, size);
            Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), right, getX() + WIDTH - PADDING - rw, y, text);
            y += ROW_H;
        }

        super.onRender2D(e);
    }
}
