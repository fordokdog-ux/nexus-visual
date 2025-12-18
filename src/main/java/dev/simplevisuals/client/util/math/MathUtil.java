package dev.simplevisuals.client.util.math;

import dev.simplevisuals.client.util.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.util.math.ColorHelper.*;

public class MathUtil implements Wrapper {
    public static double round(double num, double increment) {
        double v = (double) Math.round(num / increment) * increment;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }
    public static boolean isHovered(double mouseX,
                                    double mouseY,
                                    double x,
                                    double y,
                                    double width,
                                    double height) {
        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + height;
    }
    public static int applyOpacity(int color, float opacity) {
        return ColorHelper.getArgb((int) (getAlpha(color) * opacity / 255), getRed(color), getGreen(color),
                getBlue(color));
    }
    public static float fast(float end, float start, float multiple) {
        return (1 - clamp(deltaTime() * multiple, 0, 1)) * end + clamp(deltaTime() * multiple, 0, 1) * start;
    }
    public static float deltaTime() {
        return (float) (mc.getRenderTickCounter().getTickDelta(true) / 20.0f);
    }
    public static float clamp(float val, float min, float max) {
        if (val <= min) {
            val = min;
        }
        if (val >= max) {
            val = max;
        }
        return val;
    }

    public static void scale(MatrixStack stack,
                             float x,
                             float y,
                             float scale,
                             Runnable data) {

        stack.push();
        stack.translate(x, y, 0);
        stack.scale(scale, scale, 1);
        stack.translate(-x, -y, 0);
        data.run();
        stack.pop();
    }

    public static double computeGcd() {
        double f = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15;
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                double d = min;
                min = max;
                max = d;
            }

            return ThreadLocalRandom.current().nextDouble(min, max);
        }
    }

    public static Color injectAlpha(final Color color, final int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
    }

    public static int interpolateColor(int startRGB, int endRGB, float progress) {
        int startR = (startRGB >> 16) & 0xFF;
        int startG = (startRGB >> 8) & 0xFF;
        int startB = startRGB & 0xFF;

        int endR = (endRGB >> 16) & 0xFF;
        int endG = (endRGB >> 8) & 0xFF;
        int endB = endRGB & 0xFF;

        int r = (int) (startR + (endR - startR) * progress);
        int g = (int) (startG + (endG - startG) * progress);
        int b = (int) (startB + (endB - startB) * progress);

        return (r << 16) | (g << 8) | b;
    }
    public static Vector4f calculateRotationFromCamera(LivingEntity target) {
        Vec3d vec = target.getPos().subtract(mc.player.getEyePos());

        float rawYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90F);
        float rawPitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)))));
        float yawDelta = MathHelper.wrapDegrees(rawYaw - mc.player.getYaw());
        float pitchDelta = rawPitch - mc.player.getPitch();
        //float yawDelta = MathHelper.wrapDegrees(rawYaw - Rotation.getRealYaw());
        //float pitchDelta = rawPitch - Rotation.getRealPitch();

        return new Vector4f(rawYaw, rawPitch, yawDelta, pitchDelta);
    }

    public static double calculateFOVFromCamera(LivingEntity target) {
        Vector4f rotation = calculateRotationFromCamera(target);
        float yawDelta = rotation.z;
        float pitchDelta = rotation.w;

        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    public static double getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }

    public static int setAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static void scaleStart(MatrixStack poseStack, float x, float y, float scale) {
        poseStack.push();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1);
        poseStack.translate(- x, - y, 0);
    }

    public static void scaleEnd(MatrixStack poseStack) {
        poseStack.pop();
    }

    public static double distanceToSqr(double pX, double pY, double pZ) {
        double d0 = mc.player.getX() - pX;
        double d1 = mc.player.getY() - pY;
        double d2 = mc.player.getZ() - pZ;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public static int reAlphaInt(int color, int alpha) {
        alpha = MathHelper.clamp(alpha, 0, 255);
        int rgb = color & 0x00FFFFFF;
        return (alpha << 24) | rgb;
    }

    public static void executeOnMainThread(Runnable runnable) {
        mc.execute(runnable);
    }
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}
