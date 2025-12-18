package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.resource.language.I18n;

public class ClientSound extends Module {
    private final @NotNull BooleanSetting pair1 = new BooleanSetting("mode.pair1", true, () -> false);
    private final @NotNull BooleanSetting pair2 = new BooleanSetting("mode.pair2", false, () -> false);
    private final @NotNull BooleanSetting legacy = new BooleanSetting("mode.legacy", false, () -> false);

    private final @NotNull NumberSetting volume = new NumberSetting(
            "setting.volume",
            1.0f,
            0.0f,
            2.0f,
            0.01f
    );

    private final @NotNull ListSetting mode = new ListSetting(
            "setting.sound",
            true,
            pair1, pair2, legacy
    );

    public ClientSound() {
        super("ClientSound", Category.Utility, I18n.translate("module.clientsound.description"));
    }

    public String getEnableSoundId() {
        if (pair2.getValue()) return "simplevisuals:enable2";
        if (legacy.getValue()) return "simplevisuals:enable";
        return "simplevisuals:enable1"; // по умолчанию пара 1
    }

    public String getDisableSoundId() {
        if (pair2.getValue()) return "simplevisuals:disable2";
        if (legacy.getValue()) return "simplevisuals:disable";
        return "simplevisuals:disable1"; // по умолчанию пара 1
    }

    public NumberSetting getVolume() {
        return volume;
    }
}
