package dev.simplevisuals.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.modules.impl.render.NoRender;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.renderer.Render3D;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.simplevisuals.modules.impl.render.Zoom;
import net.minecraft.client.render.Camera;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements Wrapper {

	@Inject(method = "renderWorld", at = @At("HEAD"))
	public void renderWorld(RenderTickCounter renderTickCounter, CallbackInfo ci) {
		Render3D.prepare();
		// Очищаем очереди для отдельных рендеров
		Render3D.clearCustomHitBoxQueues();
		Render3D.clearBlockOverlayQueues();
	}

	@Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), method = "renderWorld")
	public void renderWorld(RenderTickCounter renderTickCounter, CallbackInfo info, @Local(ordinal = 2) Matrix4f matrix4f3, @Local(ordinal = 1) float tickDelta, @Local MatrixStack matrixStack) {
		RenderSystem.getModelViewStack().pushMatrix();
		RenderSystem.getModelViewStack().mul(matrix4f3);
		MatrixStack cleanMatrixStack = new MatrixStack();
		RenderSystem.getModelViewStack().mul(cleanMatrixStack.peek().getPositionMatrix());
		Render3D.setTickDelta(tickDelta);
		EventRender3D.Game event = new EventRender3D.Game(renderTickCounter, matrixStack);
		NexusVisual.getInstance().getEventHandler().post(event);
		// Рендерим остальные элементы с обычными настройками
		Render3D.draw(Render3D.QUADS, Render3D.DEBUG_LINES, false);
		Render3D.draw(Render3D.SHINE_QUADS, Render3D.SHINE_DEBUG_LINES, true);
		
		// CustomHitBox и BlockOverlay рендерятся в своих модулях
		RenderSystem.getModelViewStack().popMatrix();
	}

	@Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
	public void showFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
		if (NexusVisual.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && NexusVisual.getInstance().getModuleManager().getModule(NoRender.class).totem.getValue()) ci.cancel();
	}

	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
	public void tiltViewWhenHurt(CallbackInfo ci) {
		if (NexusVisual.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && NexusVisual.getInstance().getModuleManager().getModule(NoRender.class).hurtCam.getValue()) ci.cancel();
	}



	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void applyZoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		Zoom zoom = NexusVisual.getInstance().getModuleManager().getModule(Zoom.class);
		if (zoom != null && zoom.isToggled()) {
			float base = cir.getReturnValue();
			float adjusted = zoom.applyZoom(base, tickDelta);
			cir.setReturnValue(adjusted);
		}
	}
}