package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.client.events.impl.EventTick;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.Formatting;
import net.minecraft.client.resource.language.I18n;

public class AutoRespawn extends Module {

    public AutoRespawn() {
        super("AutoRespawn", Category.Utility, I18n.translate("module.autorespawn.description"));
    }

    public void onEnable() {
        super.onEnable();
    }

    private final BooleanSetting deathCoords = new BooleanSetting("setting.deathCoordinates", true);
    private boolean shouldRespawn;

    @EventHandler
    public void onUpdate(EventTick eventTick) {
        if (mc.currentScreen instanceof DeathScreen) {
            if (shouldRespawn) {
                if (deathCoords.getValue()) {
                    sendDeathCoords();
                }
                mc.player.requestRespawn();
                mc.setScreen(null);
                shouldRespawn = false;
            }
        } else {
            shouldRespawn = true;
        }
    }

    private void sendDeathCoords() {
        String coords = String.format(
                I18n.translate("death.coords"),
                mc.player.getX(), mc.player.getY(), mc.player.getZ()
        );
        ChatUtils.sendMessage(coords);
    }

    public void onDisable() {
        super.onDisable();
    }
}