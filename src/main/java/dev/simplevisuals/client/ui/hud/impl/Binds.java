package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.ui.hud.HudStyle;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.api.Bind;
import net.minecraft.client.gui.screen.ChatScreen;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Binds extends HudElement {

    private final ThemeManager themeManager;

    public Binds() {
        super("Binds");
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        ThemeManager.Theme theme = themeManager.getCurrentTheme();
        var matrices = e.getContext().getMatrices();

        List<Row> rows = new ArrayList<>();
        for (Module m : NexusVisual.getInstance().getModuleManager().getModules()) {
            if (!m.isToggled()) continue;
            Bind b = m.getBind();
            if (b == null || b.isEmpty()) continue;
            rows.add(new Row(m.getName(), b.toString()));
        }
        rows.sort(Comparator.comparing(r -> r.name, String.CASE_INSENSITIVE_ORDER));

        boolean preview = mc.currentScreen instanceof ChatScreen;
        if (preview && rows.isEmpty()) {
            rows.add(new Row("KillEffect", "R"));
            rows.add(new Row("TargetEsp", "V"));
        }

        float x = getX();
        float y = getY();

        // Стилизация как Potions: карточка + заголовок, строки без лишнего шума, бинд в правой плашке.
        float uiScale = 0.92f;
        float pad = 8f * uiScale;
        float spacing = 6f * uiScale;
        float rowH = 16f * uiScale;
        float radius = 7f * uiScale;
        float font = 8f * uiScale;
        float titleFont = 9f * uiScale;

        String title = "Binds";
        float titleW = Fonts.BOLD.getWidth(title, titleFont);
        float titleH = Fonts.BOLD.getHeight(titleFont);

        float maxW = Math.max(90f, pad * 2f + titleW);
        for (Row r : rows) {
            float nameW = Fonts.MEDIUM.getWidth(r.name, font);
            float bindW = Fonts.MEDIUM.getWidth(r.bind, font);
            float pillW = bindW + 6f * uiScale;
            float wRow = pad * 2f + nameW + 10f * uiScale + pillW;
            if (wRow > maxW) maxW = wRow;
        }

        float w = maxW;
        float h = pad + titleH + spacing + (rows.isEmpty() ? rowH : rows.size() * rowH) + pad;
        setBounds(x, y, w, h);

        HudStyle.drawCard(matrices, x, y, w, h, radius, theme, 200, 120);

        Color text = themeManager.getTextColor();
        Color subtle = HudStyle.alphaCap(text, 175);

        float titleX = x + pad;
        float titleY = y + pad - 0.25f;
        Render2D.drawFont(matrices, Fonts.BOLD.getFont(titleFont), title, titleX, titleY, text);

        float cy = titleY + titleH + spacing;
        if (rows.isEmpty()) {
            String none = "No binds";
            Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(font), none, x + pad, cy + (rowH - Fonts.MEDIUM.getHeight(font)) / 2f, subtle);
            super.onRender2D(e);
            return;
        }

        float rightEdge = x + w - pad;
        for (Row r : rows) {
            float rowCenterY = cy + rowH / 2f;

            // Module name
            float nameY = rowCenterY - Fonts.MEDIUM.getHeight(font) / 2f;
            Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(font), r.name, x + pad, nameY, text);

            // Bind pill on the right
            float bw = Fonts.MEDIUM.getWidth(r.bind, font);
            float pillW = bw + 6f * uiScale;
            float pillH = rowH - 4f * uiScale;
            float pillX = rightEdge - pillW;
            float pillY = cy + 2f * uiScale;
            HudStyle.drawInset(matrices, pillX, pillY, pillW, pillH, 6f * uiScale, theme, 155);
            Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(font), r.bind,
                    pillX + (pillW - bw) / 2f,
                    rowCenterY - Fonts.MEDIUM.getHeight(font) / 2f,
                    subtle);

            cy += rowH;
        }

        super.onRender2D(e);
    }

    private record Row(String name, String bind) {}
}
