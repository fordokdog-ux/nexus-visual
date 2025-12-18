package dev.simplevisuals.mixin;

import dev.simplevisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudNameProtectMixin {
    
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void modifyPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        NameProtect nameProtect = NameProtect.getInstance();
        if (nameProtect != null && nameProtect.isToggled() && entry != null) {
            Text original = cir.getReturnValue();
            if (original != null) {
                String originalText = original.getString();
                String modifiedText = nameProtect.replaceNames(originalText);
                if (!originalText.equals(modifiedText)) {
                    cir.setReturnValue(Text.of(modifiedText));
                }
            }
        }
    }
}
