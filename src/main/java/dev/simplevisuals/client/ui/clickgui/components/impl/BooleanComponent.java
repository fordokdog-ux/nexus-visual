package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
// removed unused ColorUtils import
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import dev.simplevisuals.client.managers.ThemeManager;

public class BooleanComponent extends Component {

    private final BooleanSetting setting;
    private final Animation toggleAnimation = new Animation(300, 1f, true, Easing.simplevisuals);

    public BooleanComponent(BooleanSetting setting) {
        super(setting.getName());
        this.setting = setting;
        this.visible = setting::isVisible;
    }

    private static boolean hoveredInclusive(float x, float y, float w, float h, float mx, float my, float pad) {
        float x0 = x - pad;
        float y0 = y - pad;
        float x1 = x + w + pad;
        float y1 = y + h + pad;
        return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        toggleAnimation.update(setting.getValue());

        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        boolean lightUi = ClickGui.isGuiLightMode();

        boolean hovered = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);
        float progress = (float) Math.max(0f, Math.min(1f, toggleAnimation.getValue()));

        // Modern row background: subtle hover + very light active tint
        if (hovered || setting.getValue()) {
            int a = (int) ((hovered ? 18f : 10f) * ga);
            Color hoverBg = lightUi
                ? new Color(0, 0, 0, a)
                : new Color(255, 255, 255, (int) ((hovered ? 12f : 7f) * ga));
            ClickGuiDraw.roundedRect(x + 2f, y + 1f, width - 4f, height - 2f, 6f, hoverBg);
        }

        Color themeText = ThemeManager.getInstance().getTextColor();
        Color accent = ThemeManager.getInstance().getAccentColor();

        ClickGuiDraw.text(
            Fonts.BOLD.getFont(7.5f),
            I18n.translate(setting.getName()),
            x + 4f,
            y + 3f,
            new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                (int) (Math.max(0f, Math.min(1f, ga)) * themeText.getAlpha()))
        );

        // Switch (modern pill)
        float switchW = 22f;
        float switchH = 12f;
        float switchX = x + width - switchW - 4.5f;
        float switchY = y + (height - switchH) / 2f;

        int accentSum = accent.getRed() + accent.getGreen() + accent.getBlue();
        Color safeAccent = accentSum > 720
            ? (lightUi ? new Color(140, 140, 140) : new Color(200, 200, 200))
            : accent;

        Color trackOff = lightUi
            ? new Color(0, 0, 0, (int) (32f * ga))
            : new Color(255, 255, 255, (int) (26f * ga));
        Color trackOn = new Color(safeAccent.getRed(), safeAccent.getGreen(), safeAccent.getBlue(), (int) (190f * ga));
        int tr = (int) (trackOff.getRed() + (trackOn.getRed() - trackOff.getRed()) * progress);
        int tg = (int) (trackOff.getGreen() + (trackOn.getGreen() - trackOff.getGreen()) * progress);
        int tb = (int) (trackOff.getBlue() + (trackOn.getBlue() - trackOff.getBlue()) * progress);
        int ta = (int) (trackOff.getAlpha() + (trackOn.getAlpha() - trackOff.getAlpha()) * progress);
        ClickGuiDraw.roundedRect(switchX, switchY, switchW, switchH, switchH / 2f, new Color(tr, tg, tb, ta));

        float thumbW = 10f;
        float thumbH = 10f;
        float padding = 1f;
        float thumbX = switchX + padding + (switchW - thumbW - 2f * padding) * progress;
        float thumbY = switchY + (switchH - thumbH) / 2f;
        int thumbA = (int) (255 * ga);
        // Clean white thumb (works in both themes)
        ClickGuiDraw.roundedRect(thumbX, thumbY, thumbW, thumbH, thumbH / 2f, new Color(255, 255, 255, thumbA));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float switchW = 22f;
        float switchH = 12f;
        float switchX = x + width - switchW - 4.5f;
        float switchY = y + (height - switchH) / 2f;

        if (button != 0 && button != 1) return;

        float mx = (float) mouseX;
        float my = (float) mouseY;
        boolean inRow = hoveredInclusive(x, y, width, height, mx, my, 1.5f);
        boolean inSwitch = hoveredInclusive(switchX, switchY, switchW, switchH, mx, my, 2.0f);
        if (inSwitch || inRow) {
            setting.setValue(!setting.getValue());
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