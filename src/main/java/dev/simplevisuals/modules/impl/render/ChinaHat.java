package dev.simplevisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.FriendsManager;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.api.Nameable;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.EnumSetting;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
 

import static dev.simplevisuals.client.util.Wrapper.mc;

public class ChinaHat extends Module implements ThemeManager.ThemeChangeListener {

    private final NumberSetting brimRadius = new NumberSetting("setting.brimRadius", 0.7f, 0.3f, 1.4f, 0.05f);
    private final BooleanSetting showFriends = new BooleanSetting("setting.showFriends", true);
    private final NumberSetting opacity = new NumberSetting("setting.opacity", 0.65f, 0.0f, 1.0f, 0.01f);
    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 80, 80, 255).getRGB());
    private final ColorSetting colorSecondary = new ColorSetting("Цвет 2", new Color(80, 160, 255, 255).getRGB());
    
    private static final int FIXED_SEGMENTS = 96;
    private final ThemeManager themeManager;
    private Color currentColor;
    private Color secondaryColor;

    public ChinaHat() {
        super("ChinaHat", Category.Render, net.minecraft.client.resource.language.I18n.translate("module.chinahat.description"));
        this.themeManager = ThemeManager.getInstance();
        this.currentColor = color.getColor();
        this.secondaryColor = colorSecondary.getColor();
        themeManager.addThemeChangeListener(this);

        getSettings().add(brimRadius);
        getSettings().add(opacity);
        getSettings().add(showFriends);
        getSettings().add(useThemeColor);
        getSettings().add(color);
        getSettings().add(colorSecondary);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        if (!useThemeColor.getValue()) return;
        this.currentColor = themeManager.getBackgroundColor();
        this.secondaryColor = themeManager.getSecondaryBackgroundColor();
    }

    @EventHandler
    public void onThemeChanged(dev.simplevisuals.client.events.impl.EventThemeChanged event) {
        if (!useThemeColor.getValue()) return;
        this.currentColor = themeManager.getBackgroundColor();
        this.secondaryColor = themeManager.getSecondaryBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        float tickDelta = e.getTickDelta();

        MatrixStack matrices = e.getMatrices();
        for (PlayerEntity p : mc.world.getPlayers()) {
            // Skip if it's the local player and we're in first person
            if (p == mc.player && mc.options.getPerspective().isFirstPerson()) continue;

            // For local player: always show (except in first person)
            if (p == mc.player) {
                renderHatForPlayer(matrices, p, tickDelta);
                continue;
            }

            // For other players: only show if they're friends and showFriends is enabled
            if (FriendsManager.checkFriend(p.getGameProfile().getName()) && showFriends.getValue()) {
                renderHatForPlayer(matrices, p, tickDelta);
            }
        }
    }

    private void renderHatForPlayer(MatrixStack matrices, PlayerEntity player, float tickDelta) {
        // Resolve colors (theme-independent by default)
        if (useThemeColor.getValue()) {
            ThemeManager.Theme t = themeManager.getCurrentTheme();
            currentColor = t.getBackgroundColor();
            secondaryColor = t.getSecondaryBackgroundColor();
        } else {
            currentColor = color.getColor();
            secondaryColor = colorSecondary.getColor();
        }

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
        // Bind to head yaw and pitch so the hat follows head orientation
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(headPitch));
        float baseToCrown = player.isInSneakingPose() ? 0.305f : 0.265f;
        float pitchRad = (float) Math.toRadians(MathHelper.clamp(headPitch, -90.0f, 90.0f));
        float cosPitch = MathHelper.clamp((float) Math.cos(pitchRad), 0.0f, 1.0f);
        float tiltFactor = 1.0f - cosPitch; // 0 at 0°, 1 at ±90°
        float upMul = headPitch < 0.0f ? 1.6f : 1.0f; // stronger adjustment when looking up

        float clearance = 0.03f * cosPitch + (headPitch < 0.0f ? 0.008f * tiltFactor : 0.0f);
        float dynamicOffset = -0.05f + 0.04f + 0.08f * tiltFactor * upMul;
        float forwardOffset = -0.06f * tiltFactor * Math.signum(headPitch) * upMul;
        matrices.translate(0.0, (baseToCrown + clearance) + dynamicOffset, forwardOffset);

        renderConeHatHollow(matrices,
                brimRadius.getValue().floatValue(),
                0.35f,
                currentColor,
                secondaryColor,
                FIXED_SEGMENTS);

        matrices.pop();
    }

    private void renderConeHatHollow(MatrixStack matrices, float radius, float height, Color tipColor, Color baseColor, int segs) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Matrix4f m = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();

        float tr = tipColor.getRed() / 255f;
        float tg = tipColor.getGreen() / 255f;
        float tb = tipColor.getBlue() / 255f;

        float br = baseColor.getRed() / 255f;
        float bg = baseColor.getGreen() / 255f;
        float bb = baseColor.getBlue() / 255f;

        float tipY = height;
        BufferBuilder coneStrip = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        float tipAlpha = MathHelper.clamp(opacity.getValue().floatValue(), 0.0f, 1.0f);
        float baseAlpha = MathHelper.clamp(tipAlpha * 0.7f, 0.0f, 1.0f);
        for (int i = 0; i <= segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            float x = (float) (Math.cos(a) * radius);
            float z = (float) (Math.sin(a) * radius);
            // Основание слегка уходит в secondaryColor, вершина — в основной цвет
            coneStrip.vertex(m, x, 0f, z).color(br, bg, bb, baseAlpha);
            coneStrip.vertex(m, 0f, tipY, 0f).color(tr, tg, tb, tipAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(coneStrip.end());

        // Тонкая обводка по краю шляпы (brim) — делает форму аккуратнее
        float outlineA = MathHelper.clamp(tipAlpha * 0.9f, 0.0f, 1.0f);
        BufferBuilder rim = tess.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            float x = (float) (Math.cos(a) * radius);
            float z = (float) (Math.sin(a) * radius);
            rim.vertex(m, x, 0.0f, z).color(tr, tg, tb, outlineA);
        }
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.35f);
        BufferRenderer.drawWithGlobalProgram(rim.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GL11.glEnable(GL11.GL_CULL_FACE);
    }
}