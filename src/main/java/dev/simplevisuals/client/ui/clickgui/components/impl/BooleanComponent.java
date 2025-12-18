package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
// removed unused ColorUtils import
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.renderer.Render2D;
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        toggleAnimation.update(setting.getValue());

        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));

        // Рендер текста
        Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(7.5f),
                I18n.translate(setting.getName()),
                x + 4f, y + 3f,
                new Color(
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getRed(),
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getGreen(),
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getBlue(),
                        (int) (ThemeManager.getInstance().getCurrentTheme().getTextColor().getAlpha() * ga)
                ));

        // Получаем цвета темы
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        // theme text color retrieved inline above; no separate local needed

        float animValue = toggleAnimation.getValue();

        float switchX = x + width - 14f;
        float switchY = y + 3f;
        float switchSize = 10f;

        // Безопасное создание цветов
        int alpha = Math.max(0, Math.min(255, (int) (255 * animValue * ga)));

        // Фон переключателя всегда виден с минимальной непрозрачностью
        int backgroundAlpha = (int) (255f * ga);

        // Фон переключателя (всегда виден)
        Render2D.drawRoundedRect(context.getMatrices(),
                switchX - 2f, switchY - 2f, switchSize + 4f, switchSize + 4f, 2f,
                new Color(67, 67, 67, backgroundAlpha));

        // Иконка "D"
        if (animValue > 0.1f) {
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(switchSize),
                    "D", switchX +1.7f, switchY,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float switchX = x + width - 14f;
        float switchY = y + 3.5f;
        float switchSize = 10f;

        if (MathUtils.isHovered(switchX - 2f, switchY - 2f, switchSize + 4f, switchSize + 4f, (float) mouseX, (float) mouseY) && button == 0) {
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