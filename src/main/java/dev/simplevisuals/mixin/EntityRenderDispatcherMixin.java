package dev.simplevisuals.mixin;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render3D;
import dev.simplevisuals.mixin.accessors.IWorldRenderer;
import dev.simplevisuals.modules.impl.render.CustomHitBox;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import net.minecraft.util.math.Vec3d;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void simplevisuals$replaceVanillaHitboxes(MatrixStack matrices,
                                                             VertexConsumer vertices,
                                                             Entity entity,
                                                             float r, float g, float b, float a,
                                                             CallbackInfo ci) {
        CustomHitBox module = NexusVisual.getInstance().getModuleManager().getModule(CustomHitBox.class);
        if (module == null || !module.isToggled()) return;

        // Cancel vanilla hitbox drawing
        ci.cancel();

        // Frustum cull: render only if entity is in view
        MinecraftClient mc = MinecraftClient.getInstance();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        Box worldBox = entity.getBoundingBox().expand(0.0020000000949949026);
        // Лерпим хитбокс к интерполированной позиции, чтобы убрать рывки
        var lerped = entity.getLerpedPos(tickDelta);
        var current = entity.getPos();
        worldBox = worldBox.offset(lerped.x - current.x, lerped.y - current.y, lerped.z - current.z);
        if (((IWorldRenderer) mc.worldRenderer).getFrustum() != null && !((IWorldRenderer) mc.worldRenderer).getFrustum().isVisible(worldBox)) return;

        // Theme colors and fill/outline flags
        // Получаем актуальный цвет темы (включая градиентные темы)
        Color theme = ThemeManager.getInstance().getCurrentTheme().getBackgroundColor();
        boolean fill = module.fill.getValue();

        Color outlineColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), module.outlineAlpha.getValue().intValue());
        Color fillColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), module.fillAlpha.getValue().intValue());

        // Use dedicated hitbox line queue with its own width
        Render3D.setHitboxLineWidth(module.lineWidth.getValue().floatValue());

        // Добавляем в очереди CustomHitBox (НЕ рендерим сразу)
        MatrixStack ms = new MatrixStack();
        if (fill) Render3D.renderCustomHitboxBox(ms, worldBox, fillColor);

        // Manually add outline edges to custom hitbox queue
        Vec3d[] v = new Vec3d[]{
                new Vec3d(worldBox.minX, worldBox.minY, worldBox.minZ),
                new Vec3d(worldBox.maxX, worldBox.minY, worldBox.minZ),
                new Vec3d(worldBox.maxX, worldBox.maxY, worldBox.minZ),
                new Vec3d(worldBox.minX, worldBox.maxY, worldBox.minZ),
                new Vec3d(worldBox.minX, worldBox.minY, worldBox.maxZ),
                new Vec3d(worldBox.maxX, worldBox.minY, worldBox.maxZ),
                new Vec3d(worldBox.maxX, worldBox.maxY, worldBox.maxZ),
                new Vec3d(worldBox.minX, worldBox.maxY, worldBox.maxZ)
        };
        int[][] e = new int[][]{{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] ed : e) Render3D.drawCustomHitboxLine(v[ed[0]], v[ed[1]], outlineColor.getRGB());
    }
}

