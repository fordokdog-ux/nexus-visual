package dev.simplevisuals.mixin;

import dev.simplevisuals.modules.impl.render.AspectRatio;
import dev.simplevisuals.NexusVisual;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class AspectRatioMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    private void modifyProjectionMatrix(float fov, @NotNull CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatio module = NexusVisual.getInstance().getModuleManager().getModule(AspectRatio.class);

        if (module.isToggled()) {
            float aspect = module.getAspectRatio().getValue(); // берём из NumberSetting
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.peek().getPositionMatrix().identity();
            matrixStack.peek().getPositionMatrix().mul(
                    new Matrix4f().setPerspective(
                            (float) (fov * 0.017453292), // радианы
                            aspect,
                            0.05f,
                            256.0f
                    )
            );
            cir.setReturnValue(matrixStack.peek().getPositionMatrix());
        }
    }
}
