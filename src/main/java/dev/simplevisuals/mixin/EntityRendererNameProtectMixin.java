package dev.simplevisuals.mixin;

import dev.simplevisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class EntityRendererNameProtectMixin<T extends Entity> {
    
    @ModifyVariable(
        method = "renderLabelIfPresent",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text modifyEntityLabel(Text text) {
        NameProtect nameProtect = NameProtect.getInstance();
        if (nameProtect != null && nameProtect.isToggled() && text != null) {
            String originalText = text.getString();
            String modifiedText = nameProtect.replaceNames(originalText);
            if (!originalText.equals(modifiedText)) {
                return Text.of(modifiedText);
            }
        }
        return text;
    }
}
