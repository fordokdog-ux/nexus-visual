package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.client.events.impl.EventTick;
import dev.simplevisuals.client.ui.colorgui.ColorPickerScreen;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;

public class UI extends Module {

    public final NumberSetting clickGuiScale = new NumberSetting("ClickGUI Scale", 1.0f, 0.6f, 1.2f, 0.05f);

    public final BooleanSetting overrideThemeColors = new BooleanSetting("Override Theme Colors", false);
    public final ColorSetting overrideBackgroundColor = new ColorSetting("Override Background Color", new java.awt.Color(20, 20, 24, 220).getRGB());
    public final ColorSetting overrideSecondaryBackgroundColor = new ColorSetting("Override Secondary Background", new java.awt.Color(26, 26, 32, 220).getRGB());
    public final ColorSetting overrideAccentColor = new ColorSetting("Override Accent Color", java.awt.Color.RED.getRGB());
    public final ColorSetting overrideTextColor = new ColorSetting("Override Text Color", new java.awt.Color(235, 235, 235, 255).getRGB());


    public UI() {
        super("UI", Category.Render, I18n.translate("module.ui.description"));
        setBind(new Bind(GLFW.GLFW_KEY_RIGHT_SHIFT, false));

        overrideBackgroundColor.setVisible(() -> overrideThemeColors.getValue());
        overrideSecondaryBackgroundColor.setVisible(() -> overrideThemeColors.getValue());
        overrideAccentColor.setVisible(() -> overrideThemeColors.getValue());
        overrideTextColor.setVisible(() -> overrideThemeColors.getValue());

    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!(mc.currentScreen instanceof ClickGui) && !(mc.currentScreen instanceof ColorPickerScreen)) {
            setToggled(false);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

		// Allow opening only when in a world
		if (mc.player == null || mc.world == null) {
			ChatUtils.sendMessage(I18n.translate("simplevisuals.ui.onlyInWorld"));
			setToggled(false);
			return;
		}
        mc.setScreen(NexusVisual.getInstance().getClickGui());


    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.currentScreen instanceof ClickGui) {
            ((ClickGui) mc.currentScreen).close();
        }
    }
}