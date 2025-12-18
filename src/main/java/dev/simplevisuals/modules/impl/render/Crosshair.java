package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.HitResult;

import java.awt.*;

public class Crosshair extends Module implements ThemeManager.ThemeChangeListener {

    private static Crosshair instance; // Для доступа через Mixin

    private final NumberSetting thickness = new NumberSetting("Толщина", 1f, 0.5f, 3f, 0.1f);
    private final NumberSetting length = new NumberSetting("Длина", 3f, 1f, 8f, 0.5f);
    private final NumberSetting gap = new NumberSetting("Разрыв", 2f, 0f, 5f, 0.5f);
    private final BooleanSetting dynamicGap = new BooleanSetting("Динамический разрыв", false);
    private final BooleanSetting useEntityColor = new BooleanSetting("Цвет при наведении", false);
    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255, 255).getRGB());

    private final ThemeManager themeManager;
    private Color currentColor;
    private final Color entityColor = new Color(255, 0, 0);

    private static Color opaque(Color c) {
        if (c == null) return new Color(255, 255, 255, 255);
        if (c.getAlpha() == 255) return c;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
    }

    public Crosshair() {
        super("Crosshair", Category.Render, "Кастомный прицел");
        instance = this;
        themeManager = ThemeManager.getInstance();
        currentColor = color.getColor();

        getSettings().add(thickness);
        getSettings().add(length);
        getSettings().add(gap);
        getSettings().add(dynamicGap);
        getSettings().add(useEntityColor);
        getSettings().add(useThemeColor);
        getSettings().add(color);
    }

    public static Crosshair getInstance() {
        return instance;
    }

    public void render(DrawContext context) {
        if (!isToggled()) return;
        if (mc.player == null || mc.world == null) return;

        if (!mc.options.getPerspective().isFirstPerson()) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        float x = sw * 0.5f;
        float y = sh * 0.5f;

        float currentGap = gap.getValue();
        if (dynamicGap.getValue()) {
            float cooldown = 1f - mc.player.getAttackCooldownProgress(0);
            currentGap = Math.min(currentGap + 8f * cooldown, 10f);
        }

        float w = thickness.getValue();
        float l = length.getValue();

        Color base = useThemeColor.getValue() ? opaque(themeManager.getCurrentTheme().getBackgroundColor()) : opaque(color.getColor());
        if (useEntityColor.getValue() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            base = opaque(entityColor);
        }

        var matrices = context.getMatrices();
        Render2D.drawRect(matrices, x - w / 2, y - currentGap - l, w, l, base);
        Render2D.drawRect(matrices, x - w / 2, y + currentGap, w, l, base);
        Render2D.drawRect(matrices, x - currentGap - l, y - w / 2, l, w, base);
        Render2D.drawRect(matrices, x + currentGap, y - w / 2, l, w, base);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }
}