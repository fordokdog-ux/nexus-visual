package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;

import java.awt.*;
import net.minecraft.client.resource.language.I18n;

public class CustomFog extends Module {

    private final ThemeManager themeManager;
    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    private final ColorSetting fogColor = new ColorSetting("Цвет", new Color(200, 200, 210, 255).getRGB());
    private final NumberSetting fogDistance = new NumberSetting(
            "setting.fogDistance",
            64.0f,
            0.0f,
            256.0f,
            1.0f
    );

    public CustomFog() {
        super("CustomFog", Category.Render, I18n.translate("module.customfog.description"));
        this.themeManager = ThemeManager.getInstance();

        getSettings().add(useThemeColor);
        getSettings().add(fogColor);
        getSettings().add(fogDistance);
    }

    public Color getSkyColor() {
        return useThemeColor.getValue() ? themeManager.getCurrentTheme().getBackgroundColor() : fogColor.getColor();
    }

    public Color getSkyColorSecondary() {
        return useThemeColor.getValue() ? themeManager.getCurrentTheme().getSecondaryBackgroundColor() : fogColor.getColor();
    }

    public float getFogDistance() {
        return fogDistance.getValue();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

}
