package dev.simplevisuals.client.render.renderers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.NexusVisual;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;

import static dev.simplevisuals.client.util.Wrapper.mc;

public class SoulRenderer {

    private final ThemeManager themeManager;

    public SoulRenderer(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    public void render(EventRender3D.Game e,
                       LivingEntity target,
                       int ghostsLength,
                       float ghostsWidth,
                       float ghostsSpeed,
                       float ghostsAngleStep,
                       boolean ghostsGlow,
                       double ghostsGlowIntensity,
                       float animationValue,
                       Vec3d fadeOrigin,
                       float lastKnownHeight,
                       float lastKnownWidth,
                       float alphaMultiplier,
                       Color overrideColor) {
        if (target == null && animationValue <= 0) return;

        var camera = mc.gameRenderer.getCamera();

        // Если target null, используем fadeOrigin из TargetEsp
        double entX, entY, entZ;
        float iAge;
        float height, radius;
        
        if (target != null) {
            entX = target.prevX + (target.getX() - target.prevX) * e.getTickDelta();
            entY = target.prevY + (target.getY() - target.prevY) * e.getTickDelta();
            entZ = target.prevZ + (target.getZ() - target.prevZ) * e.getTickDelta();
            iAge = (float) (target.age - 1 + (target.age - (target.age - 1)) * e.getTickDelta());
            height = target.getHeight();
            radius = target.getWidth();
        } else if (fadeOrigin != null) {
            // Для fadeOrigin используем переданные координаты и размеры
            // fadeOrigin уже содержит центр сущности, поэтому не нужно добавлять height/2
            entX = fadeOrigin.x;
            entY = fadeOrigin.y;
            entZ = fadeOrigin.z;
            iAge = 0;
            height = lastKnownHeight;
            radius = lastKnownWidth;
        } else {
            return; // Нет ни target, ни fadeOrigin
        }

        double tPosX = entX - camera.getPos().x;
        double tPosY = entY - camera.getPos().y;
        double tPosZ = entZ - camera.getPos().z;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, NexusVisual.id("hud/bloom.png"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        boolean depth = mc.player != null && target != null && mc.player.canSee(target);
        if (depth) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        float ageMultiplier = iAge * 2.5f;
        Color themeColor = overrideColor != null ? overrideColor : themeManager.getThemeColor();

        int baseR = themeColor.getRed();
        int baseG = themeColor.getGreen();
        int baseB = themeColor.getBlue();

        float scale = ghostsWidth;
        float glowScale = scale * (1.0f + (float) ghostsGlowIntensity);

        // Параметры движения как в "призраках" (спираль/орбита)
        long nowMs = System.currentTimeMillis();
        float safeSpeed = Math.max(1f, ghostsSpeed);
        float angleStep = Math.max(0.0001f, ghostsAngleStep);
        int length = Math.max(1, ghostsLength);

        // Камера-ориентированная плоскость (billboard) без MatrixStack push/pop на каждую частицу
        Quaternionf camRot = new Quaternionf(camera.getRotation());
        Matrix4f identity = new Matrix4f();
        // Предвычисляем повернутые единичные углы (без масштаба), чтобы не аллоцировать Vector3f на каждую частицу
        Vector3f u1 = new Vector3f(-1f, 1f, 0f).rotate(camRot);
        Vector3f u2 = new Vector3f(1f, 1f, 0f).rotate(camRot);
        Vector3f u3 = new Vector3f(1f, -1f, 0f).rotate(camRot);
        Vector3f u4 = new Vector3f(-1f, -1f, 0f).rotate(camRot);

        // Центр сущности в координатах камеры
        float centerX = (float) tPosX;
        float centerY = (float) (tPosY + (target != null ? (height * 0.5) : 0.0));
        float centerZ = (float) tPosZ;

        // Radius орбиты: чуть больше ширины таргета, но не слишком маленький
        float orbitRadius = Math.max(0.35f, radius * 0.8f);
        // Сдвиг по Y, чтобы эффект был ближе к "центру" как в примере
        float yBase = centerY + Math.max(0.0f, height * 0.15f);

        // Glow pass (batched)
        if (ghostsGlow && glowScale > 0.0001f) {
            BufferBuilder glowBuffer = null;
            boolean glowHasVerts = false;
            for (int pass = 0; pass < 3; pass++) {
                for (int i = 0; i < length; i++) {
                    float t = length <= 1 ? 1f : (i / (float) (length - 1));
                    float fade = animationValue * t;
                    if (fade <= 0.0001f) continue;
                    fade = Math.min(1f, fade);
                    fade = fade * fade;

                    double angle = angleStep * ((nowMs) - (double) i * 12.0D) / (double) safeSpeed;
                    float s = (float) (Math.sin(angle) * orbitRadius);
                    float c = (float) (Math.cos(angle) * orbitRadius);

                    float offX;
                    float offY;
                    float offZ;
                    if (pass == 0) {
                        offX = s;
                        offY = c;
                        offZ = -c;
                    } else if (pass == 1) {
                        offX = -s;
                        offY = s;
                        offZ = -c;
                    } else {
                        offX = c;
                        offY = -s;
                        offZ = -s;
                    }

                    int a = clamp255(180 * ghostsGlowIntensity * fade * alphaMultiplier);
                    if (a <= 0) continue;
                    int rgba = (a & 0xFF) << 24 | (baseR & 0xFF) << 16 | (baseG & 0xFF) << 8 | (baseB & 0xFF);

                    if (glowBuffer == null) {
                        glowBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    }
                    addBillboardQuadScaled(glowBuffer, identity,
                            centerX + offX, yBase + offY, centerZ + offZ,
                            glowScale, u1, u2, u3, u4, rgba);
                    glowHasVerts = true;
                }
            }
            if (glowHasVerts && glowBuffer != null) {
                BufferRenderer.drawWithGlobalProgram(glowBuffer.end());
            }
        }

        // Main pass (batched)
        BufferBuilder buffer = null;
        boolean hasVerts = false;
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < length; i++) {
                float t = length <= 1 ? 1f : (i / (float) (length - 1));
                float fade = animationValue * t;
                if (fade <= 0.0001f) continue;
                fade = Math.min(1f, fade);
                fade = fade * fade;

                double angle = angleStep * ((nowMs) - (double) i * 12.0D) / (double) safeSpeed;
                float s = (float) (Math.sin(angle) * orbitRadius);
                float c = (float) (Math.cos(angle) * orbitRadius);

                float offX;
                float offY;
                float offZ;
                if (pass == 0) {
                    offX = s;
                    offY = c;
                    offZ = -c;
                } else if (pass == 1) {
                    offX = -s;
                    offY = s;
                    offZ = -c;
                } else {
                    offX = c;
                    offY = -s;
                    offZ = -s;
                }

                int a = clamp255(230 * fade * alphaMultiplier);
                if (a <= 0) continue;
                int rgba = (a & 0xFF) << 24 | (baseR & 0xFF) << 16 | (baseG & 0xFF) << 8 | (baseB & 0xFF);

                if (buffer == null) {
                    buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                }
                addBillboardQuadScaled(buffer, identity,
                        centerX + offX, yBase + offY, centerZ + offZ,
                        scale, u1, u2, u3, u4, rgba);
                hasVerts = true;
            }
        }
        if (hasVerts && buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        if (depth) RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addBillboardQuadScaled(BufferBuilder buffer,
                                               Matrix4f matrix,
                                               float cx, float cy, float cz,
                                               float scale,
                                               Vector3f u1, Vector3f u2, Vector3f u3, Vector3f u4,
                                               int rgba) {
        buffer.vertex(matrix, cx + u1.x * scale, cy + u1.y * scale, cz + u1.z * scale).texture(0f, 1f).color(rgba);
        buffer.vertex(matrix, cx + u2.x * scale, cy + u2.y * scale, cz + u2.z * scale).texture(1f, 1f).color(rgba);
        buffer.vertex(matrix, cx + u3.x * scale, cy + u3.y * scale, cz + u3.z * scale).texture(1f, 0f).color(rgba);
        buffer.vertex(matrix, cx + u4.x * scale, cy + u4.y * scale, cz + u4.z * scale).texture(0f, 0f).color(rgba);
    }

    private static int clamp255(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }
}


