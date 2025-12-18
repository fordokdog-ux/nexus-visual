package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.resource.language.I18n;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import java.awt.Color;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;
import net.minecraft.client.gl.ShaderProgramKeys;

public class CustomHitBox extends Module implements ThemeManager.ThemeChangeListener {

    public @NotNull BooleanSetting fill = new BooleanSetting("setting.fill", true);
    public @NotNull NumberSetting lineWidth = new NumberSetting("setting.lineWidth", 1.5f, 0.5f, 6.0f, 0.1f);
    public @NotNull NumberSetting fillAlpha = new NumberSetting("setting.fillAlpha", 90, 0, 255, 1, () -> fill.getValue());
    public @NotNull NumberSetting outlineAlpha = new NumberSetting("setting.outlineAlpha", 255, 0, 255, 1);

    public @NotNull BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    public @NotNull ColorSetting color = new ColorSetting("Цвет", new Color(120, 200, 255, 255).getRGB());

    private final ThemeManager themeManager;
    private boolean prevRenderHitboxes;

    public CustomHitBox() {
        super("CustomHitBox", Category.Render, I18n.translate("module.customhitbox.description"));
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) { }

    @Override
    public void onEnable() {
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        this.prevRenderHitboxes = dispatcher.shouldRenderHitboxes();
        dispatcher.setRenderHitboxes(true);
        super.onEnable();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        renderCustomHitBoxDirect(e.getMatrices(), e.getTickDelta());
    }

    private void renderCustomHitBoxDirect(MatrixStack matrices, float tickDelta) {
        boolean wasDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            // Skip invisible entities (flag) or those with Invisibility effect
            if (entity.isInvisible()) continue;
            if (entity instanceof LivingEntity le && le.hasStatusEffect(StatusEffects.INVISIBILITY)) continue;

            Box worldBox = entity.getBoundingBox().expand(0.002);
            Vec3d interpolatedPos = entity.getLerpedPos(tickDelta);
            Box interpolatedBox = worldBox.offset(interpolatedPos.subtract(entity.getPos()));

            Color base = useThemeColor.getValue() ? ThemeManager.getInstance().getCurrentTheme().getBackgroundColor() : color.getColor();

            if (fill.getValue()) {
                Color fillColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), fillAlpha.getValue().intValue());
                renderEntityBoxFill(matrices, interpolatedBox, fillColor);
            }

            Color outlineColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), outlineAlpha.getValue().intValue());
            renderEntityBoxOutline(matrices, interpolatedBox, outlineColor, lineWidth.getValue().floatValue());
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        if (!wasDepthTestEnabled) {
            RenderSystem.disableDepthTest();
        }
        if (!wasBlendEnabled) {
            RenderSystem.disableBlend();
        }
    }

    private void renderEntityBoxFill(MatrixStack matrices, Box box, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderEntityBoxOutline(MatrixStack matrices, Box box, Color color, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        Vec3d boxCenter = box.getCenter();
        double distance = camera.distanceTo(boxCenter);
        float adjustedLineWidth = Math.max(0.5f, lineWidth * (float) (10.0 / Math.max(distance, 1.0)));
        GL11.glLineWidth(adjustedLineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        // Bottom face (y = minY)
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

        // Top face (y = maxY)
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        // Vertical edges
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    @Override
    public void onDisable() {
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderHitboxes(prevRenderHitboxes);
        super.onDisable();
    }
}
