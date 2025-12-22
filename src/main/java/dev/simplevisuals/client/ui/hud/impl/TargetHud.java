package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.ui.hud.HudStyle;
import dev.simplevisuals.client.util.Network.Server;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.animations.infinity.InfinityAnimation;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.world.WorldUtils;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.Identifier;
import dev.simplevisuals.NexusVisual;
import java.util.List;
import java.util.ArrayList;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import java.awt.*;

public class TargetHud extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color headerTextColor;
    private Color lowDurabilityColor;
    private Color absorbColor;

    // Settings
    private final BooleanSetting displayAbsorption = new BooleanSetting("displayAbsorption", true);
    private final BooleanSetting followTarget = new BooleanSetting("targethud.follow", false);
    // Hard-disabled: user requested no HUD particles on hit.
    private final BooleanSetting displayHudParticles = new BooleanSetting("hudParticles", false, () -> false);
    private final ListSetting style;

    public TargetHud() {
        super("TargetHud");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

        // Build ListSetting with internal options so individual booleans are not exposed as separate settings
        BooleanSetting optDefault = new BooleanSetting("targethud.style.default", true, () -> false);
        BooleanSetting optCard = new BooleanSetting("targethud.style.card", false, () -> false);
        this.style = new ListSetting("targethud.style", () -> true, true, optDefault, optCard).setSingleSelect(true);

        getSettings().add(displayAbsorption);
        getSettings().add(followTarget);
        getSettings().add(style);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        Color tb = themeManager.getBackgroundColor();
        this.bgColor = tb == null ? new Color(30, 30, 30, 240) : tb;
        this.textColor = themeManager.getTextColor();

        this.headerTextColor = this.textColor;
        this.lowDurabilityColor = new Color(200, 80, 80, 220);
        this.absorbColor = new Color(255, 190, 0, 255);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    // Animations
    private final InfinityAnimation fadeAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation scaleAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation slideAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation hpAnimPx = new InfinityAnimation(Easing.BOTH_SINE);
    private final InfinityAnimation absAnimPx = new InfinityAnimation(Easing.BOTH_SINE);

    private LivingEntity lastTarget = null;
    private long lastSeenTime = 0L;
    private static final long HUD_DURATION = 2000;
    private static final long OCCLUSION_DEBOUNCE_MS = 220;
    private boolean forceFade = false;
    private Vec3d lastKnownCenter = null;
    private long occludedSinceMs = 0L;

    private float animationDirectionX = 1f;
    private float animationDirectionY = 1f;
    private int lastHurtTicks = 0;
    private int prevHurtTime = 0;

    // Dimensions
    private static final float ROUNDING = 6f;
    private static final float SPACING = 5f;

    // HUD dimensions
    private static final float WIDTH = 122f;
    private static final float HEIGHT = 64f / 1.5f;

    // Head size (reduced)
    private static final float HEAD = 21.5f;

    // HUD star particles (reused from DamageParticles -> hud/star.png)
    private static final Identifier STAR_TEX = NexusVisual.id("hud/star.png");
    private final List<HudParticle> hudParticles = new ArrayList<>();
    private long lastFrameTimeMs = System.currentTimeMillis();
    private float previousHp01 = -1f;

    private static String sanitizeLegacyFormattingArtifacts(String s) {
        if (s == null) return "";
        String t = s;
        // Иногда после кривого strip остаются одинокие коды (например: "fЛошадьg").
        // Убираем только на краях и только если следующий/предыдущий символ НЕ ASCII.
        final String codes = "0123456789abcdefklmnorABCDEFKLMNORgG";

        while (t.length() >= 2) {
            char c0 = t.charAt(0);
            char c1 = t.charAt(1);
            boolean c1NonAscii = c1 > 127;
            if (c1NonAscii && codes.indexOf(c0) >= 0) {
                t = t.substring(1);
                continue;
            }
            break;
        }

        while (t.length() >= 2) {
            char cn = t.charAt(t.length() - 1);
            char cp = t.charAt(t.length() - 2);
            boolean cpNonAscii = cp > 127;
            if (cpNonAscii && codes.indexOf(cn) >= 0) {
                t = t.substring(0, t.length() - 1);
                continue;
            }
            break;
        }
        return t;
    }

    private void drawEntityAvatar3D(EventRender2D e, float x, float y, float size, LivingEntity entity) {
        if (entity == null) return;
        try {
            // Рендер 3D модели в HUD (как «фото» моба). Без вращения мышью — просто аккуратный превью.
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();

            float scale = size * 0.45f;
            float cx = x + size / 2f;
            float cy = y + size * 0.92f;

            float prevBodyYaw = entity.bodyYaw;
            float prevYaw = entity.getYaw();
            float prevHeadYaw = entity.getHeadYaw();
            float prevPitch = entity.getPitch();

            entity.bodyYaw = 180.0f;
            entity.setYaw(180.0f);
            entity.setHeadYaw(180.0f);
            entity.setPitch(0.0f);

            MatrixStack matrices = e.getContext().getMatrices();
            matrices.push();
            matrices.translate(cx, cy, 60.0);
            matrices.scale(scale, scale, scale);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));

            RenderSystem.enableDepthTest();
            dispatcher.setRenderShadows(false);
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            consumers.draw();
            dispatcher.setRenderShadows(true);
            RenderSystem.disableDepthTest();

            matrices.pop();

            entity.bodyYaw = prevBodyYaw;
            entity.setYaw(prevYaw);
            entity.setHeadYaw(prevHeadYaw);
            entity.setPitch(prevPitch);
        } catch (Throwable ignored) {
        }
    }

    // Quick toggle popup (RMB on HUD)
    private boolean particlesQuickOpen = false;
    private long particlesQuickUntilMs = 0L;
    private float particlesToggleX, particlesToggleY, particlesToggleW, particlesToggleH;
    private float particlesPopupX, particlesPopupY, particlesPopupW, particlesPopupH;
    private float lastHudScreenX, lastHudScreenY, lastHudScreenW, lastHudScreenH;
    private static final long PARTICLES_QUICK_LIFETIME_MS = 2800L;

    private void updateLastHudScreenRect(float x, float y, float w, float h, float cx, float cy, float scale, float slideX, float slideY) {
        float x0 = (x - cx) * scale + cx + slideX;
        float y0 = (y - cy) * scale + cy + slideY;
        float x1 = (x + w - cx) * scale + cx + slideX;
        float y1 = (y + h - cy) * scale + cy + slideY;

        lastHudScreenX = Math.min(x0, x1);
        lastHudScreenY = Math.min(y0, y1);
        lastHudScreenW = Math.abs(x1 - x0);
        lastHudScreenH = Math.abs(y1 - y0);
    }

    private static boolean isHoveredPadded(float x, float y, float w, float h, int mx, int my, float pad) {
        float x0 = x - pad;
        float y0 = y - pad;
        float x1 = x + w + pad;
        float y1 = y + h + pad;
        return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
    }

    private Vec3d entityCenter(LivingEntity ent, EventRender2D e) {
        Vec3d lp = ent.getLerpedPos(e.getTickDelta());
        return lp.add(0, ent.getHeight() * 0.5, 0);
    }

    private static boolean isInvisibleAndUnrevealed(LivingEntity e) {
        return e.hasStatusEffect(StatusEffects.INVISIBILITY) || e.isInvisible();
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        HitResult hr = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hr.getType() != HitResult.Type.MISS;
    }

    private boolean isOccludedForHud(LivingEntity ent, EventRender2D e) {
        if (ent == null) return true;
        Vec3d from = mc.player.getCameraPosVec(e.getTickDelta());

        // Проверяем несколько точек модели. Это снижает ложные срабатывания,
        // когда центр сущности «попадает» за полублок/стенку, но сама сущность видна.
        Vec3d lerped = ent.getLerpedPos(e.getTickDelta());
        double w = Math.max(0.25, ent.getWidth() * 0.35);
        Vec3d center = lerped.add(0, ent.getHeight() * 0.55, 0);
        Vec3d head = lerped.add(0, ent.getHeight() * 0.85, 0);
        Vec3d left = center.add(w, 0, 0);
        Vec3d right = center.add(-w, 0, 0);

        // Если хотя бы одна точка НЕ перекрыта — считаем, что таргет видим.
        return isOccluded(from, center)
                && isOccluded(from, head)
                && isOccluded(from, left)
                && isOccluded(from, right);
    }

    private Color getHitColor(LivingEntity entity, int alpha) {
        return new Color(255, 255, 255, alpha); // стандартный цвет
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTimeMs) / 1000f;
        lastFrameTimeMs = now;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
                Vec3d center = entityCenter(living, e);

                if (isOccludedForHud(living, e)) {
                    if (occludedSinceMs == 0L) occludedSinceMs = now;
                    if (now - occludedSinceMs >= OCCLUSION_DEBOUNCE_MS) {
                        lastTarget = null;
                        forceFade = true;
                        lastKnownCenter = center;
                    }
                } else {
                    occludedSinceMs = 0L;
                    if (lastTarget == null) {
                        animationDirectionX = (float) (Math.random() * 2 - 1);
                        animationDirectionY = (float) (Math.random() * 2 - 1);
                    }
                    if (lastTarget != living) {
                        prevHurtTime = 0;
                    }
                    lastTarget = living;
                    lastSeenTime = now;
                    forceFade = false;
                    lastKnownCenter = center;
                }
            }
        }

        // Если прицела по сущности нет — сбрасываем таймер «закрыто стеной», чтобы не залипало.
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            occludedSinceMs = 0L;
        }

        if (lastTarget != null && (!lastTarget.isAlive() || now - lastSeenTime > HUD_DURATION)) {
            lastTarget = null;
            forceFade = true;
        }

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean anyScreenOpen = mc.currentScreen != null;
        boolean previewMode = chatOpen && lastTarget == null;
        LivingEntity previewEntity = previewMode ? mc.player : null;

        boolean shouldShow = (lastTarget != null && !forceFade) || previewMode;
        fadeAnimation.animate(shouldShow ? 1f : 0f, 200);
        scaleAnimation.animate(shouldShow ? 1f : 0.9f, 200);
        slideAnimation.animate(shouldShow ? 0f : 1f, 220);

        if (fadeAnimation.getValue() <= 0 && forceFade && !previewMode) {
            forceFade = false;
            lastKnownCenter = null;
            previousHp01 = -1f;
            hudParticles.clear();
            prevHurtTime = 0;
            return;
        }

        int alpha = (int) (230 * fadeAnimation.getValue());
        if (alpha <= 0) return;

        // Auto-close quick popup
        if (particlesQuickOpen && System.currentTimeMillis() > particlesQuickUntilMs) {
            particlesQuickOpen = false;
        }

        LivingEntity target = lastTarget;
        if (!previewMode && target == null && lastKnownCenter == null) return;

        float posX = getX();
        float posY = getY();

        // Follow mode: pin HUD near the target on screen (only in-game; pause while any GUI is open)
        if (followTarget.getValue() && !anyScreenOpen && !previewMode && target != null) {
            try {
                Vec3d center = entityCenter(target, e);
                Vec3d screen = WorldUtils.getPosition(center);
                if (screen.z > 0) {
                    float winW = mc.getWindow().getScaledWidth();
                    float winH = mc.getWindow().getScaledHeight();
                    float offX = 18f; // left of the player
                    float offY = 0f;

                    posX = (float) screen.x - WIDTH - offX;
                    posY = (float) screen.y - (HEIGHT / 2f) + offY;

                    // Clamp to screen
                    float pad = 5f;
                    posX = Math.max(pad, Math.min(posX, winW - WIDTH - pad));
                    posY = Math.max(pad, Math.min(posY, winH - HEIGHT - pad));
                }
            } catch (Throwable ignored) {
            }
        }

        setBounds(posX, posY, WIDTH, HEIGHT);

        float rawHp = target != null ? MathUtils.round(Server.getHealth(target, false)) : (previewEntity != null ? MathUtils.round(Server.getHealth(previewEntity, false)) : 0f);
        float maxHp = target != null ? Math.max(1f, MathUtils.round(target.getMaxHealth())) : (previewEntity != null ? Math.max(1f, MathUtils.round(previewEntity.getMaxHealth())) : 20f);
        float absorb = target != null ? Math.max(0f, MathUtils.round(target.getAbsorptionAmount())) : (previewEntity != null ? Math.max(0f, MathUtils.round(previewEntity.getAbsorptionAmount())) : 0f);

        float hp01 = MathHelper.clamp(rawHp / maxHp, 0f, 1f);
        float abs01 = absorb > 0f ? MathHelper.clamp(absorb / maxHp, 0f, 1f) : 0f;

        float scale = scaleAnimation.getValue() * toggledAnimation.getValue() * fadeAnimation.getValue();
        float slideX = 16f * slideAnimation.getValue() * animationDirectionX;
        float slideY = 16f * slideAnimation.getValue() * animationDirectionY;

        // Track real screen-space rect for correct RMB hit-testing while scaled/slid
        updateLastHudScreenRect(posX, posY, WIDTH, HEIGHT, posX + WIDTH / 2f, posY + HEIGHT / 2f, scale, slideX, slideY);

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(posX + WIDTH / 2f + slideX, posY + HEIGHT / 2f + slideY, 0f);
        e.getContext().getMatrices().scale(scale, scale, 1f);
        e.getContext().getMatrices().translate(-(posX + WIDTH / 2f), -(posY + HEIGHT / 2f), 0f);

        // Alternate style: simple card with percent bar inside
        if (style.getName("targethud.style.card").getValue()) {
            // Card dimensions match default TargetHud
            float cardW = WIDTH - 2f;
            float cardH = HEIGHT + 2f;
            setBounds(posX, posY, cardW, cardH);

            // Update hitbox for card-style too
            updateLastHudScreenRect(posX, posY, cardW, cardH, posX + cardW / 2f, posY + cardH / 2f, scale, slideX, slideY);

                // Background (filled) + border
                HudStyle.drawCard(e.getContext().getMatrices(), posX, posY, cardW, cardH, ROUNDING, themeManager.getCurrentTheme(), alpha, Math.min(180, alpha));

            // Avatar box (use same size/placement vibe as default)
            float avatarSize = HEAD + 12f;
            float avatarX = posX + SPACING;
            float avatarY = posY + (cardH / 2f - avatarSize / 2f) - 0.5f;
            Render2D.drawRoundedRect(e.getContext().getMatrices(), avatarX, avatarY, avatarSize, avatarSize, 6f,
                    new Color(80, 80, 80, Math.min(200, alpha)));

            // Draw head texture same as in default style when possible
            if (previewMode && previewEntity instanceof PlayerEntity) {
                Color headColor = getHitColor(previewEntity, alpha);
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        avatarX, avatarY,
                        avatarSize, avatarSize,
                        3f,
                        0.125f, 0.125f, 0.125f, 0.125f,
                        ((AbstractClientPlayerEntity) previewEntity).getSkinTextures().texture(),
                        headColor
                );
            } else if (target instanceof PlayerEntity) {
                Color headColor = getHitColor(target, alpha);
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        avatarX, avatarY,
                        avatarSize, avatarSize,
                        3f,
                        0.125f, 0.125f, 0.125f, 0.125f,
                        ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                        headColor
                );
            } else if (target != null) {
                // Non-player entity: render 3D model as avatar
                drawEntityAvatar3D(e, avatarX, avatarY, avatarSize, target);
            } else {
                // Fallback mark
                Render2D.drawFont(
                        e.getContext().getMatrices(),
                        Fonts.BOLD.getFont(8.5f),
                        "?",
                        avatarX + (avatarSize / 2f) - Fonts.BOLD.getWidth("?", 8.5f) / 2f,
                        avatarY + (avatarSize / 2f) - Fonts.BOLD.getHeight(8.5f) / 2f,
                        new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
                );
            }

            // Name text (with ellipsis if overflow)
                String dispName = (previewMode && previewEntity != null) ? previewEntity.getName().getString()
                    : ((target != null && !target.getName().getString().isEmpty()) ? target.getName().getString() : "Unknown");
                dispName = sanitizeLegacyFormattingArtifacts(dispName);
            // Hide name in preview when NameProtect is enabled
            if (previewMode) {
                NameProtect np = NameProtect.getInstance();
                if (np != null && np.isToggled()) {
                    String replacement = np.getCustomName().getValue();
                    dispName = replacement != null && !replacement.isEmpty() ? replacement : "Protected";
                }
            }
            float nameX = avatarX + avatarSize + SPACING * 2 - 4f;
            float maxNameWCard = (posX + cardW - SPACING) - nameX;
            String drawName = ellipsize(dispName, 9f, maxNameWCard);
            Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(9f),
                    drawName,
                    nameX,
                    posY + SPACING + 1f,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
            );

            // Progress bar with percent text inside
            float barW2 = cardW - SPACING - 42f;
            float barH2 = 12f;
            float barX2 = posX + SPACING + 38f;
            float barY2 = posY + cardH - SPACING - barH2 - 1.5f;

                // Track
                HudStyle.drawInset(e.getContext().getMatrices(), barX2, barY2, barW2, barH2, 5f, themeManager.getCurrentTheme(), Math.min(150, alpha));
            // Fill (no smoothing)
            float hpTargetPx2 = barW2 * hp01;
            // Slight smoothing (fast)
            float curHpPx2 = hpAnimPx.getValue();
            long hpDur2 = hpTargetPx2 < curHpPx2 ? 40L : 65L;
            float fillW = Math.max(0f, Math.min(barW2, hpAnimPx.animate(hpTargetPx2, hpDur2)));
                Render2D.drawRoundedRect(e.getContext().getMatrices(), barX2, barY2, fillW, barH2, 5f,
                    HudStyle.alphaCap(themeManager.getAccentColor(), alpha));

            // Percent text centered
            int percent = (int) Math.round(hp01 * 100.0);
            String pct = percent + "%";
            float pctX = barX2 + (barW2 - Fonts.BOLD.getWidth(pct, 8.5f)) / 2f;
            float pctY = barY2 + (barH2 - Fonts.BOLD.getHeight(8.5f)) / 2f + 0.2f;
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(7.5f), pct, pctX, pctY,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha));

            e.getContext().getMatrices().pop();

            // Draw quick menu in screen space (not affected by HUD scale)
            renderParticlesQuick(e, lastHudScreenX, lastHudScreenY, lastHudScreenW, lastHudScreenH, alpha, themeManager.getCurrentTheme());
            super.onRender2D(e);
            return;
        }

        // Default style
        ThemeManager.Theme theme = themeManager.getCurrentTheme();
        HudStyle.drawCard(e.getContext().getMatrices(), posX, posY, WIDTH, HEIGHT, ROUNDING, theme, alpha, Math.min(180, alpha));

        float headX = posX + 8f;
        float headY = posY + (HEIGHT - HEAD) / 2f;
        float barX = headX + HEAD + 8f;
        // duplicates removed: barY/barW/barH already declared above

        // Avatar box
        HudStyle.drawInset(e.getContext().getMatrices(), headX, headY, HEAD, HEAD, 6f, theme, Math.min(220, alpha));

        // Draw head texture same as in default style when possible
        if (previewMode && previewEntity instanceof PlayerEntity) {
            Color headColor = getHitColor(previewEntity, alpha);
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    headX, headY,
                    HEAD, HEAD,
                    3f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) previewEntity).getSkinTextures().texture(),
                    headColor
            );
        } else if (target instanceof PlayerEntity) {
            Color headColor = getHitColor(target, alpha);
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    headX, headY,
                    HEAD, HEAD,
                    3f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                    headColor
            );
        } else if (target != null) {
            // Non-player entity: render 3D model as avatar
            drawEntityAvatar3D(e, headX, headY, HEAD, target);
        } else {
            // Fallback mark
            Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(8.5f),
                    "?",
                    headX + (HEAD / 2f) - Fonts.BOLD.getWidth("?", 8.5f) / 2f,
                    headY + (HEAD / 2f) - Fonts.BOLD.getHeight(8.5f) / 2f,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
            );
        }

        // Text
        float textX = headX + HEAD + 8f;
        float textY = posY + 8f;
        String name = previewMode && previewEntity != null ? previewEntity.getName().getString() : ((target != null && !target.getName().getString().isEmpty())
            ? target.getName().getString() : "Unknown");
        name = sanitizeLegacyFormattingArtifacts(name);
        // Hide name in preview when NameProtect is enabled
        if (previewMode) {
            NameProtect np = NameProtect.getInstance();
            if (np != null && np.isToggled()) {
                String replacement = np.getCustomName().getValue();
                name = replacement != null && !replacement.isEmpty() ? replacement : "Protected";
            }
        }
        float maxNameW = (posX + WIDTH - SPACING) - textX;
        String nameDraw = ellipsize(name, 9f, maxNameW);
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(9f),
                nameDraw, textX, textY, new Color(headerTextColor.getRed(), headerTextColor.getGreen(), headerTextColor.getBlue(), alpha));

        // HP text (style 1 / default): makes it clear how much HP is left
        String hpText = "HP: " + (int) Math.floor(rawHp);
        float hpFs = 8f;
        float hpY = textY + Fonts.BOLD.getHeight(9f) + 2f;
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(hpFs), hpText, textX, hpY,
            new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), Math.min(220, alpha)));
        float barY = posY + HEIGHT - 10f;
        float barW = (posX + WIDTH - 8f) - barX;
        float barH = 5.5f;

        HudStyle.drawInset(e.getContext().getMatrices(), barX, barY, barW, barH, 6f, theme, Math.min(220, alpha));

        float hpTargetPx = barW * hp01;
        float absTargetPx = barW * abs01;
        // Slight smoothing: fast but not "rubber"
        float curHpPx = hpAnimPx.getValue();
        long hpDur = hpTargetPx < curHpPx ? 40L : 65L;
        float hpPx = Math.min(barW, hpAnimPx.animate(hpTargetPx, hpDur));
        float absPx = absorb > 0f ? Math.min(barW, absAnimPx.animate(absTargetPx, 75)) : 0f;

        // HUD hit particles are intentionally disabled.
        displayHudParticles.setValue(false);
        hudParticles.clear();
        previousHp01 = -1f;
        prevHurtTime = 0;

        // Use live accent color from theme for gradient themes
        Color liveAccent = themeManager.getAccentColor();
        Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, hpPx, barH,
                Math.min(2f, hpPx / 2f), liveAccent);

        if (displayAbsorption.getValue() && absorb > 0f && absPx > 0f) {
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, absPx, barH,
                    Math.min(2f, absPx / 2f), absorbColor);
        }

        // HUD particles rendering removed (disabled).

        // Back to screen-space for correct mouse hit-testing
        e.getContext().getMatrices().pop();

        // No quick toggle menu for particles (feature disabled).

        super.onRender2D(e);
    }

    @Override
    public void onMouse(dev.simplevisuals.client.events.impl.EventMouse e) {
        if (fullNullCheck()) return;

        // Keep base drag/settings behavior in chat. Outside chat we intentionally do nothing.
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            super.onMouse(e);
        }
    }

    private void renderParticlesQuick(EventRender2D e, float hudX, float hudY, float hudW, float hudH, int alpha, ThemeManager.Theme theme) {
        if (!particlesQuickOpen) return;

        float menuW = 110f;
        float menuH = 20f;
        float x = hudX + hudW + 6f;
        float y = hudY + 6f;

        particlesPopupX = x;
        particlesPopupY = y;
        particlesPopupW = menuW;
        particlesPopupH = menuH;

        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        if (x + menuW > sw - 4f) x = hudX - menuW - 6f;
        if (y + menuH > sh - 4f) y = sh - 4f - menuH;

        particlesPopupX = x;
        particlesPopupY = y;

        HudStyle.drawCard(e.getContext().getMatrices(), x, y, menuW, menuH, 6f, theme, Math.min(220, alpha), Math.min(160, alpha));

        float icon = 12f;
        float ix = x + 6f;
        float iy = y + (menuH - icon) / 2f;
        Color accent = HudStyle.alphaCap(themeManager.getAccentColor(), alpha);
        Render2D.drawTexture(e.getContext().getMatrices(), ix, iy, icon, icon, 0f, STAR_TEX, accent);

        String label = "Частицы";
        float fs = 7.5f;
        float tx = ix + icon + 6f;
        float ty = y + (menuH - Fonts.REGULAR.getHeight(fs)) / 2f + 0.25f;
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.REGULAR.getFont(fs), label, tx, ty, HudStyle.alphaCap(themeManager.getTextColor(), Math.min(220, alpha)));

        // Toggle button
        float btnW = 30f;
        float btnH = 12f;
        float btnX = x + menuW - 6f - btnW;
        float btnY = y + (menuH - btnH) / 2f;
        this.particlesToggleX = btnX;
        this.particlesToggleY = btnY;
        this.particlesToggleW = btnW;
        this.particlesToggleH = btnH;

        boolean on = displayHudParticles.getValue();
        HudStyle.drawInset(e.getContext().getMatrices(), btnX, btnY, btnW, btnH, 6f, theme, Math.min(170, alpha));
        if (on) {
            Render2D.drawRoundedRect(e.getContext().getMatrices(), btnX, btnY, btnW, btnH, 6f, HudStyle.alphaCap(themeManager.getAccentColor(), 80));
        }
        String st = on ? "ВКЛ" : "ВЫКЛ";
        float stW = Fonts.BOLD.getWidth(st, 7f);
        float stX = btnX + (btnW - stW) / 2f;
        float stY = btnY + (btnH - Fonts.BOLD.getHeight(7f)) / 2f + 0.25f;
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(7f), st, stX, stY, HudStyle.alphaCap(themeManager.getTextColor(), Math.min(220, alpha)));
    }

    // Trim text to fit maxWidth by appending "..." when necessary using the provided font size
    private String ellipsize(String text, float fontSize, float maxWidth) {
        if (text == null) return "";
        if (Fonts.BOLD.getWidth(text, fontSize) <= maxWidth) return text;
        String ellipsis = "...";
        float ellipsisW = Fonts.BOLD.getWidth(ellipsis, fontSize);
        if (ellipsisW > maxWidth) return ""; // not enough space for anything
        int lo = 0, hi = text.length();
        String best = "";
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            String candidate = text.substring(0, mid) + ellipsis;
            float w = Fonts.BOLD.getWidth(candidate, fontSize);
            if (w <= maxWidth) {
                best = candidate;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    private static class HudParticle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        long lifeMs;
        long ageMs;
        float alpha = 1f;

        HudParticle(float x, float y, float vx, float vy, float size, long lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.lifeMs = lifeMs;
            this.ageMs = 0L;
        }

        boolean updateAndIsDead(float dtSeconds) {
            long dtMs = (long) (dtSeconds * 1000f);
            this.ageMs += dtMs;
            float t = Math.max(0f, Math.min(1f, ageMs / (float) lifeMs));
            // Smooth drift with gentle damping, no gravity
            float dampingPerSecond = 0.88f; // retain ~88% speed per second
            float factor = (float) Math.pow(dampingPerSecond, dtSeconds);
            this.vx *= factor;
            this.vy *= factor;
            this.x += vx * dtSeconds;
            this.y += vy * dtSeconds;
            // Fade out towards end
            this.alpha = 1f - t;
            return ageMs >= lifeMs || alpha <= 0f;
        }
    }
}