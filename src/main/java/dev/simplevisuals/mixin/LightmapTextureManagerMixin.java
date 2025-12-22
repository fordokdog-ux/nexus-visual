package dev.simplevisuals.mixin;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.modules.impl.render.Fullbright;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Final @Shadow private SimpleFramebuffer lightmapFramebuffer;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/SimpleFramebuffer;endWrite()V", shift = At.Shift.BEFORE))
    public void update(float delta, CallbackInfo ci) {
        try {
            Fullbright fb = NexusVisual.getInstance().getModuleManager().getModule(Fullbright.class);
            if (fb == null || !fb.isToggled()) return;

            // At this point the lightmap framebuffer is bound; override it.
            float v = fb.getIntensity01();
            v = MathHelper.clamp(v, 0.0f, 1.0f);
            RenderSystem.clearColor(v, v, v, 1.0f);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT);
        } catch (Throwable ignored) {
        }
    }
}