package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;
import dev.simplevisuals.modules.settings.Setting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListComponent extends Component {

    private final ListSetting setting;
    private final Map<BooleanSetting, Animation> pickAnimations = new HashMap<>();

    @Getter private final Animation openAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private boolean open;

    public ListComponent(ListSetting setting) {
        super(setting.getName());
        this.setting = setting;
        for (BooleanSetting setting1 : setting.getValue()) pickAnimations.put(setting1, new Animation(300, 1f, false, Easing.BOTH_SINE));
        this.visible = setting::isVisible;
        this.addHeight = () -> openAnimation.getValue() > 0 ? (setting.getValue().size() * 14f) * (float) openAnimation.getValue() : 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update(open);

        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        boolean lightUi = ClickGui.isGuiLightMode();
        Color themeText = ThemeManager.getInstance().getTextColor();
        Color accent = ThemeManager.getInstance().getAccentColor();

        boolean hoveredHeader = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);
        float openA = (float) Math.max(0f, Math.min(1f, openAnimation.getValue()));
        if (hoveredHeader || openA > 0.01f) {
            float t = hoveredHeader ? 1f : openA;
            Color bg = lightUi
                ? new Color(0, 0, 0, (int) (18f * ga * t))
                : new Color(255, 255, 255, (int) (12f * ga * t));
            ClickGuiDraw.roundedRect(x + 2f, y + 1f, width - 4f, height - 2f, 6f, bg);
        }

        ClickGuiDraw.text(
            Fonts.BOLD.getFont(7.5f),
            I18n.translate(setting.getName()),
            x + 5f,
            y + 3.5f,
            new Color(
                themeText.getRed(),
                themeText.getGreen(),
                themeText.getBlue(),
                (int) (themeText.getAlpha() * ga)
            )
        );

        // Count pill on the right
        String count = "(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")";
        float cf = 6.5f;
        float cw = Fonts.BOLD.getWidth(count, cf);
        float pillH = 11f;
        float pillW = Math.max(22f, cw + 10f);
        float pillX = x + width - pillW - 4f;
        float pillY = y + 3.5f;
        Color pillBg = lightUi
            ? new Color(0, 0, 0, (int) (14f * ga))
            : new Color(255, 255, 255, (int) (10f * ga));
        ClickGuiDraw.roundedRect(pillX, pillY, pillW, pillH, pillH / 2f, pillBg);
        if (openA > 0.01f) {
            ClickGuiDraw.roundedBorder(pillX, pillY, pillW, pillH, pillH / 2f, 1.0f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (90f * ga * openA)));
        }
        ClickGuiDraw.text(Fonts.BOLD.getFont(cf), count,
            pillX + (pillW - cw) / 2f,
            pillY + (pillH - Fonts.BOLD.getHeight(cf)) / 2f + 0.4f,
            new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), (int) (themeText.getAlpha() * ga)));

        if (openAnimation.getValue() > 0) {
            float yOffset = height;
            float a = openA;
            for (BooleanSetting bs : setting.getValue()) {
                Animation anim = pickAnimations.get(bs);
                anim.update(bs.getValue());
                float textSlide = (1f - a) * 8f;
                int itemAlpha = (int) (themeText.getAlpha() * ga * a);

                boolean hovered = MathUtils.isHovered(x, y + yOffset, width, 14f, mouseX, mouseY);
                if (hovered) {
                    Color hoverBg = lightUi
                        ? new Color(0, 0, 0, (int) (18f * ga * a))
                        : new Color(255, 255, 255, (int) (12f * ga * a));
                    ClickGuiDraw.roundedRect(x + 2f, y + yOffset + 1f, width - 4f, 12f, 6f, hoverBg);
                }

                // Checkbox indicator (left)
                float box = 8f;
                float bx = x + 6f;
                float by = y + yOffset + (14f - box) / 2f;
                Color boxBg = lightUi
                    ? new Color(0, 0, 0, (int) (18f * ga * a))
                    : new Color(255, 255, 255, (int) (14f * ga * a));
                ClickGuiDraw.roundedRect(bx, by, box, box, 3f, boxBg);

                int selA = (int) (220f * ga * a * anim.getValue());
                if (selA > 0) {
                    ClickGuiDraw.roundedRect(bx + 1.2f, by + 1.2f, box - 2.4f, box - 2.4f, 2.2f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), selA));
                }

                ClickGuiDraw.text(
                    Fonts.BOLD.getFont(7.5f),
                    I18n.translate(bs.getName()),
                    x + 6f + box + 4f + textSlide,
                    y + yOffset + 3.5f,
                    new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), itemAlpha)
                );
                yOffset += 14f;
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        // Header interactions
        if (MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY)) {
            if (button == 1) { // Right click: toggle open
                open = !open;
                return;
            }
            if (button == 0) { // Left click
                if (setting.isSingleSelect()) {
                    // Cycle to next option
                    List<BooleanSetting> opts = setting.getValue();
                    int current = -1;
                    for (int i = 0; i < opts.size(); i++) if (opts.get(i).getValue()) { current = i; break; }
                    int next = (current + 1) % Math.max(1, opts.size());
                    for (BooleanSetting bs : opts) bs.setValue(false);
                    opts.get(next).setValue(true);
                } else {
                    open = !open; // Multi-select: toggle open
                }
                return;
            }
        }
        // Select item on left click when open
        if (openAnimation.getValue() > 0 && button == 0) {
            float yOffset = height;
            float visibleH = (float) (setting.getValue().size() * 14f * Math.max(0f, Math.min(1f, openAnimation.getValue())));
            for (BooleanSetting s : setting.getValue()) {
                if (yOffset >= height + visibleH) break; // не кликаем по невидимой части во время анимации
                if (MathUtils.isHovered(x, y + yOffset, width, 14f, (float) mouseX, (float) mouseY)) {
                    if (setting.isSingleSelect()) {
                        for (BooleanSetting all : setting.getValue()) all.setValue(false);
                        s.setValue(true);
                    } else {
                        s.setValue(!s.getValue());
                    }
                    break;
                }
                yOffset += 14f;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public void charTyped(char chr, int modifiers) {

    }
}