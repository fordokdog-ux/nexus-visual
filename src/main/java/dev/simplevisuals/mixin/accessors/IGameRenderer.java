package dev.simplevisuals.mixin.accessors;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface IGameRenderer {

    @Invoker("getFov") float getFov$simple(Camera camera, float tickDelta, boolean changingFov);
}