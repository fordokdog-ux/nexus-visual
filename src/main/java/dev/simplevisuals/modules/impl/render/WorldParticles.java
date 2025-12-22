package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.client.util.renderer.Render3D;
import dev.simplevisuals.client.managers.ThemeManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WorldParticles extends Module implements ThemeManager.ThemeChangeListener {

    private final NumberSetting particleSize = new NumberSetting("Размер", 0.15f, 0.05f, 0.20f, 0.01f);
    private final NumberSetting maxParticles = new NumberSetting("Количество", 150f, 20f, 500f, 5f);
    private final NumberSetting spawnInterval = new NumberSetting("Интервал спавна", 60f, 10f, 200f, 10f);
    private final float spawnRadius = 10f;

    private final ListSetting mode = new ListSetting(
            "Режим",
            true,
            new BooleanSetting("Простой", true),
            new BooleanSetting("Взлет", false)
    );

    private final BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", false);
    private final BooleanSetting glossy = new BooleanSetting("Глянцевые", false);

    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(120, 200, 255, 255).getRGB());

    private static final Identifier GLOW = NexusVisual.id("hud/glow.png");

    private final ThemeManager themeManager;
    private Color currentColor;
    private final List<Particle> particles = new ArrayList<>();
    private long lastSpawnTime = 0;

    public WorldParticles() {
        super("Snow", Category.Render, "Красивые частицы снега");
        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();

        if (now - lastSpawnTime >= spawnInterval.getValue()) {
            spawnParticle();
            lastSpawnTime = now;
        }

        particles.removeIf(p -> {
            if (now - p.spawnTime > p.lifeTime) return true;
            p.updateMotion(isVzletMode());
            return p.pos.y < -2;
        });

        Render3D.beginBillboardBatch(GLOW);
        for (Particle p : particles) {
            if (!isInFrustum(p.pos)) continue;
            renderParticle(p, e, now);
        }
        Render3D.endBillboardBatch();
    }

    private void spawnParticle() {
        ensureSpace();

        double radius = spawnRadius;
        Vec3d pos;
        Vec3d vel;

        if (isVzletMode()) {
            pos = mc.player.getPos().add(
                    ThreadLocalRandom.current().nextDouble(-radius, radius),
                    0.2, // чуть выше ног
                    ThreadLocalRandom.current().nextDouble(-radius, radius)
            );
            vel = new Vec3d(
                    ThreadLocalRandom.current().nextDouble(-0.01, 0.01),
                    0.03,
                    ThreadLocalRandom.current().nextDouble(-0.01, 0.01)
            );
        } else {
            pos = mc.player.getPos().add(
                    ThreadLocalRandom.current().nextDouble(-radius, radius),
                    ThreadLocalRandom.current().nextDouble(5, 8),
                    ThreadLocalRandom.current().nextDouble(-radius, radius)
            );
            vel = new Vec3d(
                    ThreadLocalRandom.current().nextDouble(-0.01, 0.01),
                    -0.01,
                    ThreadLocalRandom.current().nextDouble(-0.01, 0.01)
            );
        }

        Color base = useThemeColor.getValue() ? themeManager.getBackgroundColor() : this.color.getColor();
        Color color = randomColor.getValue()
                ? new Color(ThreadLocalRandom.current().nextInt(256),
                ThreadLocalRandom.current().nextInt(256),
                ThreadLocalRandom.current().nextInt(256))
            : base;

        particles.add(new Particle(pos, vel, 5000, color));
    }

    private void ensureSpace() {
        while (particles.size() >= maxParticles.getValue() && !particles.isEmpty()) {
            particles.remove(0);
        }
    }

    private boolean isInFrustum(Vec3d pos) {
        return ((dev.simplevisuals.mixin.accessors.IWorldRenderer) mc.worldRenderer)
                .getFrustum()
                .isVisible(new net.minecraft.util.math.Box(pos.add(-0.2, -0.2, -0.2), pos.add(0.2, 0.2, 0.2)));
    }

    private void renderParticle(Particle p, EventRender3D.Game e, long now) {
        float baseSize = particleSize.getValue();
        float life = p.getLifeProgress(now);

        float sizeFactor;
        if (life < 0.2f) sizeFactor = life / 0.2f;
        else if (life > 0.8f) sizeFactor = (1 - life) / 0.2f;
        else sizeFactor = 1f;

        float heightFactor = 1f;
        if (!isVzletMode() && p.pos.y < 1.5) heightFactor = (float) (p.pos.y / 1.5);

        float lightFactor = mc.world != null && mc.player != null
                ? Math.max(0.5f, 1.0f - mc.world.getBrightness(mc.player.getBlockPos()))
                : 1f;

        float size = baseSize * sizeFactor * heightFactor;
        int alpha = Math.max(0, Math.min(255, (int) (255 * sizeFactor * heightFactor * lightFactor)));

        // Цвет всегда насыщенный, не зависит от lightFactor
        Color glowColor = new Color(
                p.color.getRed(),
                p.color.getGreen(),
                p.color.getBlue(),
                alpha
        );

        int rgbaMain = new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alpha).getRGB();
        if (glossy.getValue()) {
            // Glossy: brighter, larger glow
            Render3D.batchBillboard(e.getMatrices(), p.pos, size * 2.5f, rgbaMain);
            Render3D.batchBillboard(e.getMatrices(), p.pos, size * 1.5f, rgbaMain);
        } else {
            // Non-glossy: softer outer with reduced alpha, mid core, small white highlight
            int softAlpha = Math.max(0, Math.min(255, (int) (alpha * 0.65f)));
            int rgbaSoft = new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), softAlpha).getRGB();
            Render3D.batchBillboard(e.getMatrices(), p.pos, size * 2.2f, rgbaSoft);
            Render3D.batchBillboard(e.getMatrices(), p.pos, size * 1.2f, glowColor.getRGB());
            int whiteAlpha = Math.min(255, alpha + 50);
            int rgbaWhite = new Color(255, 255, 255, whiteAlpha).getRGB();
            Render3D.batchBillboard(e.getMatrices(), p.pos, size * 0.5f, rgbaWhite);
        }
    }

    private boolean isVzletMode() {
        BooleanSetting vzlet = mode.getName("Взлет");
        return vzlet != null && vzlet.getValue();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        if (!useThemeColor.getValue()) return;
        if (randomColor.getValue()) return;

        this.currentColor = themeManager.getBackgroundColor();
        for (Particle p : particles) p.color = this.currentColor;
    }

    @Override
    public void onDisable() {
        particles.clear();
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    private class Particle {
        Vec3d pos;
        Vec3d vel;
        long spawnTime;
        long lifeTime;
        Color color;

        Particle(Vec3d pos, Vec3d vel, long lifeTime, Color color) {
            this.pos = pos;
            this.vel = vel;
            this.spawnTime = System.currentTimeMillis();
            this.lifeTime = lifeTime;
            this.color = color;
        }

        void updateMotion(boolean vzlet) {
            double motionRandom = 0.002;

            vel = vel.add(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandom,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandom * 0.5,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandom
            );

            if (vzlet) vel = new Vec3d(vel.x, 0.03, vel.z);
            else vel = new Vec3d(vel.x, -0.005 + Math.sin((System.currentTimeMillis() - spawnTime) / 500.0) * 0.002, vel.z);

            pos = pos.add(vel);
        }

        float getLifeProgress(long now) {
            return (float) (now - spawnTime) / lifeTime;
        }
    }
}
