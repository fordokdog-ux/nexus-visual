package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.events.impl.EventAttackEntity;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.world.WorldUtils;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import meteordevelopment.orbit.EventHandler;
import java.awt.*;
// imports for renderer delegates only
import dev.simplevisuals.client.render.renderers.JelloRenderer;
import dev.simplevisuals.client.render.renderers.SoulRenderer;
// no local MatrixStack usage after refactor

public class TargetEsp extends Module {

    private static final float BASE_SIZE = 5.0f;

    private static final String markerType = "";

    // Modes
    private final BooleanSetting modeMarker = new BooleanSetting("mode.marker", true, () -> false);
    private final BooleanSetting modeGhosts = new BooleanSetting("mode.soul", false, () -> false);
    private final BooleanSetting modeJello = new BooleanSetting("mode.circle", false, () -> false);

    private final BooleanSetting modeMarkerOld = new BooleanSetting("old", true, () -> false);
    private final BooleanSetting modeMarkerNew = new BooleanSetting("new", false, () -> false);


    private final ListSetting mode = new ListSetting(
            I18n.translate("setting.animationMode"),
            true,
            modeMarker, modeGhosts, modeJello
    );

    // Marker settings
    private final NumberSetting markerScale = new NumberSetting("setting.markerSize", 3.0f, 0.5f, 8.0f, 0.1f);
    private final BooleanSetting markerGlow = new BooleanSetting("setting.markerGlow", true, () -> modeMarker.getValue());
    private final NumberSetting markerGlowIntensity = new NumberSetting("setting.glowIntensity", 0.8f, 0.1f, 2.0f, 0.1f);
    private final BooleanSetting markerHitFlash = new BooleanSetting("setting.hitFlash", true, () -> modeMarker.getValue());
    private ListSetting markerMode = new ListSetting("setting.markerMode", true, modeMarkerOld, modeMarkerNew);

    // Ghosts settings
    // По смыслу это ширина/размер призраков (оставляем ключ как был, чтобы не ломать конфиги)
    private final NumberSetting ghostsScale = new NumberSetting("setting.ghostsScale", 0.4f, 0.1f, 1.0f, 0.05f);
    // Старый регулятор плотности (оставлен ради совместимости, но больше не используется)
    private final NumberSetting ghostsParticleDensity = new NumberSetting("setting.particleDensity", 14, 5, 20, 1);
    private final NumberSetting ghostsSpeed = new NumberSetting("setting.ghostsSpeed", 33.0f, 5.0f, 100.0f, 1.0f);
    private final NumberSetting ghostsLength = new NumberSetting("setting.ghostsLength", 24, 5, 64, 1);
    private final NumberSetting ghostsAngle = new NumberSetting("setting.ghostsAngle", 0.18f, 0.01f, 1.0f, 0.01f);
    private final BooleanSetting ghostsGlow = new BooleanSetting("setting.markerGlow", true, () -> modeGhosts.getValue());
    private final NumberSetting ghostsGlowIntensity = new NumberSetting("setting.glowIntensity", 0.5f, 0.1f, 1.5f, 0.1f);
    private final NumberSetting ghostsAlpha = new NumberSetting("setting.alpha", 1.0f, 0.1f, 1.0f, 0.1f);

    // Jello settings
    private final NumberSetting jelloHeight = new NumberSetting("setting.jelloHeight", 1.5f, 0.5f, 3.0f, 0.1f);
    private final NumberSetting jelloAnimationSpeed = new NumberSetting("setting.animationSpeed", 2500.0f, 1000.0f, 5000.0f, 100.0f);
    private final BooleanSetting jelloGlow = new BooleanSetting("setting.markerGlow", true, () -> modeJello.getValue());
    private final NumberSetting jelloGlowIntensity = new NumberSetting("setting.glowIntensity", 0.5f, 0.1f, 1.5f, 0.1f);
    private final NumberSetting jelloAlpha = new NumberSetting("setting.alpha", 1.0f, 0.1f, 1.0f, 0.1f);

    // Цвет ESP (не зависит от темы)
    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255, 255).getRGB());

    private final ThemeManager themeManager;
    private final SoulRenderer soulRenderer;
    private final JelloRenderer jelloRenderer;

    public TargetEsp() {
        super("TargetEsp", Category.Render, I18n.translate("module.targetesp.description"));

        // Marker visibility
        markerScale.setVisible(() -> modeMarker.getValue());
        markerGlow.setVisible(() -> modeMarker.getValue());
        markerGlowIntensity.setVisible(() -> modeMarker.getValue() && markerGlow.getValue());
        markerHitFlash.setVisible(() -> modeMarker.getValue());
        markerMode.setVisible(() -> modeMarker.getValue());

        // Ghosts visibility
        ghostsScale.setVisible(() -> modeGhosts.getValue());
        // Скрываем старую плотность, чтобы не путать (оставлено в коде для старых конфигов)
        ghostsParticleDensity.setVisible(() -> false);
        ghostsSpeed.setVisible(() -> modeGhosts.getValue());
        ghostsLength.setVisible(() -> modeGhosts.getValue());
        ghostsAngle.setVisible(() -> modeGhosts.getValue());
        ghostsGlow.setVisible(() -> modeGhosts.getValue());
        ghostsGlowIntensity.setVisible(() -> modeGhosts.getValue() && ghostsGlow.getValue());
        markerHitFlash.setVisible(() -> modeMarker.getValue());
        ghostsAlpha.setVisible(() -> modeGhosts.getValue());

        // Jello visibility
        jelloHeight.setVisible(() -> modeJello.getValue());
        jelloAnimationSpeed.setVisible(() -> modeJello.getValue());
        jelloGlow.setVisible(() -> modeJello.getValue());
        jelloGlowIntensity.setVisible(() -> modeJello.getValue() && jelloGlow.getValue());
        markerHitFlash.setVisible(() -> modeMarker.getValue());
        jelloAlpha.setVisible(() -> modeJello.getValue());

        this.themeManager = ThemeManager.getInstance();
        this.soulRenderer = new SoulRenderer(themeManager);
        this.jelloRenderer = new JelloRenderer(themeManager);
    }

    private final Animation animation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private final Animation soulAnimation = new Animation(500, 1f, true, Easing.BOTH_SINE);
    private final Animation jelloAnimation = new Animation(500, 1f, true, Easing.BOTH_SINE);

    private LivingEntity lastTarget = null;
    private LivingEntity fadingTarget = null; // Таргет который исчезает
    private long lastSeenTime = 0L;
    private static final long ESP_DURATION = 2500;
    private long lastHitTime = 0L;
    private static final long HIT_FLASH_DURATION = 300;

    private Vec3d lastKnownCenter = null;
    private float lastKnownHeight = 1.8f;
    private float lastKnownWidth = 0.6f;

    private boolean forceFade = false;
    private Vec3d fadeOrigin = null;

    // Occlusion check throttling
    private long lastOcclusionAt = 0L;
    private boolean lastOcclusion = false;

    // Precompute storage (math-only), updated in background
    private volatile float cachedFinalSize = -1f;
    private volatile int cachedColorRGBA = 0xFFFFFFFF;
    private volatile float cachedRotation = 0f;

    private static int clamp255(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }

    private Color getTargetColor(LivingEntity target) {
        if (target == null) return Color.WHITE;
        float healthPercent = target.getHealth() / target.getMaxHealth();
        if (healthPercent > 0.7f) return Color.GREEN;
        else if (healthPercent > 0.3f) return Color.YELLOW;
        else return Color.RED;
    }

    private Color getMarkerColor() {
        // Check if target was hit recently - flash red for HIT_FLASH_DURATION
        long now = System.currentTimeMillis();
        if (markerHitFlash.getValue() && now - lastHitTime < HIT_FLASH_DURATION) {
            return Color.RED;
        }

        // Пользовательский цвет (не зависит от темы)
        return color.getColor();
    }

    private Vec3d entityCenter(LivingEntity ent, EventRender2D e) {
        Vec3d lp = ent.getLerpedPos(e.getTickDelta());
        return lp.add(0, ent.getHeight() * 0.5, 0);
    }

    // 3D entity center method kept for potential future use
    // private Vec3d entityCenter(LivingEntity ent, EventRender3D.Game e) {
    //     Vec3d lp = ent.getLerpedPos(e.getTickDelta());
    //     return lp.add(0, ent.getHeight() * 0.5, 0);
    // }

    // hasAnyArmor is unused after refactor but kept for potential future logic
    // private static boolean hasAnyArmor(LivingEntity e) {
    //     for (ItemStack s : e.getArmorItems()) {
    //         if (!s.isEmpty()) return true;
    //     }
    //     return false;
    // }

    private static boolean isInvisibleAndUnrevealed(LivingEntity e) {
        return e.hasStatusEffect(StatusEffects.INVISIBILITY) || e.isInvisible();
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        long now = System.currentTimeMillis();
        if (now - lastOcclusionAt < 50L) {
            return lastOcclusion;
        }
        HitResult hr = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        lastOcclusion = hr.getType() != HitResult.Type.MISS;
        lastOcclusionAt = now;
        return lastOcclusion;
    }

    @EventHandler
    public void onAttackEntity(EventAttackEntity e) {
        if (lastTarget != null && e.getTarget() == lastTarget) {
            lastHitTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        // Track previous target if needed in future logic
        // LivingEntity previousTarget = lastTarget;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) hit.getEntity();
                if (living.isAlive()) {
                    Vec3d center = entityCenter(living, e);

                    if (isInvisibleAndUnrevealed(living)) {
                        lastTarget = null;
                        fadeOrigin = center;
                        forceFade = true;
                        fadingTarget = null; // Очищаем fadingTarget
                    } else {
                        Vec3d eyes = mc.player.getCameraPosVec(e.getTickDelta());
                        if (isOccluded(eyes, center)) {
                            lastTarget = null;
                            fadeOrigin = center;
                            forceFade = true;
                            fadingTarget = null; // Очищаем fadingTarget
                        } else {
                            lastTarget = living;
                            lastSeenTime = now;
                            forceFade = false;
                            fadeOrigin = null;
                            fadingTarget = null; // Очищаем fadingTarget при новом таргете

                            lastKnownCenter = center;
                            lastKnownHeight = living.getHeight();
                            lastKnownWidth = living.getWidth();
                        }
                    }
                }
            }
        }

        if (lastTarget != null) {
            boolean killed = !lastTarget.isAlive();
            boolean expired = (now - lastSeenTime) > ESP_DURATION;

            if (killed || expired) {
                Vec3d centerForFade;
                try {
                    centerForFade = entityCenter(lastTarget, e);
                } catch (Throwable t) {
                    centerForFade = lastKnownCenter;
                }
                if (centerForFade != null) fadeOrigin = centerForFade;

                lastKnownHeight = lastTarget.getHeight();
                lastKnownWidth = lastTarget.getWidth();

                fadingTarget = lastTarget; // Сохраняем для анимации
                lastTarget = null;
                forceFade = true;
            } else {
                if (isInvisibleAndUnrevealed(lastTarget)) {
                    fadeOrigin = entityCenter(lastTarget, e);
                    fadingTarget = lastTarget; // Сохраняем для анимации
                    lastTarget = null;
                    forceFade = true;
                } else {
                    Vec3d eyes = mc.player.getCameraPosVec(e.getTickDelta());
                    Vec3d center = entityCenter(lastTarget, e);
                    if (isOccluded(eyes, center)) {
                        fadeOrigin = center;
                        fadingTarget = lastTarget; // Сохраняем для анимации
                        lastTarget = null;
                        forceFade = true;
                    } else {
                        lastKnownCenter = center;
                        lastKnownHeight = lastTarget.getHeight();
                        lastKnownWidth = lastTarget.getWidth();
                    }
                }
            }
        }

        boolean grow = (lastTarget != null) && !forceFade;
        animation.update(grow);
        
        // Управление анимацией soul эффекта
        boolean soulGrow = (lastTarget != null) && !forceFade;
        soulAnimation.update(soulGrow);
        
        // Управление анимацией jello эффекта
        boolean jelloGrow = (lastTarget != null) && !forceFade;
        jelloAnimation.update(jelloGrow);

        // Ensure one mode at a time
        if (modeMarker.getValue() && (modeGhosts.getValue() || modeJello.getValue())) {
            modeGhosts.setValue(false);
            modeJello.setValue(false);
        }
        if (modeGhosts.getValue() && (modeMarker.getValue() || modeJello.getValue())) {
            modeMarker.setValue(false);
            modeJello.setValue(false);
        }
        if (modeJello.getValue() && (modeMarker.getValue() || modeGhosts.getValue())) {
            modeMarker.setValue(false);
            modeGhosts.setValue(false);
        }
        if (!modeMarker.getValue() && !modeGhosts.getValue() && !modeJello.getValue()) {
            modeMarker.setValue(true);
        }

        if (modeMarker.getValue()) {
            renderMarker(e);
        }

        if (animation.getValue() <= 0 && forceFade) {
            forceFade = false;
            fadeOrigin = null;
        }
        
        // Очищаем fadeOrigin и fadingTarget когда soul анимация завершена
        if (soulAnimation.getValue() <= 0 && forceFade && modeGhosts.getValue()) {
            fadeOrigin = null;
            fadingTarget = null;
        }
        
        // Очищаем fadeOrigin и fadingTarget когда jello анимация завершена
        if (jelloAnimation.getValue() <= 0 && forceFade && modeJello.getValue()) {
            fadeOrigin = null;
            fadingTarget = null;
        }

        // Async precompute now scheduled inside renderMarker() to keep MC/GL calls on main thread
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        if (modeGhosts.getValue()) {
            // Рендерим soul эффект если есть таргет или есть fadingTarget для плавного исчезания
            if (lastTarget != null || (fadingTarget != null && soulAnimation.getValue() > 0)) {
                // Используем fadingTarget если есть, иначе lastTarget
                LivingEntity targetToRender = fadingTarget != null ? fadingTarget : lastTarget;
                soulRenderer.render(
                        e,
                        targetToRender,
                    ghostsLength.getValue().intValue(),
                    ghostsScale.getValue().floatValue(),
                    ghostsSpeed.getValue().floatValue(),
                    ghostsAngle.getValue().floatValue(),
                        ghostsGlow.getValue().booleanValue(),
                        ghostsGlowIntensity.getValue().doubleValue(),
                        soulAnimation.getValue(),
                        fadeOrigin,
                        lastKnownHeight,
                        lastKnownWidth,
                        ghostsAlpha.getValue().floatValue(),
                        getMarkerColor()
                );
            }
        } else if (modeJello.getValue()) {
            // Рендерим jello эффект с анимацией появления/исчезания
            if (lastTarget != null || (fadingTarget != null && jelloAnimation.getValue() > 0)) {
                // Используем fadingTarget если есть, иначе lastTarget
                LivingEntity targetToRender = fadingTarget != null ? fadingTarget : lastTarget;
                jelloRenderer.render(
                        e,
                        targetToRender,
                        fadeOrigin,
                        lastKnownWidth,
                        lastKnownHeight,
                        jelloHeight.getValue().doubleValue(),
                        jelloAnimationSpeed.getValue().doubleValue(),
                        jelloGlow.getValue().booleanValue(),
                        jelloGlowIntensity.getValue().doubleValue(),
                        jelloAnimation.getValue(),
                        getMarkerColor()
                );
            }
        }
    }

    private void renderMarker(EventRender2D e) {
        Vec3d centerWorld = lastTarget != null ? entityCenter(lastTarget, e) : fadeOrigin;
        if (centerWorld == null) return;

        float animVal = animation.getValue();
        if (animVal <= 0 && !markerGlow.getValue()) return;

        Vec3d pos = WorldUtils.getPosition(centerWorld);
        if (!(pos.z > 0) || !(pos.z < 1)) return;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(pos.getX(), pos.getY(), 0);

        // Быстрая математика — делаем на главном потоке (Async тут только создавал лаги)
        if (animVal > 0) {
            float maxSize = (float) WorldUtils.getScale(centerWorld, BASE_SIZE * markerScale.getValue() * animVal);

            double px = mc.player.getX();
            double pz = mc.player.getZ();
            double tx = (lastTarget != null) ? lastTarget.getX() : (lastKnownCenter != null ? lastKnownCenter.x : px);
            double tz = (lastTarget != null) ? lastTarget.getZ() : (lastKnownCenter != null ? lastKnownCenter.z : pz);

            double dx = tx - px;
            double dz = tz - pz;
            double distance = Math.sqrt(dx * dx + dz * dz);
            float markerScaleVal = markerScale.getValue().floatValue();
            float finalSize = Math.max(maxSize - (float) distance, markerScaleVal);

            float rotationAngle = (float) (Math.sin(System.currentTimeMillis() / 1000.0) * 360.0);

            Color baseCol = getMarkerColor();
            int fa = clamp255(255 * animVal);
            int rgba = (fa & 0xFF) << 24 |
                    (baseCol.getRed() & 0xFF) << 16 |
                    (baseCol.getGreen() & 0xFF) << 8 |
                    (baseCol.getBlue() & 0xFF);

            cachedFinalSize = finalSize;
            cachedRotation = rotationAngle;
            cachedColorRGBA = rgba;
        }

        // Draw glow effect if enabled
        if (markerGlow.getValue()) {
            renderMarkerGlow(e, centerWorld);
        }

        // Draw main marker
        renderMarkerMain(e, centerWorld, animVal, cachedColorRGBA);

        e.getContext().getMatrices().pop();
    }

    private void renderMarkerGlow(EventRender2D e, Vec3d centerWorld) {
        float animVal = animation.getValue();
        if (animVal <= 0) return;
        Color base = getMarkerColor();
        float glowSize = (float) WorldUtils.getScale(centerWorld, BASE_SIZE * markerScale.getValue() * (1.0f + markerGlowIntensity.getValue() * 0.5f) * animVal);
        int glowAlpha = clamp255(180 * animVal);
        Color glowColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), glowAlpha);

        Render2D.drawTexture(
                e.getContext().getMatrices(),
                -glowSize / 2f, -glowSize / 2f, glowSize, glowSize,
                0f,
                NexusVisual.id("hud/glow.png"),
            glowColor
        );
    }

    private void renderMarkerMain(EventRender2D e, Vec3d centerWorld, float animVal, int colorRGBA) {
        if (animVal <= 0) return;

        // Use precomputed values if available, else fallback to inline computation
        float finalSize = this.cachedFinalSize > 0 ? this.cachedFinalSize : (float) WorldUtils.getScale(centerWorld, BASE_SIZE * markerScale.getValue() * animVal);
        float rotationAngle = this.cachedRotation;

        Color finalColor = new Color(colorRGBA, true);

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle));

        // Старый/новый маркер — текстуры (как было раньше)
        boolean useOld = markerMode.getName("old") != null && markerMode.getName("old").getValue();
        String tex = useOld ? "hud/marker.png" : "hud/alt_marker.png";
        Render2D.drawTexture(
            e.getContext().getMatrices(),
            -finalSize / 2f, -finalSize / 2f, finalSize, finalSize,
            0f,
            NexusVisual.id(tex),
            finalColor
        );

        e.getContext().getMatrices().pop();
    }
}