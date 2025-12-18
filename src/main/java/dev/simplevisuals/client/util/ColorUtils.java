package dev.simplevisuals.client.util;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.util.Wrapper;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.ByteBuffer;

public final class ColorUtils implements Wrapper {

    private ColorUtils() {}

    public static Color picker(float x, float y) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4);

        GL11.glReadPixels(
                (int) (x * mc.getWindow().getScaleFactor()),
                (int) ((mc.getWindow().getScaledHeight() - y) * mc.getWindow().getScaleFactor()),
                1, 1,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buffer
        );

        return new Color(buffer.get() & 0xFF, buffer.get() & 0xFF, buffer.get() & 0xFF);
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color fade(Color color1, Color color2, float alpha) {
        int r = (int) (color1.getRed() * (1 - alpha) + color2.getRed() * alpha);
        int g = (int) (color1.getGreen() * (1 - alpha) + color2.getGreen() * alpha);
        int b = (int) (color1.getBlue() * (1 - alpha) + color2.getBlue() * alpha);
        int a = (int) (color1.getAlpha() * (1 - alpha) + color2.getAlpha() * alpha);

        return new Color(r, g, b, a);
    }

    public static Color pulse(Color color, long speed) {
        speed = MathHelper.clamp(speed, 0, 30);

        double sin = Math.sin(Math.TAU * (speed / 30f) * ((System.currentTimeMillis() - NexusVisual.getInstance().getInitTime()) / 1000f));
        double scale = (sin + 1f) / 2f;
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * scale));
    }

    public static Color offset(Color color, float alpha) {
        alpha = MathHelper.clamp(alpha, 0, 1);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alpha));
    }

    public static Color getGlobalColor(int alpha) {
        return new Color(68, 91, 150, alpha);
    }

    public static Color getGlobalColor() {
        return getGlobalColor(255);
    }
    
    public static Color gradient(Color color1, Color color2, float amount) {
        amount = MathHelper.clamp(amount, 0, 1);
        int r = MathHelper.lerp(amount, color1.getRed(), color2.getRed());
        int g = MathHelper.lerp(amount, color1.getGreen(), color2.getGreen());
        int b = MathHelper.lerp(amount, color1.getBlue(), color2.getBlue());
        int a = MathHelper.lerp(amount, color1.getAlpha(), color2.getAlpha());

        return new Color(r, g, b, a);
    }

    // Утилиты для работы с RGB цветами
    public static int rgb(int r, int g, int b) {
        return new Color(r, g, b).getRGB();
    }

    public static int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int getBlue(int rgb) {
        return rgb & 0xFF;
    }

    public static int getAlpha(int rgba) {
        return (rgba >> 24) & 0xFF;
    }
}