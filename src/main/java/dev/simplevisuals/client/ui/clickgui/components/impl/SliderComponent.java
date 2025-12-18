package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
// removed unused animation imports
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.ColorUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.renderer.Render2D;
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

        // Масштабирование области
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float scaledX = centerX - (width * scaleValue) / 2f;
        float scaledY = centerY - (height * scaleValue) / 2f;
        float scaledWidth = width * scaleValue;
        float scaledHeight = height * scaleValue;

        // Фон с hover эффектом
        Color baseBg = new Color(40, 40, 40, (int) (100 * fadeValue));
        Color hoverBg = new Color(50, 50, 50, (int) (120 * fadeValue));
        Color bg = ColorUtils.fade(baseBg, hoverBg, hoverAmount);
        Render2D.drawRoundedRect(context.getMatrices(), scaledX, scaledY, scaledWidth, scaledHeight, 4f, bg);

        // Текст
        int textA = (int) Math.max(0, Math.min(255, 255 * fadeValue));
        float textOffset = hoverAmount * 1f;
        Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(7.5f),
                I18n.translate(setting.getName()),
                scaledX + 4f + textOffset, scaledY + 3f,
                new Color(255, 255, 255, textA));

        // Трек
        Color trackBg = new Color(23, 23, 23, (int) (100 * fadeValue));
        Render2D.drawRoundedRect(context.getMatrices(), scaledX + 4f, scaledY + 13f,
                scaledWidth - 8f, 4f, 0.5f, trackBg);

        // Заполнение трека
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        int fillA = (int) Math.max(0, Math.min(255, 255 * fadeValue));
        Color fillColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), fillA);
        // легкое усиление при hover
        if (hoverAmount > 0f) {
            fillColor = ColorUtils.fade(fillColor,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (Math.min(255, fillA * 1.2f))),
                    hoverAmount);
        }
        float fillWidth = (scaledWidth - 8f) * (animatedPixel / barWidth);
        Render2D.drawRoundedRect(context.getMatrices(), scaledX + 4f, scaledY + 13f,
                fillWidth, 4f, 0.5f, fillColor);

        // Ручка
        float knobSize = (6f + 2f * hoverAmount) * scaleValue;
        float knobX = scaledX + 1f + (animatedPixel / barWidth) * (scaledWidth - 8f) - (knobSize - 6f) / 2f;
        float knobY = scaledY + 12f - (knobSize - 6f) / 2f;
        int knobA = (int) Math.max(0, Math.min(255, 255 * fadeValue));
        Color knobColor = new Color(255, 255, 255, knobA);
        if (hoverAmount > 0f) {
            knobColor = ColorUtils.fade(knobColor,
                    new Color(255, 255, 255, (int) Math.min(255, knobA * 1.1f)),
                    hoverAmount);
        }
        Render2D.drawRoundedRect(context.getMatrices(),
                knobX, knobY,
                knobSize, knobSize,
                knobSize / 2f, knobColor);

        // Контур ручки
        int outlineA = (int) (120 * fadeValue * hoverAmount);
        if (outlineA > 0) {
            Color outlineColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), outlineA);
            Render2D.drawRoundedRect(context.getMatrices(),
                    knobX - 0.5f, knobY - 0.5f,
                    knobSize + 1f, knobSize + 1f,
                    knobSize / 2f + 0.5f, outlineColor);
        }

        // Значение
        Color baseText = ThemeManager.getInstance().getCurrentTheme().getTextColor();
        Color textWithAlpha = new Color(baseText.getRed(), baseText.getGreen(), baseText.getBlue(),
                Math.max(0, Math.min(255, (int) (baseText.getAlpha() * fadeValue))));
        String valueStr = String.valueOf(setting.getValue());
        float valueOffset = hoverAmount * 1f;
        Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(6f), valueStr,
                scaledX + scaledWidth - Fonts.BOLD.getWidth(valueStr, 6.5f) - 4.5f + valueOffset,
                scaledY + 5f, textWithAlpha);
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