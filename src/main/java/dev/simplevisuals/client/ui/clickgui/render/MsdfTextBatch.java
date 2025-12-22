package dev.simplevisuals.client.ui.clickgui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.render.msdf.MsdfFont;
import dev.simplevisuals.client.render.providers.ResourceProvider;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import java.awt.*;

final class MsdfTextBatch {

    private static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("msdf_font"),
            VertexFormats.POSITION_TEXTURE_COLOR,
            Defines.EMPTY
    );

    private MsdfFont font;
    private BufferBuilder builder;
    private boolean begun;

    private float thickness = 0.05f;
    private float smoothness = 0.5f;
    private float spacing = 0.0f;

    void begin(MsdfFont font) {
        if (begun && this.font == font) return;
        flush();
        this.font = font;
        this.builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.begun = true;
    }

    void setParams(float thickness, float smoothness, float spacing) {
        this.thickness = thickness;
        this.smoothness = smoothness;
        this.spacing = spacing;
    }

    void draw(Matrix4f matrix, MsdfFont font, String text, float size, float x, float y, float z, int argb) {
        if (text == null || text.isEmpty() || font == null) return;
        begin(font);
        float baseline = font.getMetrics().baselineHeight() * size;
        float thicknessSpacing = (thickness) * 0.5f * size;
        font.applyGlyphs(matrix, builder, text, size, thicknessSpacing, spacing, x, y + baseline, z, argb);
    }

    void draw(Matrix4f matrix, dev.simplevisuals.client.util.renderer.fonts.Instance instance, String text, float x, float y, int argb) {
        if (instance == null) return;
        draw(matrix, instance.font(), text, instance.size(), x, y, 0f, argb);
    }

    void flush() {
        if (!begun) {
            return;
        }
        
        MsdfFont currentFont = font;
        BufferBuilder currentBuilder = builder;
        
        begun = false;
        builder = null;
        font = null;
        
        if (currentBuilder == null || currentFont == null) {
            return;
        }

        // Try to end the buffer
        BuiltBuffer built = null;
        try {
            built = currentBuilder.end();
        } catch (IllegalStateException e) {
            // Buffer was empty, nothing to draw
            return;
        }

        if (built == null) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, currentFont.getTextureId());

        ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
        shader.getUniform("Range").set(currentFont.getAtlas().range());
        shader.getUniform("Thickness").set(thickness);
        shader.getUniform("Smoothness").set(smoothness);
        shader.getUniform("Outline").set(0);

        BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static int argb(Color c) {
        if (c == null) return 0;
        return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }
}
