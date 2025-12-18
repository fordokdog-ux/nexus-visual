package dev.simplevisuals.client.render.renderers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static dev.simplevisuals.client.util.Wrapper.mc;

public class JelloRenderer {

    private final ThemeManager themeManager;

    public JelloRenderer(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    public void render(EventRender3D.Game e,
                       LivingEntity target,
                       Vec3d fadeOrigin,
                       float lastKnownWidth,
                       float lastKnownHeight,
                       double jelloHeight,
                       double jelloAnimationSpeed,
                       boolean jelloGlow,
                       double jelloGlowIntensity,
                       float animationValue,
                       Color overrideColor) {
        if (target == null && fadeOrigin == null) return;
        if (animationValue <= 0) return; // Не рендерим если анимация появления/исчезания завершена
        
        Vec3d centerWorld = target != null ? target.getLerpedPos(e.getTickDelta()).add(0, target.getHeight() * 0.5, 0) : fadeOrigin;
        var camera = mc.gameRenderer.getCamera();

        double tPosX = centerWorld.x - camera.getPos().x;
        double tPosY = centerWorld.y - camera.getPos().y - (target != null ? target.getHeight() : lastKnownHeight) * 0.5;
        double tPosZ = centerWorld.z - camera.getPos().z;

        // Применяем анимацию появления/исчезания к высоте
        float height = (float) jelloHeight * animationValue;

        double duration = jelloAnimationSpeed;
        double elapsed = (System.currentTimeMillis() % duration);
        boolean side = elapsed > duration / 2.0;
        double progress = elapsed / (duration / 2.0);
        if (side) {
            --progress;
        } else {
            progress = 1 - progress;
        }
        progress = progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
        double eased = (double) (height / 1.5F) * (progress > 0.5 ? 1.0 - progress : progress) * (double) (side ? -1 : 1) * animationValue;

        MatrixStack matrices = e.getMatrices();
        matrices.push();
        matrices.translate(tPosX, tPosY, tPosZ);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();

        if (mc.player != null && target != null && mc.player.canSee(target)) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Color themeColor = overrideColor != null ? overrideColor : themeManager.getThemeColor();
        // Базовые цвета с анимацией появления/исчезания
        int baseBloomColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), clamp255(255 * (jelloGlow ? jelloGlowIntensity : 1.0f))).getRGB();
        int baseCoreColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), clamp255(1)).getRGB();

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int segments = 360;
        // Применяем анимацию появления/исчезания к масштабу
        float radius = lastKnownWidth * animationValue;
        
        // Рендерим трэйл из цельных кругов с разными фазами
        int trailLayers = 6; // Количество слоев трэйла
        
        for (int layer = 0; layer < trailLayers; layer++) {
            // Старые слои становятся более прозрачными
            float layerAlpha = Math.max(0.0f, 1.0f - (layer * 0.2f));
            
            // Рендерим цельный цилиндр для этого слоя
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            
            for (int i = 0; i <= segments; ++i) {
                double angle = Math.toRadians(i);
                float x = (float) (Math.cos(angle) * radius);
                float z = (float) (Math.sin(angle) * radius);

                // Применяем альфа слоя к цветам (цельный круг)
                int finalCoreColor = applyAlphaToColor(baseCoreColor, layerAlpha);
                int finalBloomColor = applyAlphaToColor(baseBloomColor, layerAlpha);

                // Рендерим боковые стенки цилиндра
                buffer.vertex(matrix, x, (float) ((height * progress + eased)), z).color(finalCoreColor);
                buffer.vertex(matrix, x, (float) ((height * progress)), z).color(finalBloomColor);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableCull();
        if (mc.player != null && target != null && mc.player.canSee(target)) {
            RenderSystem.depthMask(true);
        }
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        matrices.pop();
    }

    private static int clamp255(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }
    
    private static int applyAlphaToColor(int color, float alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int existingAlpha = (color >> 24) & 0xFF;
        int newAlpha = clamp255(existingAlpha * alpha);
        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }
}