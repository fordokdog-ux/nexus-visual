package dev.simplevisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.FriendsManager;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class SkeletonEsp extends Module {
    
    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255).getRGB());
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей", new Color(0, 255, 0).getRGB());
    private final NumberSetting lineWidth = new NumberSetting("Толщина", 1.5f, 0.5f, 5.0f, 0.1f);
    private final BooleanSetting renderSelf = new BooleanSetting("На себе", true);
    
    public SkeletonEsp() {
        super("SkeletonEsp", Category.Render, "Показывает скелет игроков");
    }
    
    private boolean occludedByBlocks(Vec3d from, Vec3d to) {
        if (mc.world == null) return true;
        HitResult hr = mc.world.raycast(new RaycastContext(
                from,
                to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hr.getType() != HitResult.Type.MISS;
    }

    private boolean shouldRenderThroughPlayers(Vec3d cameraPos, PlayerEntity player, float tickDelta) {
        // Проверяем блоки (игроков игнорируем): если блок перекрывает — не рисуем.
        double px = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double py = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        float h = player.getHeight();
        Vec3d feet = new Vec3d(px, py + 0.15, pz);
        Vec3d chest = new Vec3d(px, py + h * 0.6, pz);
        Vec3d head = new Vec3d(px, py + h * 0.9, pz);

        // Если хотя бы одна точка видима без блока — считаем "не за блоком".
        return !occludedByBlocks(cameraPos, chest)
                || !occludedByBlocks(cameraPos, head)
                || !occludedByBlocks(cameraPos, feet);
    }

    @EventHandler
    private void onRender3D(EventRender3D.Game event) {
        if (mc.world == null || mc.player == null) return;
        
        float tickDelta = event.getTickDelta();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        float baseWidth = lineWidth.getValue().floatValue();
        RenderSystem.lineWidth(1.0f);
        // Рисуем поверх игроков, но скрываем блоками (через raycast)
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !renderSelf.getValue()) continue;
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (!player.isAlive()) continue;

            if (!shouldRenderThroughPlayers(cameraPos, player, tickDelta)) continue;

            float dist = (float) cameraPos.distanceTo(player.getPos());
            // Reduce width with distance so far targets don't look "fat".
            float falloff = 3.0f / (dist + 3.0f);
            float effectiveWidth = MathHelper.clamp(baseWidth * falloff, 0.10f, baseWidth);
            RenderSystem.lineWidth(effectiveWidth);
            
            int lineColor = FriendsManager.checkFriend(player.getName().getString()) 
                    ? friendColor.getValue() | 0xFF000000
                    : color.getValue() | 0xFF000000;
            
            renderSkeleton(event.getMatrices(), player, cameraPos, tickDelta, lineColor);
        }
        
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
    }
    
    private void renderSkeleton(MatrixStack matrices, PlayerEntity player, Vec3d camera, float partialTicks, int lineColor) {
        // Интерполяция позиции
        double x = MathHelper.lerp(partialTicks, player.prevX, player.getX()) - camera.x;
        double y = MathHelper.lerp(partialTicks, player.prevY, player.getY()) - camera.y;
        double z = MathHelper.lerp(partialTicks, player.prevZ, player.getZ()) - camera.z;
        
        float scale = player.isBaby() ? 0.5f : 1.0f;
        
        // Высоты частей тела
        double headY, torsoTopY, torsoBottomY, handY, footY;
        float armOffset = 0.33f;
        float legOffset = 0.15f * scale;
        
        if (player.isSwimming() || player.isGliding()) {
            headY = 0.4 * scale;
            torsoTopY = 0.2 * scale;
            torsoBottomY = -0.2 * scale;
            handY = 0.0 * scale;
            footY = -0.5 * scale;
            legOffset = 0.15f * scale;
        } else if (player.isSneaking()) {
            headY = 1.25 * scale;
            torsoTopY = 0.95 * scale;
            torsoBottomY = 0.45 * scale;
            handY = 0.55 * scale;
            footY = 0.0 * scale;
        } else {
            headY = 1.6 * scale;
            torsoTopY = 1.2 * scale;
            torsoBottomY = 0.6 * scale;
            handY = 0.8 * scale;
            footY = 0.0 * scale;
        }
        
        // Интерполяция углов
        float bodyYaw = MathHelper.lerpAngleDegrees(partialTicks, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(partialTicks, player.prevHeadYaw, player.getHeadYaw());
        float headPitch = MathHelper.lerp(partialTicks, player.prevPitch, player.getPitch());
        float netHeadYaw = headYaw - bodyYaw;
        
        headPitch = MathHelper.clamp(headPitch, -60.0f, 60.0f);
        
        // Анимация конечностей
        float limbSwing = player.limbAnimator.getPos(partialTicks);
        float limbSwingAmount = Math.min(1.0f, player.limbAnimator.getSpeed(partialTicks));
        if (player.isBaby()) {
            limbSwing *= 3.0f;
            limbSwingAmount *= 0.8f;
        }
        
        // Углы вращения конечностей
        float rightArmRotateX = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 2.0f * limbSwingAmount * 0.5f;
        float leftArmRotateX = MathHelper.cos(limbSwing * 0.6662f) * 2.0f * limbSwingAmount * 0.5f;
        float rightLegRotateX = MathHelper.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        float leftLegRotateX = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.4f * limbSwingAmount;
        
        // Анимация атаки
        float swingProgress = player.getHandSwingProgress(partialTicks);
        if (swingProgress > 0.0f) {
            float swingAngle = -MathHelper.sin(swingProgress * (float) Math.PI) * 1.5f;
            rightArmRotateX += swingAngle;
        }
        
        // One batch per player for stable, visible lines
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(bodyYaw));
        
        if (player.isSwimming() || player.isGliding()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch + 90.0f));
        } else if (player.isSneaking()) {
            matrices.translate(0.0f, -0.1f * scale, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(4.0f));
        }
        
        // Голова
        matrices.push();
        matrices.translate(0.0f, (float) torsoTopY, 0.0f);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(netHeadYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch * 0.5f));
        drawLine(bb, matrices, 0, 0, 0, 0, (float) (headY - torsoTopY), 0, lineColor);
        matrices.pop();
        
        // Торс
        drawLine(bb, matrices, 0, (float) torsoTopY, 0, 0, (float) torsoBottomY, 0, lineColor);
        
        // Левая рука - соединение с плечом
        drawLine(bb, matrices, 0, (float) torsoTopY, 0, -armOffset, (float) torsoTopY, 0, lineColor);
        // Левая рука
        matrices.push();
        matrices.translate(-armOffset, (float) torsoTopY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leftArmRotateX * (180f / (float) Math.PI)));
        drawLine(bb, matrices, 0, 0, 0, 0, (float) (handY - torsoTopY), 0, lineColor);
        matrices.pop();
        
        // Правая рука - соединение с плечом
        drawLine(bb, matrices, 0, (float) torsoTopY, 0, armOffset, (float) torsoTopY, 0, lineColor);
        // Правая рука
        matrices.push();
        matrices.translate(armOffset, (float) torsoTopY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rightArmRotateX * (180f / (float) Math.PI)));
        drawLine(bb, matrices, 0, 0, 0, 0, (float) (handY - torsoTopY), 0, lineColor);
        matrices.pop();
        
        // Левая нога - соединение с тазом
        drawLine(bb, matrices, 0, (float) torsoBottomY, 0, -legOffset, (float) torsoBottomY, 0, lineColor);
        // Левая нога
        matrices.push();
        matrices.translate(-legOffset, (float) torsoBottomY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leftLegRotateX * (180f / (float) Math.PI)));
        drawLine(bb, matrices, 0, 0, 0, 0, (float) (footY - torsoBottomY), 0, lineColor);
        matrices.pop();
        
        // Правая нога - соединение с тазом
        drawLine(bb, matrices, 0, (float) torsoBottomY, 0, legOffset, (float) torsoBottomY, 0, lineColor);
        // Правая нога
        matrices.push();
        matrices.translate(legOffset, (float) torsoBottomY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rightLegRotateX * (180f / (float) Math.PI)));
        drawLine(bb, matrices, 0, 0, 0, 0, (float) (footY - torsoBottomY), 0, lineColor);
        matrices.pop();
        
        matrices.pop();

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }
    
    private void drawLine(BufferBuilder bb, MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        var matrix = matrices.peek().getPositionMatrix();
        bb.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        bb.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
}
