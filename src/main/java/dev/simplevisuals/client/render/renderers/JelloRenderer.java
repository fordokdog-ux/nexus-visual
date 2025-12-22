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
                       boolean cocoonMode,
                       boolean jelloGlow,
                       double jelloGlowIntensity,
                       float animationValue,
                       Color overrideColor,
                       Color overrideColorSecondary) {
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
        // Additive looks fine for classic jello, but cocoon looks cleaner with normal alpha blending
        if (cocoonMode) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        } else {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        }
        RenderSystem.disableCull();

        if (mc.player != null && target != null && mc.player.canSee(target)) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Color c1 = overrideColor != null ? overrideColor : themeManager.getThemeColor();
        Color c2 = overrideColorSecondary != null ? overrideColorSecondary : c1;

        // Базовые цвета с анимацией появления/исчезания
        int baseBloomColorTop = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), clamp255(255 * (jelloGlow ? jelloGlowIntensity : 1.0f))).getRGB();
        int baseBloomColorBottom = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), clamp255(255 * (jelloGlow ? jelloGlowIntensity : 1.0f))).getRGB();
        int baseCoreColorTop = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), clamp255(1)).getRGB();
        int baseCoreColorBottom = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), clamp255(1)).getRGB();

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int segments = cocoonMode ? 240 : 360;
        // Применяем анимацию появления/исчезания к масштабу
        float radius = lastKnownWidth * animationValue;

        // "Кокон": радиус меняется по высоте (сверху узко -> вниз шире) и плавно переворачивается.
        // Делаем профиль конусом через линейный градиент с smoothstep (без волн/заломов).
        double phase01 = (System.currentTimeMillis() % (long) Math.max(1.0, duration)) / Math.max(1.0, duration);
        float cocoonSin = (float) Math.sin(phase01 * Math.PI * 2.0);
        float cocoonAmp = 0.28f;

        java.util.function.DoubleUnaryOperator smoothstep = (tt) -> {
            double t = Math.max(0.0, Math.min(1.0, tt));
            return t * t * (3.0 - 2.0 * t);
        };
        
        // Рендерим трэйл из цельных кругов с разными фазами
        int trailLayers = cocoonMode ? 4 : 6; // кокон лучше без лишней "грязи"
        
        for (int layer = 0; layer < trailLayers; layer++) {
            // Старые слои становятся более прозрачными
            float layerAlpha = Math.max(0.0f, 1.0f - (layer * 0.2f));
            
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            // Применяем альфа слоя к цветам (цельный круг)
            int finalCoreColorTop = applyAlphaToColor(baseCoreColorTop, layerAlpha);
            int finalCoreColorBottom = applyAlphaToColor(baseCoreColorBottom, layerAlpha);
            int finalBloomColorTop = applyAlphaToColor(baseBloomColorTop, layerAlpha);
            int finalBloomColorBottom = applyAlphaToColor(baseBloomColorBottom, layerAlpha);

            float yBottom = (float) (height * progress);
            float yTop = (float) (height * progress + eased);

            // Для профиля конуса важно, чтобы t=0 было сверху, t=1 снизу.
            // eased может менять знак и местами "переворачивать" yTop/yBottom.
            float yTopFixed = Math.min(yTop, yBottom);
            float yBottomFixed = Math.max(yTop, yBottom);

            if (!cocoonMode) {
                // Обычный цилиндр (как раньше)
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= segments; ++i) {
                    double angle = (i / (double) segments) * (Math.PI * 2.0);
                    float x = (float) (Math.cos(angle) * radius);
                    float z = (float) (Math.sin(angle) * radius);
                    buffer.vertex(matrix, x, yTop, z).color(finalCoreColorTop);
                    buffer.vertex(matrix, x, yBottom, z).color(finalBloomColorBottom);
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            } else {
                // Кокон: больше "стэков" по высоте -> меньше полосатости.
                int stacks = 18;

                // слои трэйла чуть расширяем наружу
                float layerRadiusMul = 1.0f + (layer * 0.03f);

                for (int s = 0; s < stacks; s++) {
                    double t0 = s / (double) stacks;
                    double t1 = (s + 1) / (double) stacks;

                    float tt0 = (float) smoothstep.applyAsDouble(t0);
                    float tt1 = (float) smoothstep.applyAsDouble(t1);

                    float yy0 = yTopFixed + (yBottomFixed - yTopFixed) * tt0;
                    float yy1 = yTopFixed + (yBottomFixed - yTopFixed) * tt1;

                    // Конусный профиль: всегда сверху уже -> снизу шире.
                    // Пульсация должна менять "силу" конуса, но не переворачивать его.
                    float cone0 = tt0; // 0=top, 1=bottom
                    float cone1 = tt1;
                    float pulse = Math.abs(cocoonSin);

                    float r0 = Math.max(0.05f, radius * layerRadiusMul * (1.0f + cocoonAmp * pulse * cone0));
                    float r1 = Math.max(0.05f, radius * layerRadiusMul * (1.0f + cocoonAmp * pulse * cone1));

                    // Мягкая альфа по высоте: ярче в середине, мягче на концах
                    float mid0 = 1.0f - Math.abs(2.0f * tt0 - 1.0f);
                    float mid1 = 1.0f - Math.abs(2.0f * tt1 - 1.0f);
                    float hA0 = 0.35f + 0.65f * mid0;
                    float hA1 = 0.35f + 0.65f * mid1;

                    // Градиент между двумя цветами по высоте
                    int r0c = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * tt0);
                    int g0c = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * tt0);
                    int b0c = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * tt0);
                    int r1c = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * tt1);
                    int g1c = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * tt1);
                    int b1c = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * tt1);

                    int core0 = new Color(r0c, g0c, b0c, 1).getRGB();
                    int core1 = new Color(r1c, g1c, b1c, 1).getRGB();
                    int bloom0 = new Color(r0c, g0c, b0c, clamp255(255 * (jelloGlow ? jelloGlowIntensity : 1.0f))).getRGB();
                    int bloom1 = new Color(r1c, g1c, b1c, clamp255(255 * (jelloGlow ? jelloGlowIntensity : 1.0f))).getRGB();

                    int c0 = applyAlphaToColor(applyAlphaToColor(core0, layerAlpha), hA0);
                    int b0 = applyAlphaToColor(applyAlphaToColor(bloom0, layerAlpha), hA0);
                    int c1v = applyAlphaToColor(applyAlphaToColor(core1, layerAlpha), hA1);
                    int b1v = applyAlphaToColor(applyAlphaToColor(bloom1, layerAlpha), hA1);

                    BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= segments; ++i) {
                        double angle = (i / (double) segments) * (Math.PI * 2.0);
                        float cos = (float) Math.cos(angle);
                        float sin = (float) Math.sin(angle);

                        // Верх ближе к core, низ ближе к bloom, но с градиентом
                        buffer.vertex(matrix, cos * r0, yy0, sin * r0).color(c0);
                        buffer.vertex(matrix, cos * r1, yy1, sin * r1).color(b1v);
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer.end());
                }
            }
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