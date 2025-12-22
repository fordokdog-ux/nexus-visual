package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
// removed unused animation imports
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.ColorUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import dev.simplevisuals.client.managers.ThemeManager;

public class SliderComponent extends Component {

    private final NumberSetting setting;
    // removed unused animation field
    private boolean drag;

    // Smooth animation state
    private float animatedPixel = -1f; // smoothed fill/knob x in pixels
    private float hoverAmount = 0f;    // 0..1 hover/drag highlight

    public SliderComponent(NumberSetting setting) {
        super(setting.getName());
        this.setting = setting;
        this.addHeight = () -> 3f;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // ранний выход, если скрыт или полностью прозрачный
        if (!visible.get() || getGlobalAlpha() <= 0.01f) return;

        // Hover и сглаживание
        boolean hovered = MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY) || drag;
        hoverAmount += ((hovered ? 1f : 0f) - hoverAmount) * 0.2f;

        // Перетаскивание — обновление значения
        if (drag) {
            float value = MathHelper.clamp(
                    MathUtils.round((mouseX - x - 5f) / (width - 12f) * (setting.getMax() - setting.getMin()) + setting.getMin(), setting.getIncrement()),
                    setting.getMin(),
                    setting.getMax()
            );
            setting.setValue(value);
        }

        // Соотношение и целевая позиция ползунка
        float ratio = (float) ((setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()));
        ratio = MathHelper.clamp(ratio, 0f, 1f);
        float barWidth = width - 8f;
        float targetPixel = barWidth * ratio;
        if (animatedPixel < 0f) animatedPixel = targetPixel; // первый кадр — без анимации
        animatedPixel += (targetPixel - animatedPixel) * 0.18f; // сглаживание

        // Параметры эффектов
        float scaleValue = 1f + 0.02f * hoverAmount;
        float fadeValue = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        boolean lightUi = ClickGui.isGuiLightMode();

        Color themeText = ThemeManager.getInstance().getTextColor();
        Color accent = ThemeManager.getInstance().getAccentColor();

        // Масштабирование области
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float scaledX = centerX - (width * scaleValue) / 2f;
        float scaledY = centerY - (height * scaleValue) / 2f;
        float scaledWidth = width * scaleValue;
        float scaledHeight = height * scaleValue;

        // Modern: keep row clean, show only a subtle hover background
        int hoverA = (int) ((lightUi ? 18f : 12f) * fadeValue * hoverAmount);
        if (hoverA > 0) {
            Color hoverBg = lightUi ? new Color(0, 0, 0, hoverA) : new Color(255, 255, 255, hoverA);
            ClickGuiDraw.roundedRect(scaledX + 2f, scaledY + 1f, scaledWidth - 4f, scaledHeight - 2f, 6f, hoverBg);
        }

        // Текст (use theme text color)
        int textA = (int) Math.max(0, Math.min(255, themeText.getAlpha() * fadeValue));
        float textOffset = hoverAmount * 1f;
        ClickGuiDraw.text(
            Fonts.BOLD.getFont(7.5f),
            I18n.translate(setting.getName()),
            scaledX + 4f + textOffset,
            scaledY + 3f,
            new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), textA)
        );

        // Track (thicker pill)
        float trackH = 5f;
        float trackY = scaledY + 13f;
        Color trackBg = lightUi
            ? new Color(0, 0, 0, (int) (28f * fadeValue))
            : new Color(255, 255, 255, (int) (22f * fadeValue));
        ClickGuiDraw.roundedRect(scaledX + 4f, trackY, scaledWidth - 8f, trackH, trackH / 2f, trackBg);

        // Заполнение трека
        int fillA = (int) Math.max(0, Math.min(255, 210f * fadeValue));
        Color fillColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), fillA);
        // легкое усиление при hover
        if (hoverAmount > 0f) {
            fillColor = ColorUtils.fade(fillColor,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (Math.min(255, fillA * 1.2f))),
                    hoverAmount);
        }
        float fillWidth = (scaledWidth - 8f) * (animatedPixel / barWidth);
        ClickGuiDraw.roundedRect(scaledX + 4f, trackY, fillWidth, trackH, trackH / 2f, fillColor);

        // Knob (white) + subtle accent ring on hover
        float knobSize = (8f + 2f * hoverAmount) * scaleValue;
        float knobX = scaledX + 4f + (animatedPixel / barWidth) * (scaledWidth - 8f) - knobSize / 2f;
        float knobY = trackY + (trackH - knobSize) / 2f;
        int knobA = (int) Math.max(0, Math.min(255, 255 * fadeValue));
        ClickGuiDraw.roundedRect(knobX, knobY, knobSize, knobSize, knobSize / 2f, new Color(255, 255, 255, knobA));

        int ringA = (int) (120 * fadeValue * hoverAmount);
        if (ringA > 0) {
            ClickGuiDraw.roundedBorder(knobX - 0.6f, knobY - 0.6f, knobSize + 1.2f, knobSize + 1.2f,
                (knobSize + 1.2f) / 2f, 1.0f, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), ringA));
        }

        // Value pill (right)
        Color textWithAlpha = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
            Math.max(0, Math.min(255, (int) (themeText.getAlpha() * fadeValue))));
        String valueStr = String.valueOf(setting.getValue());
        float vf = 6.5f;
        float vw = Fonts.BOLD.getWidth(valueStr, vf);
        float pillH = 11f;
        float pillW = Math.max(18f, vw + 10f);
        float pillX = scaledX + scaledWidth - pillW - 4f;
        float pillY = scaledY + 3.5f;
        Color pillBg = lightUi
            ? new Color(0, 0, 0, (int) (18f * fadeValue * (0.55f + 0.45f * hoverAmount)))
            : new Color(255, 255, 255, (int) (14f * fadeValue * (0.55f + 0.45f * hoverAmount)));
        ClickGuiDraw.roundedRect(pillX, pillY, pillW, pillH, pillH / 2f, pillBg);
        ClickGuiDraw.text(Fonts.BOLD.getFont(vf), valueStr,
            pillX + (pillW - vw) / 2f,
            pillY + (pillH - Fonts.BOLD.getHeight(vf)) / 2f + 0.4f,
            textWithAlpha);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MathUtils.isHovered(x + 4f, y + 12f, width - 8f, 6f, (float) mouseX, (float) mouseY)) {
            drag = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) drag = false;
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