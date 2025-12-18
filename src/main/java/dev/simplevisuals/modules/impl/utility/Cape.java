package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import net.minecraft.client.resource.language.I18n;

public class Cape extends Module {

    public Cape() {
        super("Cape", Category.Utility, I18n.translate("module.cape.description"));
    }

}
