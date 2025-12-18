package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import net.minecraft.client.resource.language.I18n;

public class ItemPhysic extends Module {
    public ItemPhysic() {
        super("ItemPhysic", Category.Render, I18n.translate("module.itemphysic.description"));
    }
}
