package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventAttackEntity;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render3D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.mixin.accessors.IWorldRenderer;
import dev.simplevisuals.NexusVisual;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
// imports cleaned; using ArrayList for performance
import java.util.concurrent.ThreadLocalRandom;

public class DamageParticles extends Module implements ThemeManager.ThemeChangeListener {

    private final NumberSetting dmgCount      = new NumberSetting("setting.amount", 30f, 10f, 50f, 1f);
    private final NumberSetting dmgSize       = new NumberSetting("setting.size", 30f, 10f, 50f, 1f);
    private final NumberSetting scatterStrength = new NumberSetting("setting.scatter", 0.07f, 0.01f, 0.2f, 0.01f);
    private final NumberSetting animSpeed     = new NumberSetting("setting.speed", 1.0f, 0.25f, 3.0f, 0.05f);
    private final NumberSetting lifeTimeSec   = new NumberSetting(I18n.translate("setting.lifetime"), 10f, 1f, 20f, 1f);
    private final ListSetting mode            = new ListSetting(I18n.translate("setting.animationMode"), true,
            new BooleanSetting("mode.bouncy", true),
            new BooleanSetting("mode.legacy", false)
    );

    // Texture selection (multi-select)
    private final ListSetting textures = new ListSetting(
            "setting.textures",
            false,
            new BooleanSetting("setting.star", true),
            new BooleanSetting("setting.heart", false),
            new BooleanSetting("setting.dollar", false),
            new BooleanSetting("setting.circle", false),
            new BooleanSetting("setting.amongus", false)
    );

        private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
        private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255, 255).getRGB());

    private static final Identifier STAR    = NexusVisual.id("hud/star.png");
    private static final Identifier HEART   = NexusVisual.id("hud/heart.png");
    private static final Identifier DOLLAR  = NexusVisual.id("hud/dollar.png");
    private static final Identifier CIRCLE  = NexusVisual.id("hud/circle.png");
    private static final Identifier AMONGUS = NexusVisual.id("hud/amongus.png");
    // Default cap; configurable via setting below
    private static int maxParticles = 500;
    private static final double HOVER_HEIGHT = 0.03;
    private static final double CAMERA_BIAS = 0.02;
    private static final double MICRO_BOUNCE_EPS = 0.02;
    private final ThemeManager themeManager;
    private Color currentColor;
    private final java.util.ArrayList<Particle> particles = new java.util.ArrayList<>();
    private final java.util.ArrayList<PendingSpawn> pending = new java.util.ArrayList<>();

    // Performance-related settings (fixed cap via code)

    public DamageParticles() {
        super("DamageParticles", Category.Render, I18n.translate("module.damageparticles.description"));
        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
        // fixed cap
        maxParticles = 500;
    }

    @EventHandler
    private void onAttackEntity(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (e.getTarget() == mc.player) return;
        if (!(e.getTarget() instanceof LivingEntity entity)) return;
        if (!entity.isAlive()) return;
        if (!e.isEffectsAllowed()) return;
        // Проверяем, можно ли обработать этот удар (не является ли он дубликатом)
        if (!e.canProcess()) return;

        List<Identifier> textures = getSelectedTextures();
        if (textures.isEmpty()) return;

        boolean legacyMode = mode.getName("mode.legacy") != null && mode.getName("mode.legacy").getValue();

		int spawnCount = Math.round(dmgCount.getValue());
        if (legacyMode) {
            Vec3d base = entity.getPos().add(0, entity.getHeight() / 2f, 0);
            for (int i = 0; i < spawnCount; i++) {
                ensureSpace();
                Identifier tex = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
                long lifeMs = (long) (lifeTimeSec.getValue() * 1000L);
                Particle p = Particle.createLegacy(this, base, tex, lifeMs, scatterStrength.getValue());
                particles.add(p);
            }
            return;
        }

        // Определяем точку удара аналогично HitBubbles
        Vec3d base;
        HitResult ch = mc.crosshairTarget;
        if (ch != null && ch.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) ch;
            if (ehr.getEntity() == e.getTarget()) {
                base = ehr.getPos();
            } else {
                base = computeHitOnEntityAABB(entity);
            }
        } else {
            base = computeHitOnEntityAABB(entity);
        }
        if (base == null) {
            base = entity.getPos().add(0, entity.getHeight() / 2f, 0);
        }

        // Спавним частицы сразу, как в HitBubbles (без отложенной очереди)
        // Предпочтительное направление: от атакующего к точке удара (разлет в противоположную сторону)
        Vec3d attackerEye = e.getPlayer() != null ? e.getPlayer().getEyePos() : mc.player.getEyePos();
        Vec3d preferredDir = base.subtract(attackerEye);
        if (preferredDir.lengthSquared() == 0) preferredDir = mc.player.getRotationVec(1.0f);
        preferredDir = preferredDir.normalize();
        for (int i = 0; i < spawnCount; i++) {
            ensureSpace();
            Identifier tex = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
            long lifeMs = (long) (lifeTimeSec.getValue() * 1000L);
            Particle p = Particle.createDamage(this, base, tex, lifeMs, scatterStrength.getValue(), preferredDir);
            particles.add(p);
        }
    }

    private Vec3d computeHitOnEntityAABB(LivingEntity entity) {
        Vec3d start = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(6.0));
        Box bb = entity.getBoundingBox();
        Optional<Vec3d> res = bb.raycast(start, end);
        return res.orElse(null);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();

        boolean bouncyMode = mode.getName("mode.bouncy") != null && mode.getName("mode.bouncy").getValue();
        if (bouncyMode) {
            if (!pending.isEmpty()) {
                for (int i = pending.size() - 1; i >= 0; i--) {
                    PendingSpawn ps = pending.get(i);
                    if (ps.entity == null || !ps.entity.isAlive()) {
                        pending.remove(i);
                        continue;
                    }
                    if (ps.entity.hurtTime > 0) {
                        List<Identifier> textures = getSelectedTextures();
                        if (!textures.isEmpty()) {
                            for (int j = 0; j < dmgCount.getValue(); j++) {
                                ensureSpace();
                                Identifier tex = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
                                long lifeMs = (long) (lifeTimeSec.getValue() * 1000L);
                                Vec3d attackerEye2 = mc.player != null ? mc.player.getEyePos() : ps.impactPoint;
                                Vec3d preferredDir2 = ps.impactPoint.subtract(attackerEye2);
                                if (preferredDir2.lengthSquared() == 0) preferredDir2 = new Vec3d(0, 1, 0);
                                preferredDir2 = preferredDir2.normalize();
                                Particle p = Particle.createDamage(this, ps.impactPoint, tex, lifeMs, scatterStrength.getValue(), preferredDir2);
                                particles.add(p);
                            }
                        }
                        pending.remove(i);
                    } else if (now - ps.createdAt > 250L) {
                        pending.remove(i);
                    }
                }
            }
        } else {
            pending.clear();
        }

        // fixed cap enforced
        maxParticles = 500;

        // Cull expired or too far particles with manual loop to avoid iterator allocations
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if ((now - p.spawnTime > p.lifeTime) || (mc.player.getPos().distanceTo(p.pos) > 100)) {
                particles.remove(i);
            }
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        for (int i = 0, size = particles.size(); i < size; i++) {
            Particle p = particles.get(i);
            // Упростим отсечение: как в Snow, по дистанции, без frustum

            p.updatePhysics();

            // Рендер не подавляем: приземление обрабатывается физикой, частица остаётся видимой

            float f = Math.max(0f, 1 - ((now - p.spawnTime) / (float) p.lifeTime));
            int alpha = (int) (255 * 0.8f * f);
            if (alpha <= 0) continue;

            // Перевод пиксельного размера в мировые единицы (~блоки)
            // Масштаб в мире с коэффициентом 0.3 по запросу
            float worldSize = dmgSize.getValue() * 0.04f * 0.3f * f;
            Color base = useThemeColor.getValue() ? themeManager.getBackgroundColor() : this.color.getColor();
            Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
            Vec3d toCam = cameraPos.subtract(p.pos);
            Vec3d renderPos = toCam.lengthSquared() > 0 ? p.pos.add(toCam.normalize().multiply(CAMERA_BIAS)) : p.pos;
            Render3D.drawBillboardTexture(e.getMatrices(), renderPos, worldSize, p.tex, color);
        }
    }

    private void ensureSpace() {
        int size = particles.size();
        if (size >= maxParticles) {
            int toRemove = size - maxParticles + 1;
            if (toRemove > 0) particles.subList(0, toRemove).clear();
        }
    }

    private boolean isInView(Vec3d pos) {
        return ((IWorldRenderer) mc.worldRenderer).getFrustum().isVisible(new Box(pos.add(-0.2, -0.2, -0.2), pos.add(0.2, 0.2, 0.2)));
    }

    private boolean isVisible(Vec3d pos) {
        Vec3d start = mc.player.getEyePos();
        net.minecraft.world.RaycastContext ctx = new net.minecraft.world.RaycastContext(
                start, pos,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        );
        HitResult hr = mc.world.raycast(ctx);
        return hr.getType() == HitResult.Type.MISS || hr.getPos().distanceTo(pos) < 0.1;
    }

    private boolean isSolidAt(BlockPos pos) {
        if (mc.world == null) return false;
        var state = mc.world.getBlockState(pos);
        // Считаем блок «твёрдым», если у него НЕ пустая коллизионная форма
        return !state.getCollisionShape(mc.world, pos).isEmpty();
    }

    private double groundDistance(Vec3d pos) {
        if (mc.world == null) return -1;
        BlockPos base = new BlockPos((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
        double best = Double.MAX_VALUE;

        for (int dy = 0; dy <= 1; dy++) {
            BlockPos check = base.down(dy);
            var state = mc.world.getBlockState(check);
            var shape = state.getCollisionShape(mc.world, check);
            if (shape.isEmpty()) continue;
            // Берём AABB шейпа целиком (shape.getBoundingBox) — для оценки верхней границы
            net.minecraft.util.math.Box bb = shape.getBoundingBox();
            double topY = check.getY() + bb.maxY;
            if (pos.y >= topY) {
                double dist = pos.y - topY;
                if (dist < best) best = dist;
            }
        }

        return best == Double.MAX_VALUE ? -1 : best;
    }

    private double groundTopY(Vec3d pos) {
        if (mc.world == null) return Double.NaN;
        BlockPos base = new BlockPos((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
        double top = -Double.MAX_VALUE;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos check = base.down(dy);
            var state = mc.world.getBlockState(check);
            var shape = state.getCollisionShape(mc.world, check);
            if (shape.isEmpty()) continue;
            net.minecraft.util.math.Box bb = shape.getBoundingBox();
            double ty = check.getY() + bb.maxY;
            if (pos.y >= ty && ty > top) top = ty;
        }
        return top == -Double.MAX_VALUE ? Double.NaN : top;
    }

    private List<Identifier> getSelectedTextures() {
        List<Identifier> list = new ArrayList<>();
        for (BooleanSetting b : textures.getToggled()) {
            String n = b.getName();
            if ("setting.star".equals(n)) list.add(STAR);
            else if ("setting.heart".equals(n)) list.add(HEART);
            else if ("setting.dollar".equals(n)) list.add(DOLLAR);
            else if ("setting.circle".equals(n)) list.add(CIRCLE);
            else if ("setting.amongus".equals(n)) list.add(AMONGUS);
        }
        return list;
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = themeManager.getBackgroundColor();
    }

    private static class PendingSpawn {
        final LivingEntity entity;
        final Vec3d impactPoint;
        final long createdAt;
        PendingSpawn(LivingEntity entity, Vec3d impactPoint, long createdAt) {
            this.entity = entity;
            this.impactPoint = impactPoint;
            this.createdAt = createdAt;
        }
    }

    private static class Particle {
        Vec3d pos;
        Vec3d velocityPhysics;
        Identifier tex;
        long spawnTime;
        long lifeTime;
        long collisionTime = -1;
        float alpha = 0.8f;
        int bounces = 0;
        final DamageParticles owner;
        final boolean legacy;

        Particle(DamageParticles owner, boolean legacy, Vec3d pos, Vec3d vel, Identifier tex, long lifeTime){
            this.owner = owner;
            this.legacy = legacy;
            this.pos = pos;
            this.velocityPhysics = vel;
            this.tex = tex;
            this.spawnTime = System.currentTimeMillis() - 30L;
            this.lifeTime = lifeTime;
        }

		static Particle createDamage(DamageParticles owner, Vec3d pos, Identifier tex, long lifeTime, float scatter, Vec3d preferredDir) {
            double speedJitter = ThreadLocalRandom.current().nextDouble(0.2, 0.4);
            // Позиционная погрешность (XYZ)
            double jx = ThreadLocalRandom.current().nextDouble(-scatter, scatter);
            double jy = ThreadLocalRandom.current().nextDouble(-scatter, scatter);
            double jz = ThreadLocalRandom.current().nextDouble(-scatter, scatter);
            Vec3d spawnPos = pos.add(jx, jy, jz);

            // Равномерный разброс во все стороны (полная сфера)
            double theta = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2.0);
            double u = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double r = Math.sqrt(1 - u * u);
            double sx = r * Math.cos(theta);
            double sy = u;
            double sz = r * Math.sin(theta);
            // Никогда не вверх: направляем вертикальную составляющую вниз или по горизонтали
			Vec3d dir = new Vec3d(sx, -Math.abs(sy), sz).normalize();

			// Масштабируем скорость разлёта от scatter
			double baseline = 0.07; // соответствует настройке по умолчанию
			double speedScale = Math.max(0.4, Math.min(2.5, scatter / baseline));
			double baseSpeed = ThreadLocalRandom.current().nextDouble(0.02, 0.25) * speedScale;
            Vec3d velocity = dir.multiply(baseSpeed * speedJitter);

            return new Particle(owner, false, spawnPos, velocity, tex, lifeTime);
        }

		static Particle createLegacy(DamageParticles owner, Vec3d pos, Identifier tex, long lifeTime, float scatter) {
            double speedJitter = ThreadLocalRandom.current().nextDouble(0.2, 0.4);
            // Позиционная погрешность (XYZ)
            double jx = ThreadLocalRandom.current().nextDouble(-scatter, scatter);
            double jy = ThreadLocalRandom.current().nextDouble(-scatter, scatter);
            double jz = ThreadLocalRandom.current().nextDouble(-scatter, scatter);
            Vec3d spawnPos = pos.add(jx, jy, jz);

            // Равномерный разброс во все стороны (полная сфера)
            double theta = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2.0);
            double u = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double r = Math.sqrt(1 - u * u);
            double sx = r * Math.cos(theta);
            double sy = u;
            double sz = r * Math.sin(theta);
            // Никогда не вверх: направляем вертикальную составляющую вниз или по горизонтали
			Vec3d dir = new Vec3d(sx, -Math.abs(sy), sz).normalize();
			// Масштабируем скорость разлёта от scatter (legacy)
			double baseline = 0.07;
			double speedScale = Math.max(0.4, Math.min(2.5, scatter / baseline));
			double baseSpeed = ThreadLocalRandom.current().nextDouble(0.04, 0.12) * speedScale;
            Vec3d velocity = dir.multiply(baseSpeed * speedJitter);

            return new Particle(owner, true, spawnPos, velocity, tex, lifeTime);
        }

        void updatePhysics() {
            // Простой плавный полёт: гравитация + лёгкое затухание, без столкновений/отскоков
            double s = owner.animSpeed.getValue();
            double gravity = 0.00006;
            velocityPhysics = velocityPhysics.subtract(0, gravity * s, 0);
            if (velocityPhysics.y < -0.08) {
                velocityPhysics = new Vec3d(velocityPhysics.x, -0.08, velocityPhysics.z);
            }

			Vec3d prevPos = pos;
			Vec3d step = velocityPhysics.multiply(s);
			Vec3d nextPos = pos.add(step);

			// Горизонтальные столкновения со стенами (raycast между позициями)
			if (owner.mc.world != null) {
				net.minecraft.world.RaycastContext ctx = new net.minecraft.world.RaycastContext(
					prevPos,
					nextPos,
					net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
					net.minecraft.world.RaycastContext.FluidHandling.NONE,
					owner.mc.player
				);
				net.minecraft.util.hit.HitResult hit = owner.mc.world.raycast(ctx);
				if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
					net.minecraft.util.hit.BlockHitResult bhr = (net.minecraft.util.hit.BlockHitResult) hit;
					// Сдвигаем позицию к точке контакта с небольшим отступом по нормали
					final double eps = 0.002;
					switch (bhr.getSide().getAxis()) {
						case X -> {
							boolean positive = (bhr.getSide() == net.minecraft.util.math.Direction.EAST);
							double nx = hit.getPos().x + (positive ? eps : -eps);
							nextPos = new Vec3d(nx, hit.getPos().y, hit.getPos().z);
							velocityPhysics = new Vec3d(-velocityPhysics.x * 0.4, velocityPhysics.y, velocityPhysics.z * 0.88);
						}
						case Z -> {
							boolean positive = (bhr.getSide() == net.minecraft.util.math.Direction.SOUTH);
							double nz = hit.getPos().z + (positive ? eps : -eps);
							nextPos = new Vec3d(hit.getPos().x, hit.getPos().y, nz);
							velocityPhysics = new Vec3d(velocityPhysics.x * 0.88, velocityPhysics.y, -velocityPhysics.z * 0.4);
						}
						case Y -> {
							// Верх/низ: оставим обработку отскока ниже в логике «земли», но притянем к точке
							boolean up = (bhr.getSide() == net.minecraft.util.math.Direction.UP);
							double ny = hit.getPos().y + (up ? HOVER_HEIGHT : -eps);
							nextPos = new Vec3d(hit.getPos().x, ny, hit.getPos().z);
							if (!up) {
								// удар в потолок — инвертируем vy мягко
								velocityPhysics = new Vec3d(velocityPhysics.x * 0.9, -velocityPhysics.y * 0.3, velocityPhysics.z * 0.9);
							}
						}
					}
				}
			}

            // Приземление на верхнюю грань блока (с отскоком, без проваливания под блок)
            double groundY = owner.groundTopY(nextPos);
            if (!Double.isNaN(groundY) && nextPos.y <= groundY + HOVER_HEIGHT) {
                // Контакт с поверхностью
                pos = new Vec3d(nextPos.x, groundY + HOVER_HEIGHT, nextPos.z);
				// Отскакиваем до 3 раз, затем оседаем
				if (velocityPhysics.y <= 0.0) {
					if (bounces < 3) {
                        double restitution = 0.18 * Math.pow(0.6, Math.max(0, bounces));
                        double newVy = -velocityPhysics.y * restitution;
                        newVy = Math.min(newVy, 0.04);
                        newVy = Math.max(newVy, 0.01);
                        bounces++;
                        velocityPhysics = new Vec3d(velocityPhysics.x * 0.88, newVy, velocityPhysics.z * 0.88);
                    } else {
                        velocityPhysics = new Vec3d(velocityPhysics.x * 0.85, 0.0, velocityPhysics.z * 0.85);
                    }
                }
            } else {
                pos = nextPos;
            }
            double drag = Math.pow(0.998, s);
            velocityPhysics = velocityPhysics.multiply(drag);
        }

        float getLifeProgress(long now) {
            return (float)(now - spawnTime) / lifeTime;
        }
    }

    private void resolveEntityClipping(Particle p) {
        if (mc.world == null) return;
        Box query = new Box(p.pos.add(-0.2, -0.2, -0.2), p.pos.add(0.2, 0.2, 0.2));
        java.util.List<Entity> entities = mc.world.getOtherEntities(null, query);
        for (Entity ent : entities) {
            Box bb = ent.getBoundingBox();
            if (bb.contains(p.pos)) {
                double dxMin = p.pos.x - bb.minX;
                double dxMax = bb.maxX - p.pos.x;
                double dzMin = p.pos.z - bb.minZ;
                double dzMax = bb.maxZ - p.pos.z;
                double dyMin = p.pos.y - bb.minY;
                double dyMax = bb.maxY - p.pos.y;
                double minPen = dxMin;
                int axis = 0; // 0-x,1-y,2-z
                boolean positive = false;
                if (dxMax < minPen) { minPen = dxMax; axis = 0; positive = true; }
                if (dzMin < minPen) { minPen = dzMin; axis = 2; positive = false; }
                if (dzMax < minPen) { minPen = dzMax; axis = 2; positive = true; }
                if (dyMin < minPen) { minPen = dyMin; axis = 1; positive = false; }
                if (dyMax < minPen) { minPen = dyMax; axis = 1; positive = true; }

                if (axis == 0) {
                    double nx = positive ? (bb.maxX + 0.01) : (bb.minX - 0.01);
                    p.pos = new Vec3d(nx, p.pos.y, p.pos.z);
                    p.velocityPhysics = new Vec3d(p.velocityPhysics.x * -0.4, p.velocityPhysics.y, p.velocityPhysics.z);
                } else if (axis == 2) {
                    double nz = positive ? (bb.maxZ + 0.01) : (bb.minZ - 0.01);
                    p.pos = new Vec3d(p.pos.x, p.pos.y, nz);
                    p.velocityPhysics = new Vec3d(p.velocityPhysics.x, p.velocityPhysics.y, p.velocityPhysics.z * -0.4);
                } else {
                    double ny = positive ? (bb.maxY + HOVER_HEIGHT) : (bb.minY - 0.01);
                    p.pos = new Vec3d(p.pos.x, ny, p.pos.z);
                    p.velocityPhysics = new Vec3d(p.velocityPhysics.x, -p.velocityPhysics.y * 0.3, p.velocityPhysics.z);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        particles.clear();
        pending.clear();
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

}
