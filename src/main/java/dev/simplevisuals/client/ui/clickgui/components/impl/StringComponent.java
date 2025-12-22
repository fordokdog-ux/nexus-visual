package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.StringSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.ColorUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.clickgui.ClickGui;

public class StringComponent extends Component {

    private final StringSetting setting;
    private boolean typing, selected;
    private final Animation animation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private float scrollOffset = 0f;

    public StringComponent(StringSetting setting) {
        super(setting.getName());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animation.update(typing);

        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        boolean lightUi = ClickGui.isGuiLightMode();

        Color themeText = ThemeManager.getInstance().getTextColor();
        Color accent = ThemeManager.getInstance().getAccentColor();

        String label = I18n.translate(setting.getName());
        String value = setting.getValue() == null ? "" : setting.getValue();

        boolean hovered = MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY);

        // Field background
        float fieldX = x + 4f;
        float fieldY = y + 1f;
        float fieldW = width - 8f;
        float fieldH = height - 2f;
        float rr = 5f;

        int bgA = (int) ((lightUi ? 22f : 18f) * ga);
        int bgHoverA = (int) ((lightUi ? 30f : 24f) * ga);
        Color baseBg = lightUi ? new Color(0, 0, 0, bgA) : new Color(255, 255, 255, bgA);
        Color hoverBg = lightUi ? new Color(0, 0, 0, bgHoverA) : new Color(255, 255, 255, bgHoverA);
        ClickGuiDraw.roundedRect(fieldX, fieldY, fieldW, fieldH, rr, hovered ? hoverBg : baseBg);

        // Accent border when typing
        int borderA = (int) ((typing ? 170f : 85f) * ga);
        ClickGuiDraw.roundedBorder(fieldX, fieldY, fieldW, fieldH, rr, 1.0f,
            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderA));

        int textA = (int) (Math.max(0f, Math.min(1f, ga)) * themeText.getAlpha());
        Color labelCol = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), textA);
        Color valueCol = lightUi
                ? new Color(25, 25, 25, (int) (235f * ga))
                : new Color(235, 235, 235, (int) (235f * ga));

        float fontSize = 7.5f;
        float padX = 6f;
        float padY = 3.2f;
        float textX = fieldX + padX;
        float textY = fieldY + padY;

        // Label (left)
        ClickGuiDraw.text(Fonts.BOLD.getFont(fontSize), label, textX, textY, labelCol);

        // Value (right)
        float valueAreaX = fieldX + fieldW * 0.52f;
        float valueAreaW = fieldX + fieldW - valueAreaX - 6f;

        float valueFont = 7.5f;
        float valueTextW = Fonts.REGULAR.getWidth(value, valueFont);
        float maxTextWidth = Math.max(8f, valueAreaW);

        if (valueTextW > maxTextWidth) {
            if (valueTextW - scrollOffset > maxTextWidth) scrollOffset = valueTextW - maxTextWidth;
            if (valueTextW - scrollOffset < 0) scrollOffset = valueTextW;
        } else {
            scrollOffset = 0f;
        }

        // Clip value area (scale-aware)
        float clipX = valueAreaX;
        float clipY = fieldY;
        float clipW = valueAreaW + 6f;
        float clipH = fieldH;
        if (mc.currentScreen instanceof ClickGui clickGui) {
            clickGui.startScissorScaledPublic(context, clipX, clipY, clipW, clipH);
        } else {
            context.enableScissor((int) clipX, (int) clipY, (int) (clipX + clipW), (int) (clipY + clipH));
        }

        float valueX = fieldX + fieldW - 6f - Math.min(valueTextW, maxTextWidth);
        float drawValueX = valueX - scrollOffset;

        if (selected && !value.isEmpty()) {
            ClickGuiDraw.roundedRect(drawValueX - 1f, textY - 0.6f,
                Math.min(valueTextW, maxTextWidth) + 2f, Fonts.REGULAR.getHeight(valueFont) + 1.2f, 2f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (80f * ga)));
        }

        if (!value.isEmpty()) {
            ClickGuiDraw.text(Fonts.REGULAR.getFont(valueFont), value, drawValueX, textY, valueCol);
        }

        // Caret
        if (typing) {
            boolean blink = ((System.currentTimeMillis() / 450L) % 2L) == 0L;
            if (blink) {
                float caretX = drawValueX + valueTextW + 1f;
                ClickGuiDraw.text(Fonts.REGULAR.getFont(valueFont), "|", caretX, textY - 0.2f,
                    new Color(valueCol.getRed(), valueCol.getGreen(), valueCol.getBlue(), (int) (200f * ga)));
            }
        }

        if (mc.currentScreen instanceof ClickGui clickGui) {
            clickGui.stopScissorPublic(context);
        } else {
            context.disableScissor();
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY) && button == 0) typing = !typing;
        else typing = false;
        selected = false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (selected) {
                    setting.setValue("");
                    selected = false;
                }
                if (typing && setting.getValue() != null && !setting.getValue().isEmpty()) setting.setValue(setting.getValue().substring(0, setting.getValue().length() - 1));
            }
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> {
                if (typing) {
                    typing = false;
                    selected = false;
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (typing) {
                    setting.setValue("");
                    selected = false;
                }
            }
            case GLFW.GLFW_KEY_C -> {
                if (Screen.hasControlDown() && typing && selected && setting.getValue() != null && !setting.getValue().isEmpty()) {
                    GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), setting.getValue());
                    selected = false;
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (Screen.hasControlDown() && typing && GLFW.glfwGetClipboardString(mc.getWindow().getHandle()) != null) {
                    selected = false;
                    setting.setValue(setting.getValue() + GLFW.glfwGetClipboardString(mc.getWindow().getHandle()));
                }
            }
            case GLFW.GLFW_KEY_A -> {
                if (Screen.hasControlDown() && typing && setting.getValue() != null && !setting.getValue().isEmpty()) selected = true;
            }
        }
    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {}

    @Override
    public void charTyped(char chr, int modifiers) {
        if (!typing) return;
        if (setting.isOnlyDigit() && !Character.isDigit(chr)) return;
        setting.setValue(setting.getValue() + chr);
        selected = false;
    }
}
