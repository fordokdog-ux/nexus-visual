package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.EnumSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.modules.settings.api.Nameable;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.*;
import dev.simplevisuals.client.managers.ThemeManager;

public class EnumComponent extends Component {

    private final EnumSetting<?> setting;
    private final Animation openAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private final Map<Enum<?>, Animation> pickAnimations = new HashMap<>();
    private boolean open;

    public EnumComponent(EnumSetting<?> setting) {
        super(setting.getName());
        this.setting = setting;
        for (Enum<?> enums : setting.getValue().getClass().getEnumConstants()) pickAnimations.put(enums, new Animation(300, 1f, false, Easing.BOTH_SINE));
        this.addHeight = () -> openAnimation.getValue() > 0 ? ((setting.getValue().getClass().getEnumConstants().length * 14f)) * openAnimation.getValue() : 0;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update(open);

        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        boolean lightUi = ClickGui.isGuiLightMode();
        Color themeText = ThemeManager.getInstance().getTextColor();
        Color accent = ThemeManager.getInstance().getAccentColor();
        int textA = (int) Math.max(0, Math.min(255, themeText.getAlpha() * ga));
        Color textCol = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), textA);

        boolean hoveredHeader = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);
        float openA = (float) Math.max(0f, Math.min(1f, openAnimation.getValue()));

        // Modern header background when hovered/open
        int bgA = (int) ((hoveredHeader ? 18f : 10f) * ga * (hoveredHeader ? 1f : openA));
        if (bgA > 0) {
            Color bg = lightUi ? new Color(0, 0, 0, bgA) : new Color(255, 255, 255, (int) (bgA * 0.75f));
            ClickGuiDraw.roundedRect(x + 2f, y + 1f, width - 4f, height - 2f, 6f, bg);
        }

        String label = I18n.translate(setting.getName());
        ClickGuiDraw.text(Fonts.BOLD.getFont(7.5f), label, x + 4f, y + 3f, textCol);

        // Value pill on the right
        String value = I18n.translate(setting.currentEnumName());
        float vf = 6.5f;
        float vw = Fonts.BOLD.getWidth(value, vf);
        float pillH = 11f;
        float pillW = Math.max(22f, vw + 10f);
        float pillX = x + width - pillW - 4f;
        float pillY = y + 3.5f;
        Color pillBg = lightUi
            ? new Color(0, 0, 0, (int) ((hoveredHeader ? 20f : 14f) * ga))
            : new Color(255, 255, 255, (int) ((hoveredHeader ? 16f : 10f) * ga));
        ClickGuiDraw.roundedRect(pillX, pillY, pillW, pillH, pillH / 2f, pillBg);
        // Accent border only when open
        if (openA > 0.01f) {
            ClickGuiDraw.roundedBorder(pillX, pillY, pillW, pillH, pillH / 2f, 1.0f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (110f * ga * openA)));
        }
        ClickGuiDraw.text(Fonts.BOLD.getFont(vf), value,
            pillX + (pillW - vw) / 2f,
            pillY + (pillH - Fonts.BOLD.getHeight(vf)) / 2f + 0.4f,
            textCol);

        if (openAnimation.getValue() > 0) {
            float yOffset = height;
            for (Enum<?> enums : setting.getValue().getClass().getEnumConstants()) {
                if (mc.currentScreen instanceof ClickGui clickGui) {
                    clickGui.startScissorScaledPublic(context, x, y + yOffset, width, 14f);
                } else {
                    context.enableScissor((int) x, (int) (y + yOffset), (int) (x + width), (int) (y + yOffset + 14f));
                }
                Animation anim = pickAnimations.get(enums);
                anim.update(enums == setting.getValue());

            boolean hovered = MathUtils.isHovered(x, y + yOffset, width, 14f, mouseX, mouseY);
            int rowA = (int) (255 * ga * openA);

            // subtle hover row background
            if (hovered) {
                Color hoverBg = lightUi
                    ? new Color(0, 0, 0, (int) (18f * ga * openA))
                    : new Color(255, 255, 255, (int) (12f * ga * openA));
                ClickGuiDraw.roundedRect(x + 2f, y + yOffset + 1f, width - 4f, 12f, 6f, hoverBg);
            }

            // selection indicator: small pill on right
            float selW = 14f;
            float selH = 8f;
            float selX = x + width - selW - 5f;
            float selY = y + yOffset + (14f - selH) / 2f;
            int selA = (int) (140f * ga * openA * anim.getValue());
            if (selA > 0) {
                ClickGuiDraw.roundedRect(selX, selY, selW, selH, selH / 2f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), selA));
            }

                ClickGuiDraw.text(
                        Fonts.BOLD.getFont(7.5f),
                        I18n.translate(((Nameable) enums).getName()),
                        x + 6f,
                        y + yOffset + 2f,
                        new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), rowA)
                );
                yOffset += 14f;
                if (mc.currentScreen instanceof ClickGui clickGui) {
                    clickGui.stopScissorPublic(context);
                } else {
                    context.disableScissor();
                }
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY)) {
            if (button == 0) setting.increaseEnum();
            else if (button == 1) open = !open;
        }

        if (open && button == 0) {
            float yOffset = height;
            for (Enum<?> enums : setting.getValue().getClass().getEnumConstants()) {
                if (MathUtils.isHovered(x, y + yOffset, width, 14f, (float) mouseX, (float) mouseY)) {
                    setting.setEnumValue(((Nameable) enums).getName());
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