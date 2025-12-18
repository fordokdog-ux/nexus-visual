package dev.simplevisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.NexusVisual;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

import static dev.simplevisuals.client.util.Wrapper.mc;

public class SantaHat extends Module {

    private final NumberSetting size = new NumberSetting("setting.size", 0.55f, 0.2f, 1.2f, 0.05f);
    private final NumberSetting yOffset = new NumberSetting("setting.yOffset", 0.00f, -0.25f, 0.35f, 0.01f);
    private final NumberSetting alpha = new NumberSetting("setting.alpha", 1.0f, 0.1f, 1.0f, 0.1f);

    public SantaHat() {
        super("SantaHat", Category.Render, net.minecraft.client.resource.language.I18n.translate("module.santahat.description"));

        getSettings().add(size);
        getSettings().add(yOffset);
        getSettings().add(alpha);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        // Only local player; skip in first person
        PlayerEntity p = mc.player;
        if (p == null) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        renderHatForPlayer(e.getMatrices(), p, e.getTickDelta());
    }

    private void renderHatForPlayer(MatrixStack matrices, PlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);

        matrices.push();

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float eyeHeight = player.getEyeHeight(player.getPose());
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

        float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

        float basePivotY = eyeHeight - 0.15f;
        matrices.translate(0.0, basePivotY, 0.0);

        float headYawAbs = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
        float netHeadYaw = headYawAbs - bodyYaw;
        float headPitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(headPitch));

        float baseToCrown = player.isInSneakingPose() ? 0.305f : 0.265f;
        matrices.translate(0.0, baseToCrown + yOffset.getValue().floatValue(), 0.0);

        float s = size.getValue().floatValue();
        renderCrossQuadsHat(matrices, s, s * 1.05f, alpha.getValue().floatValue());

        matrices.pop();
    }

    private void renderCrossQuadsHat(MatrixStack matrices, float width, float height, float alphaMul) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, NexusVisual.id("textures/santa.png"));

        float a = MathHelper.clamp(alphaMul, 0.0f, 1.0f);
        int rgba = ((Math.round(a * 255f) & 0xFF) << 24) | 0xFFFFFF;

        Matrix4f m = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();

        float w = width;
        float h = height;
        float yLift = 0.015f;

        BufferBuilder bb = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // Two crossed textured quads. Looks cleaner than the old blocky model.
        addTexturedQuad(bb, m,
            -w / 2f, yLift, 0f,
            w / 2f, yLift, 0f,
            w / 2f, yLift + h, 0f,
            -w / 2f, yLift + h, 0f,
            rgba);

        addTexturedQuad(bb, m,
            0f, yLift, -w / 2f,
            0f, yLift, w / 2f,
            0f, yLift + h, w / 2f,
            0f, yLift + h, -w / 2f,
            rgba);

        BufferRenderer.drawWithGlobalProgram(bb.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addTexturedQuad(BufferBuilder bb, Matrix4f m,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float x4, float y4, float z4,
                                        int rgba) {
        bb.vertex(m, x1, y1, z1).texture(0f, 1f).color(rgba);
        bb.vertex(m, x2, y2, z2).texture(1f, 1f).color(rgba);
        bb.vertex(m, x3, y3, z3).texture(1f, 0f).color(rgba);
        bb.vertex(m, x4, y4, z4).texture(0f, 0f).color(rgba);
    }
}
