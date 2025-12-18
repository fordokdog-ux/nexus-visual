package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.ui.hud.HudStyle;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.renderer.fonts.Font;
import dev.simplevisuals.client.util.perf.Perf;
import dev.simplevisuals.modules.impl.render.UI;
import dev.simplevisuals.NexusVisual;

import java.awt.Color;
import net.minecraft.util.Identifier;

public class Watermark extends HudElement implements ThemeManager.ThemeChangeListener {

    private static final Identifier LOGO_TEXTURE = NexusVisual.id("hud/logo.png");

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color accentColor; // цвет для логотипа и глова

    private float totalWidth, totalHeight;

    public Watermark() {
        super("Watermark");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        this.bgColor = theme.getBackgroundColor();
        this.textColor = theme.getTextColor();
        this.accentColor = theme.getAccentColor();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("Watermark.onRender2D")) {

        var matrices = e.getContext().getMatrices();
        Font fontBold = Fonts.BOLD;
        Font fontRegular = Fonts.REGULAR;

        ThemeManager.Theme theme = themeManager.getCurrentTheme();
        Color text = theme.getTextColor();
        Color accent = theme.getAccentColor();

        String title = "Nexus Visual";
        String link = "t.me/NexusVisual";

        float titleSize = 9f;
        float linkSize = 7f;
        float padX = 8f;
        float padY = 6f;
        float gap = 7f;
        float badge = 20f;
        float radius = 7f;

        float titleW = fontBold.getWidth(title, titleSize);
        float linkW = fontRegular.getWidth(link, linkSize);
        float textW = Math.max(titleW, linkW);

        float titleH = fontBold.getHeight(titleSize);
        float linkH = fontRegular.getHeight(linkSize);

        totalWidth = padX * 2 + badge + gap + textW;
        totalHeight = padY * 2 + titleH + 2f + linkH;

        setBounds(getX(), getY(), totalWidth, totalHeight);

        HudStyle.drawCard(matrices, getX(), getY(), totalWidth, totalHeight, radius, theme);

        // icon badge
        float bx = getX() + padX;
        float by = getY() + (totalHeight - badge) / 2f;

        // Logo
        float logoPad = 0.0f;
        Render2D.drawTexture(
            matrices,
            bx + logoPad,
            by + logoPad,
            badge - logoPad * 2f,
            badge - logoPad * 2f,
            0f,
            LOGO_TEXTURE,
            HudStyle.alphaCap(accent, 255)
        );

        float tx = bx + badge + gap;
        float ty = getY() + padY;

        Render2D.drawFont(matrices, fontBold.getFont(titleSize), title, tx, ty, HudStyle.alphaCap(text, 255));
        Render2D.drawFont(matrices, fontRegular.getFont(linkSize), link, tx, ty + titleH + 2f, HudStyle.alphaCap(text, 190));

        super.onRender2D(e);
        }
    }
}
