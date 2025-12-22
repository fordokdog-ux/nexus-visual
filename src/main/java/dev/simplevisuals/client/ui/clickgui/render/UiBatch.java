package dev.simplevisuals.client.ui.clickgui.render;

import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.*;

import static net.minecraft.client.render.VertexFormat.DrawMode;

final class UiBatch {

    private final DrawMode drawMode;
    private final VertexFormat vertexFormat;
    private BufferBuilder builder;
    private boolean begun;
    private int vertexCount;

    UiBatch() {
        this(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    UiBatch(DrawMode drawMode, VertexFormat vertexFormat) {
        this.drawMode = drawMode;
        this.vertexFormat = vertexFormat;
    }

    void begin() {
        if (begun) return;
        builder = Tessellator.getInstance().begin(drawMode, vertexFormat);
        begun = true;
        vertexCount = 0;
    }

    boolean isBegun() {
        return begun;
    }

    boolean hasVertices() {
        return begun && vertexCount > 0;
    }

    void reset() {
        // Reset without drawing - just clear state
        begun = false;
        vertexCount = 0;
        builder = null;
    }

    void endAndDraw() {
        if (!begun) return;
        begun = false;
        int count = vertexCount;
        vertexCount = 0;
        
        if (builder == null) return;
        
        // Always try to end the buffer to release Tessellator
        try {
            BuiltBuffer built = builder.end();
            // Only draw if we actually have vertices
            if (built != null && count > 0) {
                BufferRenderer.drawWithGlobalProgram(built);
            } else if (built != null) {
                built.close();
            }
        } catch (IllegalStateException e) {
            // Buffer was empty, ignore
        }
        builder = null;
    }

    void triangle(Matrix4f matrix, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        begin();
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        builder.vertex(matrix, x1, y1, 0).color(r, g, b, a);
        builder.vertex(matrix, x2, y2, 0).color(r, g, b, a);
        builder.vertex(matrix, x3, y3, 0).color(r, g, b, a);
        vertexCount += 3;
    }

    void triangle(Matrix4f matrix,
                  float x1, float y1, int c1,
                  float x2, float y2, int c2,
                  float x3, float y3, int c3) {
        begin();
        builder.vertex(matrix, x1, y1, 0).color(
            ((c1 >>> 16) & 0xFF) / 255f,
            ((c1 >>> 8) & 0xFF) / 255f,
            (c1 & 0xFF) / 255f,
            ((c1 >>> 24) & 0xFF) / 255f
        );
        builder.vertex(matrix, x2, y2, 0).color(
            ((c2 >>> 16) & 0xFF) / 255f,
            ((c2 >>> 8) & 0xFF) / 255f,
            (c2 & 0xFF) / 255f,
            ((c2 >>> 24) & 0xFF) / 255f
        );
        builder.vertex(matrix, x3, y3, 0).color(
            ((c3 >>> 16) & 0xFF) / 255f,
            ((c3 >>> 8) & 0xFF) / 255f,
            (c3 & 0xFF) / 255f,
            ((c3 >>> 24) & 0xFF) / 255f
        );
        vertexCount += 3;
    }

    void quad(Matrix4f matrix, float x, float y, float w, float h, int c1, int c2, int c3, int c4) {
        // 2 triangles: (x,y) (x,y+h) (x+w,y+h) and (x,y) (x+w,y+h) (x+w,y)
        begin();
        builder.vertex(matrix, x, y, 0).color(
            ((c1 >>> 16) & 0xFF) / 255f,
            ((c1 >>> 8) & 0xFF) / 255f,
            (c1 & 0xFF) / 255f,
            ((c1 >>> 24) & 0xFF) / 255f
        );
        builder.vertex(matrix, x, y + h, 0).color(
            ((c2 >>> 16) & 0xFF) / 255f,
            ((c2 >>> 8) & 0xFF) / 255f,
            (c2 & 0xFF) / 255f,
            ((c2 >>> 24) & 0xFF) / 255f
        );
        builder.vertex(matrix, x + w, y + h, 0).color(
            ((c3 >>> 16) & 0xFF) / 255f,
            ((c3 >>> 8) & 0xFF) / 255f,
            (c3 & 0xFF) / 255f,
            ((c3 >>> 24) & 0xFF) / 255f
        );

        builder.vertex(matrix, x, y, 0).color(
            ((c1 >>> 16) & 0xFF) / 255f,
            ((c1 >>> 8) & 0xFF) / 255f,
            (c1 & 0xFF) / 255f,
            ((c1 >>> 24) & 0xFF) / 255f
        );
        builder.vertex(matrix, x + w, y + h, 0).color(
            ((c3 >>> 16) & 0xFF) / 255f,
            ((c3 >>> 8) & 0xFF) / 255f,
            (c3 & 0xFF) / 255f,
            ((c3 >>> 24) & 0xFF) / 255f
        );
        builder.vertex(matrix, x + w, y, 0).color(
            ((c4 >>> 16) & 0xFF) / 255f,
            ((c4 >>> 8) & 0xFF) / 255f,
            (c4 & 0xFF) / 255f,
            ((c4 >>> 24) & 0xFF) / 255f
        );

        vertexCount += 6;
    }

    void quad(Matrix4f matrix, float x, float y, float w, float h, int argb) {
        quad(matrix, x, y, w, h, argb, argb, argb, argb);
    }

    static int withAlpha(int argb, int alpha) {
        alpha = MathHelper.clamp(alpha, 0, 255);
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    static int argb(Color c) {
        if (c == null) return 0;
        return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }
}
