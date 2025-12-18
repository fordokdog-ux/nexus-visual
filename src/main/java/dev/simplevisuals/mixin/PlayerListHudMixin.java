package dev.simplevisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.modules.impl.render.BetterMinecraft;
import dev.simplevisuals.NexusVisual;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void simplevisuals$setupTabAnimation(DrawContext context, int scaledWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
		BetterMinecraft module = NexusVisual.getInstance().getModuleManager().getModule(BetterMinecraft.class);
		if (module == null || !module.isToggled() || !module.smoothTab.getValue()) return;

		float value = module.getTabOpenAnimation().getValue();
		// Avoid rendering if the tab is fully closed, but don't cancel yet
		if (!module.isTabPressed() && value <= 0.01f) {
			ci.cancel(); // Cancel only if the tab is effectively invisible
			return;
		}

		MatrixStack matrices = context.getMatrices();
		matrices.push();

		// Apply slide animation (move tab up/down)
		float slideOffset = 22.0f;
		float slide = slideOffset * (1.0f - value);
		matrices.translate(0, -slide, 0);

		// Enable blending and set transparency for all rendered elements
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1f, 1f, 1f, value);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void simplevisuals$cleanupTabAnimation(DrawContext context, int scaledWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
		BetterMinecraft module = NexusVisual.getInstance().getModuleManager().getModule(BetterMinecraft.class);
		if (module == null || !module.isToggled() || !module.smoothTab.getValue()) return;

		// Clean up: reset the matrix stack and blending state
		context.getMatrices().pop();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.disableBlend();
	}
}