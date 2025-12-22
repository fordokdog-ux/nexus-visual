package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.events.impl.EventTick;
import dev.simplevisuals.client.events.impl.EventThemeChanged;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.Render3D;
import dev.simplevisuals.client.util.perf.Perf;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.managers.ThemeManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.resource.language.I18n;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JumpCircle extends Module implements ThemeManager.ThemeChangeListener {

    // 👇 сильно уменьшаем размер
    private final NumberSetting size = new NumberSetting("Circle Size", 0.5f, 0.2f, 2.0f, 0.05f);
    private final NumberSetting Life_Time = new NumberSetting("Life Time", 0.5f, 0.2f, 2.0f, 0.05f);
    private final NumberSetting speed = new NumberSetting("Speed", 1.0f, 0.5f, 3.0f, 0.1f);

    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(120, 200, 255, 255).getRGB());

    private static final long GROW_MS = 600L;

    // Текстуры: вариант 1 — jumpcircle.png, вариант 2 — circle.png
    private final BooleanSetting texJumpCircle = new BooleanSetting("First", true, () -> false);
    private final BooleanSetting texCircle = new BooleanSetting("Second", false, () -> false);
    private final ListSetting textureMode = new ListSetting("Texture", true, texJumpCircle, texCircle);

    // Анимации: Grow, Pulse, Ripple
    private final BooleanSetting animGrow = new BooleanSetting("Grow", true, () -> false);
    private final BooleanSetting animPulse = new BooleanSetting("Pulse", false, () -> false);
    private final BooleanSetting animRipple = new BooleanSetting("Ripple", false, () -> false);
    private final ListSetting animationMode = new ListSetting("Animation", true, animGrow, animPulse, animRipple);

    private final Identifier texJump = NexusVisual.id("textures/jumpcircle.png");
    private final Identifier texCircle2 = NexusVisual.id("textures/circle.png");

    private final List<Circle> circles = new CopyOnWriteArrayList<>();
    private final ThemeManager themeManager;
    private Color currentColor;

    private boolean wasOnGround = true;
    private long lastJumpTime = 0;

    public JumpCircle() {
        super("JumpCircle", Category.Render, I18n.translate("module.jumpcircle.description"));
        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = themeManager.getBackgroundColor();
    }

    @EventHandler
    public void onThemeChanged(EventThemeChanged event) {
        this.currentColor = themeManager.getBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        circles.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        boolean onGround = p.isOnGround();
        long currentTime = System.currentTimeMillis();
        double velocityY = p.getVelocity().y;

        // Упрощенная логика: игрок был на земле, теперь не на земле, и есть положительная скорость
        if (wasOnGround && !onGround && velocityY > 0.01 && (currentTime - lastJumpTime) > 50) {
            // Создаем круг на позиции прыжка, но на 1 блок выше
            BlockPos blockPos = p.getBlockPos();
            double y = blockPos.getY() + 0.1; // На 1 блок выше + небольшой отступ
            Vec3d origin = new Vec3d(p.getX(), y, p.getZ());
            circles.add(new Circle(origin, (long) (Life_Time.getValue() * 1000L)));
            lastJumpTime = currentTime;
        }

        wasOnGround = onGround;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("JumpCircle.onRender3D")) {
            long now = System.currentTimeMillis();
            circles.removeIf(c -> (now - c.spawnTime) > c.ttl);

            for (Circle c : circles) {
                long age = now - c.spawnTime;

                // Нормированная величина времени жизни 0..1 с учетом скорости
                float t = Math.max(0f, Math.min(1f, (age / (float) c.ttl) * speed.getValue()));

                // Вычисляем масштаб и прозрачность в зависимости от режима
                float radiusMul;
                float alphaK;
                if (animPulse.getValue()) {
                    // Пульсация: несколько «всплесков» в течение жизни
                    float pulses = 2.0f; // количество пульсов
                    float s = (float) Math.sin(Math.PI * pulses * t);
                    radiusMul = 0.7f + 0.5f * Math.max(0f, s);
                    alphaK = Math.max(0f, s) * (1.0f - t);
                } else if (animRipple.getValue()) {
                    // Ripple: мягкое появление/исчезновение и ease-out по радиусу
                    float te = Easing.EASE_OUT_CIRC.apply(Math.max(0f, Math.min(1f, t)));
                    radiusMul = 0.6f + 0.8f * te;
                    alphaK = (float) Math.pow(Math.sin(Math.max(0f, Math.min(1f, t)) * (float) Math.PI), 1.2f);
                } else {
                    // Grow: используем плавный S-curve по времени + эксп. затухание
                    float te = Easing.BOTH_SINE.apply(t);
                    radiusMul = 0.6f + 0.4f * te;
                    alphaK = (float) Math.pow(1.0f - t, 0.8f) * te;
                }

                alphaK = Math.max(0f, Math.min(1f, alphaK));
                int alpha = (int) (255 * alphaK);
                if (alpha <= 2) continue;

                float r = size.getValue() * radiusMul;
                Color base = useThemeColor.getValue() ? themeManager.getBackgroundColor() : color.getColor();
                Color drawColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

                MatrixStack matrices = e.getMatrices();
                matrices.push();
                matrices.translate(
                        c.origin.x - mc.getEntityRenderDispatcher().camera.getPos().x,
                        c.origin.y - mc.getEntityRenderDispatcher().camera.getPos().y,
                        c.origin.z - mc.getEntityRenderDispatcher().camera.getPos().z
                );

                Identifier texture = getSelectedTexture();
                Render3D.drawTextureVivid(
                        matrices,
                        -r, 0, -r,
                        r, 0, r,
                        texture,
                        drawColor,
                        1.0f
                );

                matrices.pop();
            }
        }
    }

    private Identifier getSelectedTexture() {
        if (texJumpCircle.getValue() && texCircle.getValue()) {
            texCircle.setValue(false);
        }
        if (!texJumpCircle.getValue() && !texCircle.getValue()) {
            texJumpCircle.setValue(true);
        }
        return texJumpCircle.getValue() ? texJump : texCircle2;
    }

    private static class Circle {
        final Vec3d origin;
        final long spawnTime;
        final long ttl;

        Circle(Vec3d origin, long ttl) {
            this.origin = origin;
            this.spawnTime = System.currentTimeMillis();
            this.ttl = ttl;
        }
    }
}