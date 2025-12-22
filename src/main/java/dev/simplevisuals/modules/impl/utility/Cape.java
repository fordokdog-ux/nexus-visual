package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import net.minecraft.client.MinecraftClient;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.resource.language.I18n;

public class Cape extends Module {

    private final BooleanSetting onlySelf = new BooleanSetting("На себе", false);
    private final BooleanSetting allPlayers = new BooleanSetting("На всех", true);
    public final ListSetting targets = new ListSetting("Кому", true, onlySelf, allPlayers);

    public Cape() {
        super("Cape", Category.Utility, I18n.translate("module.cape.description"));

        // Single-select behavior is handled by ClickGUI; default is "На всех".
        getSettings().add(targets);
    }

    public boolean applyTo(GameProfile profile) {
        if (!isToggled()) return false;
        if (profile == null) return false;
        if (allPlayers.getValue()) return true;
        if (!onlySelf.getValue()) return true; // fallback
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        return profile.getId() != null && profile.getId().equals(mc.player.getGameProfile().getId());
    }

}
