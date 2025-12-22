package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.ui.colorgui.ColorPickerScreen;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class ColorComponent extends Component {

    private final ColorSetting setting;

    public ColorComponent(ColorSetting setting) {
        super(setting.getName());
        this.setting = setting;
        setHeight(20f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);

        boolean lightUi = dev.simplevisuals.client.ui.clickgui.ClickGui.isGuiLightMode();
        float aMul = (float) Math.max(0f, Math.min(1f, globalAlpha));

        // Clean row: show background only on hover
        if (hovered) {
            Color bg = lightUi
                ? new Color(0, 0, 0, (int) (18f * aMul))
                : new Color(255, 255, 255, (int) (12f * aMul));
            ClickGuiDraw.roundedRect(x + 2f, y + 1f, width - 4f, height - 2f, 6f, bg);
        }

        Color text = ThemeManager.getInstance().getTextColor();
        ClickGuiDraw.text(Fonts.MEDIUM.getFont(7.5f), getName(), x + 4f, y + 6f,
            new Color(text.getRed(), text.getGreen(), text.getBlue(), (int) (220 * aMul)));

        float pillW = 34f;
        float pillH = 12f;
        float pillX = x + width - pillW - 6f;
        float pillY = y + (height - pillH) / 2f;

        Color value = setting.getColor();
        ClickGuiDraw.roundedRect(pillX, pillY, pillW, pillH, 6f,
            new Color(value.getRed(), value.getGreen(), value.getBlue(), (int) (255 * aMul)));

        Color accent = ThemeManager.getInstance().getAccentColor();
        ClickGuiDraw.roundedBorder(pillX, pillY, pillW, pillH, 6f, 1.0f,
            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) ((hovered ? 150 : 110) * aMul)));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!getVisible().get()) return;

        float pillW = 34f;
        float pillH = 12f;
        float pillX = x + width - pillW - 6f;
        float pillY = y + (height - pillH) / 2f;

        if (MathUtils.isHovered(pillX, pillY, pillW, pillH, (float) mouseX, (float) mouseY) && mc != null) {
            if (mc.currentScreen != null) {
                mc.setScreen(new ColorPickerScreen(setting, mc.currentScreen));
            } else {
                mc.setScreen(new ColorPickerScreen(setting));
            }
            return;
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
