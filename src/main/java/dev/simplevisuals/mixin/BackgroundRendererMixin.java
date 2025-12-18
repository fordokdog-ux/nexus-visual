package dev.simplevisuals.mixin;

import dev.simplevisuals.modules.impl.render.CustomFog;
import dev.simplevisuals.NexusVisual;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

	@Inject(
			method = "getFogColor",
			at = @At("RETURN"),
			cancellable = true
	)
	private static void onGetFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
		CustomFog customFog = NexusVisual.getInstance().getModuleManager().getModule(CustomFog.class);

		if (customFog != null && customFog.isToggled()) {
			var color = customFog.getSkyColor();
			float red = color.getRed() / 255.0f;
			float green = color.getGreen() / 255.0f;
			float blue = color.getBlue() / 255.0f;
			float alpha = color.getAlpha() / 255.0f;
			cir.setReturnValue(new Vector4f(red, green, blue, alpha));
		}
	}

	@ModifyVariable(method = "applyFog", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static float simplevisuals$applyCustomFogDistance(float viewDistance) {
		CustomFog customFog = NexusVisual.getInstance().getModuleManager().getModule(CustomFog.class);
		if (customFog != null && customFog.isToggled()) {
			return Math.max(0.0f, customFog.getFogDistance());
		}
		return viewDistance;
	}
}
