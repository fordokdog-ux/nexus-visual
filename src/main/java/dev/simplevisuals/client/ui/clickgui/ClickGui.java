package dev.simplevisuals.client.ui.clickgui;

import dev.simplevisuals.client.ui.clickgui.components.impl.ModuleComponent;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;
import dev.simplevisuals.client.managers.ThemeManager; // Import ThemeManager
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.impl.render.UI;
import dev.simplevisuals.modules.impl.utility.ClientSound;
import dev.simplevisuals.NexusVisual;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.client.sound.PositionedSoundInstance;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ClickGui extends Screen implements Wrapper {

    private final Animation yAnimation = new Animation(360, 1f, true, Easing.OUT_QUART);
    private final ThemeManager themeManager; // Add ThemeManager

    private String description = "";
    private boolean closing = false;
    private float uiAlpha = 0f;
    private float contentOffsetY = 0f;

    private static final Category[] TABS = {
            Category.Render,
            Category.Utility,
            Category.Theme // Add Theme category
    };

    private Category selectedCategory = Category.Render;

    private float x, y, width, height;

    private float lastUiScale = 1.0f;
    private float lastLayoutScale = -1.0f;

    private static final float HEADER_H = 30f;
    private static final float SIDEBAR_W = 120f;
    private static final float SIDEBAR_PAD = 10f;
    private static final float SIDEBAR_ITEM_H = 24f;
    private static final float SIDEBAR_GAP = 6f;

    // ClickGUI color scheme (independent from ThemeManager themes)
    private static boolean guiLightMode = false;
    private float themeToggleX, themeToggleY, themeToggleS;

    public static boolean isGuiLightMode() {
        return guiLightMode;
    }

    private static boolean isLightTheme(ThemeManager.Theme theme) {
        if (theme == null) return false;
        Color bg = theme.getBackgroundColor();
        if (bg == null) return false;
        double l = (0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue()) / 255.0;
        return l > 0.55;
    }

    private final Map<Category, List<ModuleComponent>> componentsByCategory = new EnumMap<>(Category.class);

    private float scrollY = 0f;
    private float maxScroll = 0f;
    private float tabScrollY = 0f;
    private float maxTabScroll = 0f;
    private float scrollYTarget = 0f;

    private static final int COLS = 2;
    private static final float GAP = 10f;

    private ModuleComponent activeSettings = null;
    private float settingsScrollY = 0f;
    private float settingsMaxScroll = 0f;
    private float settingsScrollYTarget = 0f;
    private final Animation settingsAnimation = new Animation(280, 1f, true, Easing.OUT_QUART);

    // Theme editor (for custom themes)
    private boolean themeEditorDragging = false;
    private int themeEditorDragKey = -1;
    private boolean themeNameEditing = false;
    private String themeNameBuffer = "";
    private ThemeManager.CustomTheme themeNameTarget = null;

    // Color wheel picker (replaces RGB sliders)
    private NativeImageBackedTexture colorWheelTexture = null;
    private boolean colorWheelReady = false;
    private int themeColorTarget = 0; // 0=bg, 1=secondary, 2=accent
    private boolean wheelDragging = false;

    public ClickGui() {
        super(Text.of("nexus-visual-clickgui"));
        this.themeManager = ThemeManager.getInstance(); // Initialize ThemeManager
    }

    private float getUiScale() {
        try {
            UI ui = NexusVisual.getInstance().getModuleManager().getModule(UI.class);
            if (ui != null && ui.clickGuiScale != null) {
                float requested = ui.clickGuiScale.getValue().floatValue();
                // Keep scaling linear: layout size is stable, scale just zooms.
                // Also clamp to the maximum scale that still fits on screen.
                float sw = mc.getWindow().getScaledWidth();
                float sh = mc.getWindow().getScaledHeight();
                float baseW = Math.min(640f, Math.max(260f, sw - 40f));
                float baseH = Math.min(360f, Math.max(180f, sh - 60f));
                float fit = Math.min((sw - 40f) / Math.max(1f, baseW), (sh - 60f) / Math.max(1f, baseH));
                float max = Math.min(1.2f, Math.max(0.6f, fit));

                // Keep user range; readability is handled by font compensation below.
                return clamp(requested, 0.6f, max);
            }
        } catch (Throwable ignored) {}
        return 1.0f;
    }

    private void drawFontScaled(DrawContext ctx, dev.simplevisuals.client.util.renderer.fonts.Instance font, String text, float x, float y, Color color) {
        // When UI is scaled down, fonts become too small. We partially compensate reminding that scissor
        // and hit-tests are still computed in unscaled coords.
        float s = lastUiScale;
        if (s >= 0.999f) {
            Render2D.drawFont(ctx.getMatrices(), font, text, x, y, color);
            return;
        }

        float inv = (float) (1.0 / Math.sqrt(Math.max(0.0001, s))); // net font scale becomes sqrt(s)
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(inv, inv, 1f);
        ctx.getMatrices().translate(-x, -y, 0);
        Render2D.drawFont(ctx.getMatrices(), font, text, x, y, color);
        ctx.getMatrices().pop();
    }

    private double[] unscaleMouse(double mouseX, double mouseY) {
        float s = lastUiScale;
        if (s == 1.0f) return new double[]{mouseX, mouseY};
        double cx = mc.getWindow().getScaledWidth() / 2.0;
        double cy = mc.getWindow().getScaledHeight() / 2.0;
        return new double[]{(mouseX - cx) / s + cx, (mouseY - cy) / s + cy};
    }

    @Override
    public void init() {
        super.init();
        // Wide (landscape) ClickGUI like the reference screenshot
        // Размеры считаем в «нескейленых» координатах так, чтобы после UI-scale окно не вылезало за экран.
        lastUiScale = getUiScale();
        updateLayoutForScale(lastUiScale);
        this.x = (mc.getWindow().getScaledWidth() - this.width) / 2f;
        this.y = (mc.getWindow().getScaledHeight() - this.height) / 2f; // фиксированная позиция по центру

        buildComponentsCache();
        scrollY = 0f;
        scrollYTarget = 0f;
        tabScrollY = 0f;

        // Keep ClickGUI light/dark visuals in sync with effective colors
        Color bg = themeManager.getBackgroundColor();
        if (bg != null) {
            double l = (0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue()) / 255.0;
            guiLightMode = l > 0.55;
        } else {
            guiLightMode = isLightTheme(themeManager.getCurrentTheme());
        }

        closing = false;
        yAnimation.update(true); // Animation for opening (fade)
    }

    private void updateLayoutForScale(float uiScale) {
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();

        // Layout size should not depend on scale (scale is applied via matrices).
        this.width = Math.min(640f, Math.max(260f, sw - 40f));
        this.height = Math.min(360f, Math.max(180f, sh - 60f));

        lastLayoutScale = uiScale;
    }

    private void ensureColorWheelTexture() {
        if (colorWheelReady) return;
        colorWheelReady = true;
        try {
            int size = 160;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            float r = (size - 2) / 2f;
            float cx = (size - 1) / 2f;
            float cy = (size - 1) / 2f;

            for (int py = 0; py < size; py++) {
                for (int px = 0; px < size; px++) {
                    float dx = px - cx;
                    float dy = py - cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist > r) {
                        img.setRGB(px, py, 0x00000000);
                        continue;
                    }

                    float sat = clamp(dist / r, 0f, 1f);
                    float ang = (float) Math.atan2(dy, dx);
                    float hue = (ang / (float) (Math.PI * 2.0));
                    if (hue < 0f) hue += 1f;

                    int rgb = Color.HSBtoRGB(hue, sat, 1f);

                    // Soft edge AA
                    float edge = r - dist;
                    int a = 255;
                    if (edge < 1.4f) {
                        a = (int) (255f * clamp(edge / 1.4f, 0f, 1f));
                    }

                    int argb = (a << 24) | (rgb & 0x00FFFFFF);
                    img.setRGB(px, py, argb);
                }
            }

            AbstractTexture tex = Render2D.convert(img);
            if (tex instanceof NativeImageBackedTexture nit) {
                nit.upload();
                colorWheelTexture = nit;
            }
        } catch (Exception ignored) {
            colorWheelTexture = null;
        }
    }

    private Color getTargetColor(ThemeManager.CustomTheme ct) {
        if (themeColorTarget == 1) return ct.getSecondaryBackgroundColor();
        if (themeColorTarget == 2) return ct.getAccentColor();
        return ct.getBackgroundColor();
    }

    private void setTargetColor(ThemeManager.CustomTheme ct, Color c) {
        if (themeColorTarget == 1) ct.setSecondary(c);
        else if (themeColorTarget == 2) ct.setAccent(c);
        else ct.setBackground(c);
        themeManager.setTheme(ct);
        try { NexusVisual.getInstance().getConfigManager().saveThemeStore(); } catch (Exception ignored) {}
    }

    private void applyWheelAt(ThemeManager.CustomTheme ct, double mouseX, double mouseY, float wheelX, float wheelY, float wheelS) {
        float centerX = wheelX + wheelS / 2f;
        float centerY = wheelY + wheelS / 2f;
        float dx = (float) (mouseX - centerX);
        float dy = (float) (mouseY - centerY);
        float r = wheelS / 2f;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > r) return;

        float sat = clamp(dist / r, 0f, 1f);
        float ang = (float) Math.atan2(dy, dx);
        float hue = (ang / (float) (Math.PI * 2.0));
        if (hue < 0f) hue += 1f;

        Color cur = getTargetColor(ct);
        float[] hsb = Color.RGBtoHSB(cur.getRed(), cur.getGreen(), cur.getBlue(), null);
        // Keep brightness so you can still make dark themes.
        // But: accent often starts as pure black (v=0), which makes hue/sat changes appear "not working".
        float v = hsb[2];
        if (themeColorTarget == 2 && v < 0.08f) v = 1f;

        int rgb = Color.HSBtoRGB(hue, sat, v);
        Color out = new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, cur.getAlpha());
        setTargetColor(ct, out);
    }

    private void buildComponentsCache() {
        componentsByCategory.clear();
        for (Category cat : TABS) {
            if (cat != Category.Theme) { // Skip Theme category for module components
                List<Module> mods = new ArrayList<>(NexusVisual.getInstance().getModuleManager().getModules(cat));
                mods.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
                List<ModuleComponent> comps = new ArrayList<>(mods.size());
                for (Module m : mods) comps.add(new ModuleComponent(m));
                componentsByCategory.put(cat, comps);
            }
        }
    }

    // Method to play sound on module toggle or theme change
    private void playToggleSound(boolean wasToggled) {
        ClientSound clientSound = NexusVisual.getInstance().getModuleManager().getModule(ClientSound.class);
        if (clientSound != null && clientSound.isToggled()) {
            String soundId = wasToggled ? clientSound.getDisableSoundId() : clientSound.getEnableSoundId();
            float volume = clientSound.getVolume().getValue();
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(
                            SoundEvent.of(Identifier.of(soundId)),
                            1.0f,
                            volume
                    )
            );
        }
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            yAnimation.update(false); // Animation for closing (fade out)
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        lastUiScale = getUiScale();
        if (lastLayoutScale < 0f || Math.abs(lastLayoutScale - lastUiScale) > 0.001f) {
            updateLayoutForScale(lastUiScale);
        }
        double[] um = unscaleMouse(mouseX, mouseY);
        int mx = (int) um[0];
        int my = (int) um[1];

        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(lastUiScale, lastUiScale, 1f);
        context.getMatrices().translate(-cx, -cy, 0);

        float targetY = (mc.getWindow().getScaledHeight() - this.height) / 2f;

        if (closing) {
            yAnimation.update(false);
            // Плавное смещение к центру при закрытии (чуть ниже → к центру)
            float offset = (1f - yAnimation.getValue()) * 12f;
            this.y = targetY + offset;
            if (yAnimation.getValue() <= 0.01f) {
                NexusVisual.getInstance().getModuleManager().getModule(UI.class).setToggled(false);
                super.close();
                context.getMatrices().pop();
                return;
            }
        } else {
            yAnimation.update(true);
            // Появление: старт чуть ниже и плавно встаём по центру
            float offset = (1f - yAnimation.getValue()) * 12f;
            this.y = targetY + offset;
        }

        this.x = (mc.getWindow().getScaledWidth() - this.width) / 2f;

        // Используем fade по альфе + небольшой вертикальный оффсет для контента
        uiAlpha = Math.max(0f, Math.min(1f, yAnimation.getValue()));
        contentOffsetY = (1f - uiAlpha) * 8f;

        int alpha = (int) (255 * uiAlpha);
        // Modern glassmorphism-inspired design
        float r = 16f;
        Color panelBg = guiLightMode
            ? new Color(255, 255, 255, (int) (240 * uiAlpha))
            : new Color(18, 18, 24, (int) (245 * uiAlpha));
        // Very subtle border
        Color border = guiLightMode
            ? new Color(0, 0, 0, (int) (25 * uiAlpha))
            : new Color(255, 255, 255, (int) (18 * uiAlpha));

        Render2D.drawRoundedRect(context.getMatrices(), x, y, width, height, r, panelBg);
        Render2D.drawBorder(context.getMatrices(), x, y, width, height, r, 1f, 1f, border);

        renderHeader(context);
        renderCategories(context, mx, my);
        renderTopDescription(context);
        renderModulesArea(context, mx, my, delta);
        renderBottomHints(context);

        // RGB garland border disabled for a cleaner modern look

        context.getMatrices().pop();
    }

    // RGB гирлянда по всему периметру ClickGUI
    private void renderRgbGarlandBorder(DrawContext ctx) {
        if (uiAlpha <= 0f) return;

        // Make sure nothing clips the garland (some panels enable scissor).
        // DrawContext uses a stack-based scissor; disabling without enabling causes underflow.
        // We push a full-screen scissor and pop it after rendering to restore previous state.
        ctx.enableScissor(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());

        // Use nanoTime + double math so colors don't "freeze" into red due to float precision.
        double t = System.nanoTime() * 1.0e-9; // seconds
        double speed = 0.14; // cycles per second
        int totalBulbs = 72; // плотнее, чтобы выглядело ровнее
        float bulbSize = 5.0f;
        float offset = 4.0f; // ближе к краю, чтобы было полностью видно

        // Периметр: top -> right -> bottom -> left
        float perimeter = (width - 2 * offset) * 2 + (height - 2 * offset) * 2;
        float stepDist = perimeter / totalBulbs;

        for (int i = 0; i < totalBulbs; i++) {
            float dist = i * stepDist;
            float[] pos = getPerimeterPos(dist, offset);
            float px = pos[0];
            float py = pos[1];

            // RGB цвет с анимацией по позиции
            // Each bulb has its own hue offset + time-shifted wave => real RGB, all different colors.
            float hue = (float) ((t * speed + (double) i / (double) totalBulbs) % 1.0);
            float twinkle = 0.70f + 0.30f * (float) Math.sin(t * 6.0 + i * 0.55f);
            float brightness = 0.80f + 0.20f * (float) Math.sin(t * 3.5 + i * 0.35f);
            float b = Math.max(0.0f, Math.min(1.0f, brightness));
            int rgb = Color.HSBtoRGB(hue, 0.90f, b);

            int bulbAlpha = (int) (240f * uiAlpha * twinkle);
            float sizePulse = 0.85f + 0.15f * (float) Math.sin(t * 8.0 + i * 0.65f);
            float s = bulbSize * sizePulse;

            Color bulbCol = new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, bulbAlpha);
            Color hi = new Color(255, 255, 255, (int) (85f * uiAlpha * twinkle));

            // лампочка
            Render2D.drawRoundedRect(ctx.getMatrices(), px - s / 2f, py - s / 2f, s, s, s / 2f, bulbCol);
            // блик
            float hiSize = 1.4f + 0.35f * twinkle;
            Render2D.drawRoundedRect(ctx.getMatrices(), px - 0.9f, py - 0.9f, hiSize, hiSize, hiSize / 2f, hi);
        }

        ctx.disableScissor();
    }

    // Возвращает [x, y] позицию на периметре по расстоянию от начала (верхний левый угол)
    private float[] getPerimeterPos(float dist, float offset) {
        float left = x + offset;
        float right = x + width - offset;
        float top = y + offset + contentOffsetY;
        float bottom = y + height - offset + contentOffsetY;

        float topLen = right - left;
        float rightLen = bottom - top;
        float bottomLen = right - left;
        float leftLen = bottom - top;

        if (dist < topLen) {
            // top edge (left to right)
            return new float[]{left + dist, top};
        }
        dist -= topLen;
        if (dist < rightLen) {
            // right edge (top to bottom)
            return new float[]{right, top + dist};
        }
        dist -= rightLen;
        if (dist < bottomLen) {
            // bottom edge (right to left)
            return new float[]{right - dist, bottom};
        }
        dist -= bottomLen;
        // left edge (bottom to top)
        return new float[]{left, bottom - dist};
    }

    private void renderHeader(DrawContext ctx) {
        if (uiAlpha <= 0f) return;

        float headerH = HEADER_H;
        Color txt = guiLightMode
            ? new Color(30, 30, 35, (int) (255 * uiAlpha))
            : new Color(245, 245, 250, (int) (255 * uiAlpha));

        // Minimal header - just title, no background bar
        float headerY = y + contentOffsetY;
        
        // Subtle gradient line under header
        Color accent = ThemeManager.getInstance().getAccentColor();
        Color lineColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (80 * uiAlpha));
        Render2D.drawRoundedRect(ctx.getMatrices(), x + 20f, headerY + headerH - 1f, width - 40f, 2f, 1f, lineColor);

        String title = "Nexus Visual";
        float fs = 10f;
        float maxW = width - 44f;
        while (fs > 7.5f && Fonts.MEDIUM.getWidth(title, fs) > maxW) fs -= 0.5f;
        float w = Fonts.MEDIUM.getWidth(title, fs);
        float tx = x + (width - w) / 2f;
        float ty = y + 8f + contentOffsetY;
        drawFontScaled(ctx, Fonts.MEDIUM.getFont(fs), title, tx, ty, txt);

        // Theme toggle button - modern circle
        themeToggleS = 18f;
        themeToggleX = x + width - themeToggleS - 12f;
        themeToggleY = y + 6f + contentOffsetY;

        Color btnBg = guiLightMode
            ? new Color(240, 240, 245, (int) (255 * uiAlpha))
            : new Color(35, 35, 42, (int) (255 * uiAlpha));
        float btnR = themeToggleS / 2f;
        Render2D.drawRoundedRect(ctx.getMatrices(), themeToggleX, themeToggleY, themeToggleS, themeToggleS, btnR, btnBg);

        if (guiLightMode) drawSunIcon(ctx);
        else drawMoonIcon(ctx);
    }

    private void drawSunIcon(DrawContext ctx) {
        float cx = themeToggleX + themeToggleS / 2f;
        float cy = themeToggleY + themeToggleS / 2f;

        float r = 3.2f;
        Color c = new Color(255, 190, 60, (int) (235 * uiAlpha));

        // flat core
        Render2D.drawRoundedRect(ctx.getMatrices(), cx - r, cy - r, r * 2f, r * 2f, r, c);

        // rays (8 directions)
        float rayOuter = 6.0f;
        float rayInner = r + 1.6f;
        float w = 1.0f;
        for (int i = 0; i < 8; i++) {
            double ang = (Math.PI / 4.0) * i;
            float ox = (float) Math.cos(ang);
            float oy = (float) Math.sin(ang);
            float x1 = cx + ox * rayInner;
            float y1 = cy + oy * rayInner;
            float x2 = cx + ox * rayOuter;
            float y2 = cy + oy * rayOuter;
            Render2D.drawLine(ctx.getMatrices(), x1, y1, x2, y2, w, c);
        }
    }

    private void drawMoonIcon(DrawContext ctx) {
        float cx = themeToggleX + themeToggleS / 2f;
        float cy = themeToggleY + themeToggleS / 2f;
        float r = 4.4f;

        Color moon = new Color(220, 220, 240, (int) (235 * uiAlpha));
        Color cut = guiLightMode
                ? new Color(235, 235, 235, (int) (255 * uiAlpha))
                : new Color(30, 30, 30, (int) (255 * uiAlpha));

        // base disk
        Render2D.drawRoundedRect(ctx.getMatrices(), cx - r, cy - r, r * 2f, r * 2f, r, moon);
        // cut-out for crescent
        Render2D.drawRoundedRect(ctx.getMatrices(), cx - r + 2.3f, cy - r + 0.4f, r * 2f, r * 2f, r, cut);
        // no stars/glow for minimal look
    }

    private void renderBottomHints(DrawContext ctx) {
        if (uiAlpha <= 0f) return;

        String hint1 = I18n.translate("simplevisuals.clickgui.hint.bind");
        String hint2 = I18n.translate("simplevisuals.clickgui.hint.settings");
        String hint3 = I18n.translate("simplevisuals.clickgui.hint.hud_move");

        float gap = 2f;
        float fontSize = 8f;
        float lineHeight = Fonts.MEDIUM.getHeight(fontSize) + gap;

        float startY = y + height + 8f; // ниже ClickGUI
        int textA = (int) (230 * uiAlpha);
        // Текст рисуется поверх мира (без плашки), поэтому в светлой теме "тёмный" текст часто теряется.
        // Делаем цвет всегда читаемым.
        Color textColor = new Color(255, 255, 255, textA);

        float screenCenterX = mc.getWindow().getScaledWidth() / 2f;

        // line 1
        float w1 = Fonts.MEDIUM.getWidth(hint1, fontSize);
        float x1 = screenCenterX - w1 / 2f;
        drawFontScaled(ctx, Fonts.MEDIUM.getFont(fontSize), hint1, x1, startY, textColor);

        // line 2
        float w2 = Fonts.MEDIUM.getWidth(hint2, fontSize);
        float x2 = screenCenterX - w2 / 2f;
        drawFontScaled(ctx, Fonts.MEDIUM.getFont(fontSize), hint2, x2, startY + lineHeight, textColor);

        // line 3
        float w3 = Fonts.MEDIUM.getWidth(hint3, fontSize);
        float x3 = screenCenterX - w3 / 2f;
        drawFontScaled(ctx, Fonts.MEDIUM.getFont(fontSize), hint3, x3, startY + lineHeight * 2f, textColor);
    }

    private void renderPanelGlow(DrawContext ctx) {
        // Blur удалён
    }

    private void renderCategories(DrawContext ctx, int mouseX, int mouseY) {
        // Left vertical sidebar categories (landscape layout)
        float sidebarX = x + 8f;
        float sidebarY = y + HEADER_H + 10f + contentOffsetY;
        float sidebarW = SIDEBAR_W;
        float sidebarH = height - (sidebarY - y) - 10f;

        int a = (int) (220 * uiAlpha);
        Color accent = themeManager.getAccentColor();
        if (accent == null) accent = new Color(140, 140, 140);

        // Modern minimal sidebar - no background, cleaner look
        Color border = guiLightMode
            ? new Color(0, 0, 0, (int) (15 * uiAlpha))
            : new Color(255, 255, 255, (int) (12 * uiAlpha));
        float rr = 12f;
        // Just a subtle separator line instead of full border
        Render2D.drawRect(ctx.getMatrices(), sidebarX + sidebarW - 1f, sidebarY + 8f, 1f, sidebarH - 16f, border);

        float itemX = sidebarX + SIDEBAR_PAD;
        float itemY = sidebarY + SIDEBAR_PAD;
        float itemW = sidebarW - SIDEBAR_PAD * 2f;

        for (Category cat : TABS) {
            boolean active = cat == selectedCategory;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= itemY && mouseY <= itemY + SIDEBAR_ITEM_H;
            int ia = (int) (255 * uiAlpha);

            // Modern pill-style tabs
            Color itemBg;
            if (active) {
                itemBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (45 * uiAlpha));
            } else if (hovered) {
                itemBg = guiLightMode ? new Color(0, 0, 0, (int) (15 * uiAlpha)) : new Color(255, 255, 255, (int) (15 * uiAlpha));
            } else {
                itemBg = new Color(0, 0, 0, 0); // transparent
            }

            float itemR = 8f;
            if (itemBg.getAlpha() > 0) {
                Render2D.drawRoundedRect(ctx.getMatrices(), itemX, itemY, itemW, SIDEBAR_ITEM_H, itemR, itemBg);
            }

            // Beautiful outline for active tab (no shadows)
            if (active) {
                Color outer = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (150 * uiAlpha));
                Color inner = guiLightMode
                    ? new Color(255, 255, 255, (int) (90 * uiAlpha))
                    : new Color(0, 0, 0, (int) (85 * uiAlpha));
                Render2D.drawBorder(ctx.getMatrices(), itemX, itemY, itemW, SIDEBAR_ITEM_H, itemR, 1f, 1f, outer);
                Render2D.drawBorder(ctx.getMatrices(), itemX + 1f, itemY + 1f, itemW - 2f, SIDEBAR_ITEM_H - 2f, itemR - 1f, 1f, 1f, inner);
            }
            
            Color text = active
                    ? (guiLightMode ? new Color(25, 25, 25, ia) : new Color(235, 235, 235, ia))
                    : (guiLightMode ? new Color(90, 90, 90, ia) : new Color(170, 170, 170, ia));
            String name = cat.name();
            float ty = itemY + (SIDEBAR_ITEM_H - Fonts.MEDIUM.getHeight(9f)) / 2f + 1f;
            drawFontScaled(ctx, Fonts.MEDIUM.getFont(9f), name, itemX + 10f, ty, text);

            itemY += SIDEBAR_ITEM_H + SIDEBAR_GAP;
        }
    }

    private void renderTopDescription(DrawContext ctx) {
        if (description == null || description.isEmpty()) return;

        String descText = I18n.translate(description);
        float textW = Fonts.MEDIUM.getWidth(descText, 9f);

        // Place just above the panel and make it overhang the panel width
        float bgOverhang = 12f; // how far to extend past the panel on each side
        float bgX = x - bgOverhang;
        float bgY = y - 14f; // slightly above the ClickGUI panel
        float bgW = width + bgOverhang * 2f;
        float bgH = 14f;

        // Center text relative to the ClickGUI panel
        float textX = x + (width - textW) / 2f;
        float textY = bgY - 5f;

        int panelAlpha = (int) (120 * uiAlpha);
        int textAlpha = (int) (255 * uiAlpha);

        drawFontScaled(ctx, Fonts.MEDIUM.getFont(9f), descText, textX, textY,
            guiLightMode
                ? new Color(20, 20, 20, textAlpha)
                : new Color(255, 255, 255, textAlpha));
        description = "";
    }

    private void startScissorScaled(DrawContext ctx, float rx, float ry, float rw, float rh) {
        // Flush batched UI before changing scissor state to keep correct clipping/order.
        ClickGuiDraw.flush();
        float s = lastUiScale;
        if (s == 1.0f) {
            Render2D.startScissor(ctx, rx, ry, rw, rh);
            return;
        }
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        float sx = cx + (rx - cx) * s;
        float sy = cy + (ry - cy) * s;
        float sw = rw * s;
        float sh = rh * s;
        Render2D.startScissor(ctx, sx, sy, sw, sh);
    }

    // Exposed for ClickGUI components that need scale-aware clipping.
    public void startScissorScaledPublic(DrawContext ctx, float rx, float ry, float rw, float rh) {
        startScissorScaled(ctx, rx, ry, rw, rh);
    }

    public void stopScissorPublic(DrawContext ctx) {
        // Flush inside scissor before disabling it.
        ClickGuiDraw.flush();
        Render2D.stopScissor(ctx);
    }

    private void renderModulesArea(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Content area (to the right of sidebar)
        float sidebarX = x + 8f;
        float sidebarY = y + HEADER_H + 10f + contentOffsetY;
        float sidebarW = SIDEBAR_W;
        float contentX = sidebarX + sidebarW + 10f;
        float contentY = sidebarY;
        float contentW = (x + width - 8f) - contentX;
        float contentH = height - (contentY - y) - 10f;

        // Settings panel lives inside the main panel on the right
        ModuleComponent target = activeSettings;
        boolean hasSettings = selectedCategory != Category.Theme && target != null && !target.getComponents().isEmpty();
        boolean hasThemeEditor = selectedCategory == Category.Theme && (themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme);
        settingsAnimation.update(hasSettings);
        float anim = settingsAnimation.getValue();

        float settingsW = 190f;
        float settingsGap = 10f;
        float modulesX = contentX;
        float modulesY = contentY;
        float modulesW = (hasSettings || hasThemeEditor) ? (contentW - settingsW - settingsGap) : contentW;
        float modulesH = contentH;

        // Smooth scroll interpolation for main list
        float listSmooth = 0.18f;
        scrollY += (scrollYTarget - scrollY) * listSmooth;
        scrollY = clamp(scrollY, 0f, maxScroll);

        startScissorScaled(ctx, modulesX, modulesY, modulesW, modulesH);

        if (selectedCategory == Category.Theme) {
            float themeY = modulesY + 2f - scrollY;
            float totalHeight = 0f;

            float itemH = 28f;
            float itemGap = 6f;
            float cardW = modulesW;

            // Create custom theme card
            boolean hoveredCreate = mouseX >= modulesX && mouseX <= modulesX + cardW && mouseY >= themeY && mouseY <= themeY + itemH;
            int createCardA = (int) (185 * uiAlpha);
            Color createBg = hoveredCreate
                ? (guiLightMode ? new Color(245, 245, 245, createCardA) : new Color(34, 34, 34, createCardA))
                : (guiLightMode ? new Color(235, 235, 235, createCardA) : new Color(24, 24, 24, createCardA));
            Render2D.drawRoundedRect(ctx.getMatrices(), modulesX, themeY, cardW, itemH, 6f, createBg);
            drawFontScaled(ctx, Fonts.MEDIUM.getFont(9f), "+ Custom Theme", modulesX + 10f, themeY + 9f,
                guiLightMode
                    ? new Color(25, 25, 25, (int) (245 * uiAlpha))
                    : new Color(235, 235, 235, (int) (245 * uiAlpha)));

            themeY += itemH + itemGap;
            totalHeight += itemH + itemGap;

            for (ThemeManager.Theme theme : themeManager.getAvailableThemes()) {
            boolean isCurrent = theme == themeManager.getCurrentTheme();
            boolean hovered = mouseX >= modulesX && mouseX <= modulesX + cardW && mouseY >= themeY && mouseY <= themeY + itemH;

            int cardA = (int) (185 * uiAlpha);
            Color cardBg = hovered
                ? (guiLightMode ? new Color(245, 245, 245, cardA) : new Color(34, 34, 34, cardA))
                : (guiLightMode ? new Color(235, 235, 235, cardA) : new Color(24, 24, 24, cardA));
            Render2D.drawRoundedRect(ctx.getMatrices(), modulesX, themeY, cardW, itemH, 6f, cardBg);

            // Название
            Color nameColor = isCurrent
                ? (guiLightMode ? new Color(25, 25, 25, (int) (255 * uiAlpha)) : new Color(255, 255, 255, (int) (255 * uiAlpha)))
                : (guiLightMode ? new Color(70, 70, 70, (int) (240 * uiAlpha)) : new Color(210, 210, 210, (int) (240 * uiAlpha)));
            drawFontScaled(ctx, Fonts.MEDIUM.getFont(9f),
                theme.getName(), modulesX + 10f, themeY + 9f, nameColor);

            // Превью (градиент bg -> secondary)
            float previewW = 52f;
            float previewH = 14f;
            float previewX = modulesX + cardW - previewW - 8f;
            float previewY = themeY + (itemH - previewH) / 2f;
            Render2D.drawRoundedRect(ctx.getMatrices(), previewX, previewY, previewW, previewH, 4f,
                guiLightMode
                        ? new Color(255, 255, 255, (int) (140 * uiAlpha))
                        : new Color(0, 0, 0, (int) (120 * uiAlpha)));
            float innerX = previewX + 1f;
            float innerY = previewY + 1f;
            float innerW = previewW - 2f;
            float innerH = previewH - 2f;
            float r = 3f;
            float leftHalfW = innerW * 0.5f;

            // Закруглённое превью цветов темы (вместо квадратного градиента)
            Render2D.drawRoundedRect2(ctx.getMatrices(), innerX, innerY, leftHalfW, innerH,
                r, r, 0f, 0f, theme.getBackgroundColor());
            Render2D.drawRoundedRect2(ctx.getMatrices(), innerX + leftHalfW, innerY, innerW - leftHalfW, innerH,
                0f, 0f, r, r, theme.getSecondaryBackgroundColor());

            // Тонкая рамка по акценту (hover/selected)
            if (hovered || isCurrent) {
                Color acc = theme.getAccentColor();
                int a = (int) ((hovered ? 110 : 160) * uiAlpha);
                Render2D.drawBorder(ctx.getMatrices(), modulesX, themeY, cardW, itemH, 6f,
                    0.6f, 0.6f, new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), a));
            }

            themeY += itemH + itemGap;
            totalHeight += itemH + itemGap;
            }

            maxScroll = Math.max(0f, totalHeight - modulesH);
            scrollYTarget = clamp(scrollYTarget, 0f, maxScroll);
            scrollY = clamp(scrollY, 0f, maxScroll);
        } else {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            int cols = COLS;
            float gap = GAP;
            float colW = (modulesW - gap * (cols - 1)) / cols;
            float[] baseY = new float[cols];
            Arrays.fill(baseY, modulesY + 2f);
            float maxBottom = modulesY;
            int placed = 0;

            // Batched pass for module cards (ModuleComponent is already migrated to ClickGuiDraw).
            ClickGuiDraw.begin(ctx);
            for (ModuleComponent mcComp : comps) {
                int col = placed % cols;
                float cx = modulesX + col * (colW + gap);
                float cyDraw = baseY[col] - scrollY;
                mcComp.setX(cx);
                mcComp.setY(cyDraw);
                mcComp.setWidth(colW);
                mcComp.setRenderExternally(true);
                mcComp.setGlobalAlpha(uiAlpha);
                // принудительно скрыть внутренние дети (не рисовать раскрытые настройки слева)
                if (mcComp.getOpenAnimation().getValue() > 0f && mcComp != activeSettings) {
                    // оставляем как есть — внутренний рендер отключён флагом renderExternally
                }
                mcComp.render(ctx, mouseX, mouseY, delta);
                float totalH = mcComp.getHeight();
                baseY[col] += totalH + gap;
                maxBottom = Math.max(maxBottom, baseY[col]);
                placed++;
            }
            ClickGuiDraw.end();
            float contentBottom = modulesY + modulesH;
            maxScroll = Math.max(0f, (maxBottom - gap) - contentBottom);
            scrollYTarget = clamp(scrollYTarget, 0f, maxScroll);
            scrollY = clamp(scrollY, 0f, maxScroll);

            // Мягкий оверлей для плавного исчезновения модулей при открытии/закрытии
            float tModules = 1f - uiAlpha;
            float easedModules = tModules * tModules * (3f - 2f * tModules); // smoothstep
            int modulesOverlayAlpha = (int) (140 * easedModules);
            if (modulesOverlayAlpha > 2) {
                Render2D.drawRoundedRect(ctx.getMatrices(), modulesX + 1f, modulesY + 1f, modulesW - 2f, modulesH - 2f, 3f,
                        guiLightMode
                                ? new Color(255, 255, 255, modulesOverlayAlpha)
                                : new Color(30, 30, 30, modulesOverlayAlpha));
            }
        }

        Render2D.stopScissor(ctx);

        // Scrollbar for modules/themes list when content overflows (rendered to the right of ClickGUI)
        if (maxScroll > 0.5f) {
            float trackX = modulesX + modulesW - 3.5f;
            float trackY = modulesY;
            float trackW = 3f;
            float trackH = modulesH;

            boolean hoverScroll = mouseX >= trackX - 6f && mouseX <= trackX + trackW + 6f && mouseY >= trackY && mouseY <= trackY + trackH;
            int trackA = (int) ((hoverScroll ? 40f : 26f) * uiAlpha);
            Color trackCol = guiLightMode
                ? new Color(0, 0, 0, trackA)
                : new Color(255, 255, 255, (int) ((hoverScroll ? 26f : 16f) * uiAlpha));

            // Slim track
            Render2D.drawRoundedRect(ctx.getMatrices(), trackX + 1f, trackY + 6f, 1f, trackH - 12f, 0.5f, trackCol);

            float visibleRatio = modulesH / Math.max(modulesH + maxScroll, 1f);
            float thumbH = Math.max(14f, trackH * visibleRatio);
            float maxThumbTravel = trackH - thumbH;
            float scrollRatio = maxScroll <= 0f ? 0f : (scrollY / maxScroll);
            float thumbY = trackY + maxThumbTravel * scrollRatio;
                Color accent = themeManager.getAccentColor();
                if (accent == null) accent = new Color(140, 140, 140);

                int thumbA = (int) ((hoverScroll ? 210f : 175f) * uiAlpha);
                Color thumbColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.min(235, thumbA));
            Render2D.drawRoundedRect(ctx.getMatrices(), trackX, thumbY, trackW, thumbH, trackW / 2f, thumbColor);
        }

        if (anim > 0.01f) {
            // right settings panel inside ClickGUI (slides in with fade)
            float basePanelX = modulesX + modulesW + settingsGap;
            float panelY = modulesY;
            float panelW = settingsW;
            float panelH = modulesH;
            float slideOffset = (1f - anim) * 24f;
            float drawPanelX = basePanelX + slideOffset;

            int settingsAlpha = (int) (255 * Math.min(1f, anim * uiAlpha));
            float smoothPanel = (float) (Math.pow(Math.min(1f, anim * uiAlpha), 2) * (3 - 2 * Math.min(1f, anim * uiAlpha)));
            int panelA = (int) (255 * smoothPanel);

            // Убрали blur под панелью настроек: лёгкая подложка
            Render2D.drawRoundedRect(ctx.getMatrices(), drawPanelX - 1f, panelY - 1f, panelW + 2f, panelH + 2f, 3f,
                    new Color(255, 255, 255, Math.min(18, (int) (18f * anim * uiAlpha))));

                Render2D.drawRoundedRect(ctx.getMatrices(), drawPanelX, panelY, panelW, panelH, 3f,
                    guiLightMode
                        ? new Color(245, 245, 245, panelA)
                        : new Color(30, 30, 30, panelA));

            float rContentX = drawPanelX + 8f;
            float rContentY = panelY + 8f;
            float rContentW = panelW - 16f;
            float rContentH = panelH - 16f;

            float total = 0f;
            if (hasSettings) {
                target.setGlobalAlpha(Math.min(1f, anim * uiAlpha));
                // Batched pass for settings components (settings component impls are migrated to ClickGuiDraw).
                ClickGuiDraw.begin(ctx);
                total = target.renderSettingsExternally(ctx, rContentX, rContentY, rContentW,
                        rContentX, rContentY, rContentW, rContentH, mouseX, mouseY, delta, settingsScrollY);
                ClickGuiDraw.end();
            }
            settingsMaxScroll = Math.max(0f, total - rContentH);
            scrollYTarget = clamp(scrollYTarget, 0f, maxScroll);
            scrollY = clamp(scrollY, 0f, maxScroll);
            settingsScrollYTarget = clamp(settingsScrollYTarget, 0f, settingsMaxScroll);
            // Smooth scroll interpolation for settings list
            float settingsSmooth = 0.2f;
            settingsScrollY += (settingsScrollYTarget - settingsScrollY) * settingsSmooth;
            settingsScrollY = clamp(settingsScrollY, 0f, settingsMaxScroll);

            // vertical scrollbar on the right of settings content
            if (settingsMaxScroll > 0.5f) {
                float trackX = drawPanelX + panelW - 3.5f;
                float trackY = rContentY;
                float trackW = 3f;
                float trackH = rContentH;

                boolean hoverScroll = mouseX >= trackX - 6f && mouseX <= trackX + trackW + 6f && mouseY >= trackY && mouseY <= trackY + trackH;
                int trackA = (int) ((hoverScroll ? 40f : 26f) * anim * uiAlpha);
                Color trackCol = guiLightMode
                    ? new Color(0, 0, 0, trackA)
                    : new Color(255, 255, 255, (int) ((hoverScroll ? 26f : 16f) * anim * uiAlpha));

                // Slim track
                Render2D.drawRoundedRect(ctx.getMatrices(), trackX + 1f, trackY + 6f, 1f, trackH - 12f, 0.5f, trackCol);

                float visibleRatio = rContentH / Math.max(rContentH + settingsMaxScroll, 1f);
                float thumbH = Math.max(14f, trackH * visibleRatio);
                float maxThumbTravel = trackH - thumbH;
                float scrollRatio = settingsMaxScroll <= 0f ? 0f : (settingsScrollY / settingsMaxScroll);
                float thumbY = trackY + maxThumbTravel * scrollRatio;
                Color accent = themeManager.getAccentColor();
                if (accent == null) accent = new Color(140, 140, 140);

                int thumbA = (int) ((hoverScroll ? 210f : 175f) * anim * uiAlpha);
                Color thumbColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.min(235, thumbA));
                Render2D.drawRoundedRect(ctx.getMatrices(), trackX, thumbY, trackW, thumbH, trackW / 2f, thumbColor);
            }

            // Мягкий скруглённый оверлей над содержимым (снижаем альфу и убираем жёсткие края) с плавной кривой
            float tSettings = 1f - Math.min(1f, anim * uiAlpha);
            float easedSettings = tSettings * tSettings * (3f - 2f * tSettings); // smoothstep
            int contentOverlayAlpha = (int) (160 * easedSettings);
            if (contentOverlayAlpha > 6) {
                Render2D.drawRoundedRect(ctx.getMatrices(), rContentX, rContentY, rContentW, rContentH, 2f,
                        guiLightMode
                                ? new Color(255, 255, 255, contentOverlayAlpha)
                                : new Color(30, 30, 30, contentOverlayAlpha));
            }
        }

        // Theme editor panel (inside ClickGUI, right side)
        if (hasThemeEditor) {
            float panelX = modulesX + modulesW + settingsGap;
            float panelY = modulesY;
            float panelW = settingsW;
            float panelH = modulesH;
            int panelA = (int) (255 * uiAlpha);

            Render2D.drawRoundedRect(ctx.getMatrices(), panelX, panelY, panelW, panelH, 3f,
                    guiLightMode ? new Color(245, 245, 245, panelA) : new Color(30, 30, 30, panelA));

            ThemeManager.CustomTheme ct = (ThemeManager.CustomTheme) themeManager.getCurrentTheme();
            if (ct != themeNameTarget) {
                themeNameTarget = ct;
                themeNameBuffer = ct.getName() == null ? "" : ct.getName();
                themeNameEditing = false;
            }
            float px = panelX + 10f;
            float py = panelY + 10f;
            Color label = guiLightMode ? new Color(40, 40, 40, (int) (240 * uiAlpha)) : new Color(220, 220, 220, (int) (240 * uiAlpha));

            drawFontScaled(ctx, Fonts.MEDIUM.getFont(9f), "Custom Theme", px, py, label);

                // Delete button (custom themes only)
                float delW = 54f;
                float delH = 16f;
                float delX = panelX + panelW - 10f - delW;
                float delY = panelY + 10f;
                Color delBg = guiLightMode ? new Color(235, 235, 235, panelA) : new Color(20, 20, 20, panelA);
                Color delBorder = guiLightMode ? new Color(0, 0, 0, (int) (35 * uiAlpha)) : new Color(255, 255, 255, (int) (30 * uiAlpha));
                Render2D.drawRoundedRect(ctx.getMatrices(), delX, delY, delW, delH, 4f, delBg);
                Render2D.drawBorder(ctx.getMatrices(), delX, delY, delW, delH, 4f, 0.6f, 0.6f, delBorder);
                String delText = "Delete";
                float delFont = 8f;
                float delTextW = Fonts.MEDIUM.getWidth(delText, delFont);
                float delTextX = delX + (delW - delTextW) / 2f;
                float delTextY = delY + (delH - delFont) / 2f + 0.5f;
                drawFontScaled(ctx, Fonts.MEDIUM.getFont(delFont), delText, delTextX, delTextY,
                    guiLightMode ? new Color(25, 25, 25, (int) (240 * uiAlpha)) : new Color(235, 235, 235, (int) (240 * uiAlpha)));
            py += 16f;

            // Name input
            drawFontScaled(ctx, Fonts.MEDIUM.getFont(8f), "Name", px, py, guiLightMode ? new Color(60, 60, 60, (int) (235 * uiAlpha)) : new Color(210, 210, 210, (int) (235 * uiAlpha)));
            py += 12f;
            float nameW = panelW - 20f;
            float nameH = 18f;
            Color nameBg = guiLightMode ? new Color(235, 235, 235, (int) (235 * uiAlpha)) : new Color(20, 20, 20, (int) (235 * uiAlpha));
            Color nameBorder = guiLightMode ? new Color(0, 0, 0, (int) (30 * uiAlpha)) : new Color(255, 255, 255, (int) (25 * uiAlpha));
            Render2D.drawRoundedRect(ctx.getMatrices(), px, py, nameW, nameH, 4f, nameBg);
                Render2D.drawBorder(ctx.getMatrices(), px, py, nameW, nameH, 4f, 0.6f, 0.6f, nameBorder);
            String shown = themeNameEditing ? themeNameBuffer : (ct.getName() == null ? "" : ct.getName());
            if (shown.length() > 22) shown = shown.substring(0, 22);
                drawFontScaled(ctx, Fonts.MEDIUM.getFont(8f), shown, px + 6f, py + 5f,
                    guiLightMode ? new Color(25, 25, 25, (int) (245 * uiAlpha)) : new Color(235, 235, 235, (int) (245 * uiAlpha)));
            py += nameH + 10f;

                // Target button (single square): cycles BG / SEC / ACC
                float btnS = 18f;
                float bx = px + (panelW - 20f - btnS) / 2f;
                float by = py;
                Color bb = guiLightMode ? new Color(235, 235, 235, (int) (210 * uiAlpha)) : new Color(25, 25, 25, (int) (210 * uiAlpha));
                Color bbSel = guiLightMode ? new Color(225, 225, 225, (int) (240 * uiAlpha)) : new Color(45, 45, 45, (int) (240 * uiAlpha));
                Render2D.drawRoundedRect(ctx.getMatrices(), bx, by, btnS, btnS, 4f, bbSel);
                Render2D.drawBorder(ctx.getMatrices(), bx, by, btnS, btnS, 4f, 0.6f, 0.6f,
                    guiLightMode ? new Color(0, 0, 0, (int) (30 * uiAlpha)) : new Color(255, 255, 255, (int) (25 * uiAlpha)));

                String targetLabel = themeColorTarget == 1 ? "SC" : (themeColorTarget == 2 ? "AC" : "BG");
                float tf = 7f;
                float tw = Fonts.MEDIUM.getWidth(targetLabel, tf);
                float th = Fonts.MEDIUM.getHeight(tf);
                drawFontScaled(ctx, Fonts.MEDIUM.getFont(tf), targetLabel,
                    bx + (btnS - tw) / 2f,
                    by + (btnS - th) / 2f + 0.25f,
                    guiLightMode ? new Color(25, 25, 25, (int) (240 * uiAlpha)) : new Color(235, 235, 235, (int) (240 * uiAlpha)));

                py += btnS + 10f;

                // Color wheel
                ensureColorWheelTexture();
                float reserveAfterWheel = 34f; // space for alpha slider
                float wheelS = Math.min(panelW - 20f, panelH - (py - panelY) - 10f - reserveAfterWheel);
                wheelS = Math.min(wheelS, 150f);
                float wheelX = px + (panelW - 20f - wheelS) / 2f;
                float wheelY = py;

                if (colorWheelTexture != null) {
                Render2D.drawTexture(ctx.getMatrices(), wheelX, wheelY, wheelS, wheelS, wheelS / 2f, colorWheelTexture,
                    new Color(255, 255, 255, (int) (255 * uiAlpha)));
                Render2D.drawBorder(ctx.getMatrices(), wheelX, wheelY, wheelS, wheelS, wheelS / 2f, 0.6f, 0.6f,
                    guiLightMode ? new Color(0, 0, 0, (int) (40 * uiAlpha)) : new Color(255, 255, 255, (int) (35 * uiAlpha)));

                // Indicator dot for current target color
                Color cur = getTargetColor(ct);
                float[] hsb = Color.RGBtoHSB(cur.getRed(), cur.getGreen(), cur.getBlue(), null);
                float ang = (float) (hsb[0] * Math.PI * 2.0);
                float rr = (wheelS / 2f) * hsb[1];
                float cx = wheelX + wheelS / 2f;
                float cy = wheelY + wheelS / 2f;
                float ix = cx + (float) Math.cos(ang) * rr;
                float iy = cy + (float) Math.sin(ang) * rr;
                float dot = 7f;
                Render2D.drawRoundedRect(ctx.getMatrices(), ix - dot / 2f, iy - dot / 2f, dot, dot, dot / 2f,
                    new Color(0, 0, 0, (int) (190 * uiAlpha)));
                Render2D.drawRoundedRect(ctx.getMatrices(), ix - (dot - 2f) / 2f, iy - (dot - 2f) / 2f, dot - 2f, dot - 2f, (dot - 2f) / 2f,
                    new Color(255, 255, 255, (int) (235 * uiAlpha)));
                }

                // Alpha slider (for selected BG/SEC/ACC)
                float alphaBlockY = wheelY + wheelS + 10f;
                float alphaW = panelW - 20f;
                Color cur = getTargetColor(ct);
                int alphaV = cur.getAlpha();
                Color alphaLabel = guiLightMode ? new Color(60, 60, 60, (int) (235 * uiAlpha)) : new Color(210, 210, 210, (int) (235 * uiAlpha));
                drawFontScaled(ctx, Fonts.MEDIUM.getFont(8f), "Alpha: " + alphaV, px, alphaBlockY, alphaLabel);

                float trackX = px;
                float trackY = alphaBlockY + 10f;
                float trackW = alphaW;
                float trackH = 8f;
                Color track = guiLightMode ? new Color(230, 230, 230, (int) (210 * uiAlpha)) : new Color(20, 20, 20, (int) (210 * uiAlpha));
                Render2D.drawRoundedRect(ctx.getMatrices(), trackX, trackY, trackW, trackH, 4f, track);

                float tA = alphaV / 255f;
                Color fill = new Color(cur.getRed(), cur.getGreen(), cur.getBlue(), (int) (180 * uiAlpha));
                Render2D.drawRoundedRect(ctx.getMatrices(), trackX, trackY, trackW * tA, trackH, 4f, fill);
                float knobX = trackX + trackW * tA;
                float knobS = 10f;
                Color knob = guiLightMode ? new Color(35, 35, 35, (int) (230 * uiAlpha)) : new Color(235, 235, 235, (int) (230 * uiAlpha));
                Render2D.drawRoundedRect(ctx.getMatrices(), knobX - knobS / 2f, trackY - 1f, knobS, trackH + 2f, 5f, knob);
        }
    }

    private float renderRgbGroup(DrawContext ctx, float x, float y, float w, String name, Color color, int keyBase) {
        Color label = guiLightMode ? new Color(60, 60, 60, (int) (235 * uiAlpha)) : new Color(210, 210, 210, (int) (235 * uiAlpha));
        drawFontScaled(ctx, Fonts.MEDIUM.getFont(8f), name, x, y, label);
        y += 12f;

        y = renderRgbSlider(ctx, x, y, w, "R", color.getRed(), keyBase + 0, new Color(255, 90, 90, (int) (180 * uiAlpha)));
        y = renderRgbSlider(ctx, x, y, w, "G", color.getGreen(), keyBase + 1, new Color(90, 255, 140, (int) (180 * uiAlpha)));
        y = renderRgbSlider(ctx, x, y, w, "B", color.getBlue(), keyBase + 2, new Color(90, 160, 255, (int) (180 * uiAlpha)));
        return y;
    }

    private float renderRgbSlider(DrawContext ctx, float x, float y, float w, String ch, int value, int key, Color fill) {
        Color text = guiLightMode ? new Color(70, 70, 70, (int) (235 * uiAlpha)) : new Color(190, 190, 190, (int) (235 * uiAlpha));
        drawFontScaled(ctx, Fonts.MEDIUM.getFont(8f), ch + ": " + value, x, y, text);

        float trackX = x;
        float trackY = y + 10f;
        float trackW = w;
        float trackH = 8f;
        Color track = guiLightMode ? new Color(230, 230, 230, (int) (210 * uiAlpha)) : new Color(20, 20, 20, (int) (210 * uiAlpha));
        Render2D.drawRoundedRect(ctx.getMatrices(), trackX, trackY, trackW, trackH, 4f, track);

        float t = value / 255f;
        Render2D.drawRoundedRect(ctx.getMatrices(), trackX, trackY, trackW * t, trackH, 4f, fill);
        float knobX = trackX + trackW * t;
        float knobS = 10f;
        Color knob = guiLightMode ? new Color(35, 35, 35, (int) (230 * uiAlpha)) : new Color(235, 235, 235, (int) (230 * uiAlpha));
        Render2D.drawRoundedRect(ctx.getMatrices(), knobX - knobS / 2f, trackY - 1f, knobS, trackH + 2f, 5f, knob);

        return y + 22f;
    }

    private void applyThemeEditorValue(int key, int newValue) {
        if (!(themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme ct)) return;
        int v = (int) clamp(newValue, 0f, 255f);

        if (key >= 0 && key < 100) {
            Color c = ct.getBackgroundColor();
            ct.setBackground(applyChannel(c, key % 100, v));
        } else if (key >= 100 && key < 200) {
            Color c = ct.getSecondaryBackgroundColor();
            ct.setSecondary(applyChannel(c, key % 100, v));
        } else if (key >= 200 && key < 300) {
            Color c = ct.getAccentColor();
            ct.setAccent(applyChannel(c, key % 100, v));
        }
        // ensure UI updates
        themeManager.setTheme(ct);
        try { NexusVisual.getInstance().getConfigManager().saveThemeStore(); } catch (Exception ignored) {}
    }

    private Color applyChannel(Color c, int channel, int value) {
        if (c == null) c = new Color(0, 0, 0, 255);
        int a = c.getAlpha();
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        if (channel == 0) r = value;
        else if (channel == 1) g = value;
        else if (channel == 2) b = value;
        else if (channel == 3) a = value;
        return new Color(r, g, b, a);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;

        double[] um = unscaleMouse(mouseX, mouseY);
        mouseX = um[0];
        mouseY = um[1];

        // If we are editing a custom theme name, clicking elsewhere commits it
        if (button == 0 && selectedCategory == Category.Theme && themeNameEditing && (themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme)) {
            // recompute editor panel bounds
            float sidebarX0 = x + 8f;
            float sidebarY0 = y + HEADER_H + 10f + contentOffsetY;
            float contentX0 = sidebarX0 + SIDEBAR_W + 10f;
            float contentY0 = sidebarY0;
            float contentW0 = (x + width - 8f) - contentX0;
            float contentH0 = height - (contentY0 - y) - 10f;
            float settingsW0 = 190f;
            float settingsGap0 = 10f;
            boolean hasThemeEditor0 = true;
            float modulesW0 = hasThemeEditor0 ? (contentW0 - settingsW0 - settingsGap0) : contentW0;
            float panelX0 = contentX0 + modulesW0 + settingsGap0;
            float panelY0 = contentY0;
            float px0 = panelX0 + 10f;
            float py0 = panelY0 + 10f + 16f;
            float nameBoxY0 = py0 + 12f;
            float nameW0 = settingsW0 - 20f;
            float nameH0 = 18f;
            boolean inName0 = mouseX >= px0 && mouseX <= px0 + nameW0 && mouseY >= nameBoxY0 && mouseY <= nameBoxY0 + nameH0;
            if (!inName0) {
                applyThemeNameEdit();
            }
        }

        // ClickGUI black/white toggle (sun/moon)
        if (button == 0 && uiAlpha > 0.05f) {
            if (mouseX >= themeToggleX && mouseX <= themeToggleX + themeToggleS &&
                    mouseY >= themeToggleY && mouseY <= themeToggleY + themeToggleS) {
                guiLightMode = !guiLightMode;
                // Also switch global theme so HUD follows (White <-> Black)
                ThemeManager.Theme next = themeManager.findThemeByName(guiLightMode ? "White" : "Black");
                if (next != null) {
                    themeManager.setTheme(next);
                }
                playToggleSound(!guiLightMode);
                return true;
            }
        }

        // Sidebar category clicks (vertical)
        float sidebarX = x + 8f;
        float sidebarY = y + HEADER_H + 10f + contentOffsetY;
        float itemX = sidebarX + SIDEBAR_PAD;
        float itemY = sidebarY + SIDEBAR_PAD;
        float itemW = SIDEBAR_W - SIDEBAR_PAD * 2f;
        for (Category cat : TABS) {
            if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= itemY && mouseY <= itemY + SIDEBAR_ITEM_H) {
                selectedCategory = cat;
                scrollY = 0f;
                scrollYTarget = 0f;
                activeSettings = null;
                return true;
            }
            itemY += SIDEBAR_ITEM_H + SIDEBAR_GAP;
        }

        // Content bounds (right of sidebar)
        float contentX = sidebarX + SIDEBAR_W + 10f;
        float contentY = sidebarY;
        float contentW = (x + width - 8f) - contentX;
        float contentH = height - (contentY - y) - 10f;

        ModuleComponent target = activeSettings;
        boolean hasSettings = selectedCategory != Category.Theme && target != null && !target.getComponents().isEmpty();
        boolean hasThemeEditor = selectedCategory == Category.Theme && (themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme);
        float settingsW = 190f;
        float settingsGap = 10f;
        float modulesX = contentX;
        float modulesY = contentY;
        float modulesW = (hasSettings || hasThemeEditor) ? (contentW - settingsW - settingsGap) : contentW;
        float modulesH = contentH;

        if (mouseX >= modulesX && mouseX <= modulesX + modulesW && mouseY >= modulesY && mouseY <= modulesY + modulesH) {
            if (selectedCategory == Category.Theme && (button == 0 || button == 1)) {
                float themeY = modulesY + 2f - scrollY;
                float itemH = 28f;
                float itemGap = 6f;

                // Create custom theme (first card)
                if (button == 0 && mouseX >= modulesX && mouseX <= modulesX + modulesW && mouseY >= themeY && mouseY <= themeY + itemH) {
                    int idx = themeManager.getCustomThemes().size() + 1;
                    ThemeManager.Theme base = themeManager.getCurrentTheme();
                    ThemeManager.CustomTheme created = themeManager.createCustomTheme("Custom " + idx, base);
                    themeNameTarget = created;
                    themeNameBuffer = created.getName() == null ? "" : created.getName();
                    themeNameEditing = true;
                    try { NexusVisual.getInstance().getConfigManager().saveThemeStore(); } catch (Exception ignored) {}
                    playToggleSound(true);
                    return true;
                }
                themeY += itemH + itemGap;
                for (ThemeManager.Theme theme : themeManager.getAvailableThemes()) {
                    if (mouseX >= modulesX && mouseX <= modulesX + modulesW && mouseY >= themeY && mouseY <= themeY + itemH) {
                        if (button == 1 && theme instanceof ThemeManager.CustomTheme ct) {
                            if (themeNameTarget == ct) {
                                themeNameEditing = false;
                                themeNameTarget = null;
                                themeNameBuffer = "";
                            }
                            themeManager.removeCustomTheme(ct);
                            try { NexusVisual.getInstance().getConfigManager().saveThemeStore(); } catch (Exception ignored) {}
                            playToggleSound(true);
                            return true;
                        }
                        if (button == 0) {
                            // apply theme
                            themeNameEditing = false;
                            ThemeManager.Theme previousTheme = themeManager.getCurrentTheme();
                            themeManager.setTheme(theme);
                            guiLightMode = isLightTheme(theme);
                            if (previousTheme != theme) { playToggleSound(true); }
                            return true;
                        }
                    }
                    themeY += itemH + itemGap;
                }
            } else {
                List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
                // Если какое-либо меню бинда открыто — сделать модальным: обработать клик только этим компонентом
                for (ModuleComponent mcComp : comps) {
                    if (mcComp.isBindModeMenuOpen()) {
                        mcComp.mouseClicked(mouseX, mouseY, button);
                        return true;
                    }
                }
                for (ModuleComponent mcComp : comps) {
                    boolean wasToggled = mcComp.getModule().isToggled();
                    float headerH = 24f;
                    if (button == 1 && mouseX >= mcComp.getX() && mouseX <= mcComp.getX() + mcComp.getWidth() &&
                            mouseY >= mcComp.getY() && mouseY <= mcComp.getY() + headerH) {
                        // Toggle settings panel on repeated right-click
                        if (activeSettings == mcComp) {
                            activeSettings = null;
                        } else {
                            activeSettings = mcComp.getComponents().isEmpty() ? null : mcComp;
                            settingsScrollY = 0f;
                            settingsScrollYTarget = 0f;
                        }
                        return true;
                    }
                    mcComp.mouseClicked(mouseX, mouseY, button);
                    if (button == 0 && wasToggled != mcComp.getModule().isToggled()) {
                        playToggleSound(wasToggled);
                    }
                }
            }
        }

        // Settings panel interactions (inside ClickGUI)
        if (hasSettings) {
            float anim = settingsAnimation.getValue();
            if (anim > 0.2f) {
                float basePanelX = modulesX + modulesW + settingsGap;
                float panelY = modulesY;
                float panelW = settingsW;
                float panelH = modulesH;
                float slideOffset = (1f - anim) * 24f;
                float drawPanelX = basePanelX + slideOffset;
                float rContentX = drawPanelX + 8f;
                float rContentY = panelY + 8f;
                float rContentW = panelW - 16f;
                float rContentH = panelH - 16f;
                if (mouseX >= rContentX && mouseX <= rContentX + rContentW && mouseY >= rContentY && mouseY <= rContentY + rContentH) {
                    activeSettings.mouseClickedExternal(mouseX, mouseY, button);
                    return true;
                }
            }
        }

        // Theme editor interactions (inside ClickGUI)
        if (selectedCategory == Category.Theme && (themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme ct)) {
            float panelX = modulesX + modulesW + settingsGap;
            float panelY = modulesY;
            float panelW = settingsW;
            float panelH = modulesH;
            if (mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH) {
                // Delete button
                float delW = 54f;
                float delH = 16f;
                float delX = panelX + panelW - 10f - delW;
                float delY = panelY + 10f;
                if (button == 0 && mouseX >= delX && mouseX <= delX + delW && mouseY >= delY && mouseY <= delY + delH) {
                    if (themeNameTarget == ct) {
                        themeNameEditing = false;
                        themeNameTarget = null;
                        themeNameBuffer = "";
                    }
                    themeManager.removeCustomTheme(ct);
                    try { NexusVisual.getInstance().getConfigManager().saveThemeStore(); } catch (Exception ignored) {}
                    playToggleSound(true);
                    return true;
                }

                float px = panelX + 10f;
                float py = panelY + 10f + 16f;

                // Name box (label 12 + box 18)
                float nameLabelY = py;
                float nameBoxY = nameLabelY + 12f;
                float nameW = panelW - 20f;
                float nameH = 18f;
                boolean inName = mouseX >= px && mouseX <= px + nameW && mouseY >= nameBoxY && mouseY <= nameBoxY + nameH;
                if (button == 0 && inName) {
                    themeNameTarget = ct;
                    themeNameBuffer = ct.getName() == null ? "" : ct.getName();
                    themeNameEditing = true;
                    return true;
                } else if (button == 0 && themeNameEditing) {
                    applyThemeNameEdit();
                }

                // Target button (single square): click to cycle BG / SEC / ACC
                py = nameBoxY + nameH + 10f;
                float btnS = 18f;
                float bx = px + (panelW - 20f - btnS) / 2f;
                if (mouseX >= bx && mouseX <= bx + btnS && mouseY >= py && mouseY <= py + btnS) {
                    themeColorTarget = (themeColorTarget + 1) % 3;
                    return true;
                }
                py += btnS + 10f;

                // Wheel + alpha slider: left mouse only
                if (button != 0) return true;

                // Wheel
                float reserveAfterWheel = 34f; // space for alpha slider
                float wheelS = Math.min(panelW - 20f, panelH - (py - panelY) - 10f - reserveAfterWheel);
                wheelS = Math.min(wheelS, 150f);
                float wheelX = px + (panelW - 20f - wheelS) / 2f;
                float wheelY = py;
                float cx = wheelX + wheelS / 2f;
                float cy = wheelY + wheelS / 2f;
                float dx = (float) (mouseX - cx);
                float dy = (float) (mouseY - cy);
                float r = wheelS / 2f;
                if ((dx * dx + dy * dy) <= (r * r)) {
                    wheelDragging = true;
                    applyWheelAt(ct, mouseX, mouseY, wheelX, wheelY, wheelS);
                    return true;
                }

                // Alpha slider
                float alphaBlockY = wheelY + wheelS + 10f;
                float trackX = px;
                float trackY = alphaBlockY + 10f;
                float trackW = panelW - 20f;
                float trackH = 8f;
                if (mouseX >= trackX && mouseX <= trackX + trackW && mouseY >= trackY - 2f && mouseY <= trackY + trackH + 2f) {
                    themeEditorDragging = true;
                    themeEditorDragKey = (themeColorTarget == 1) ? 103 : (themeColorTarget == 2 ? 203 : 3);
                    updateThemeEditorDrag(mouseX, trackX, trackW);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double[] um = unscaleMouse(mouseX, mouseY);
        mouseX = um[0];
        mouseY = um[1];
        if (lastUiScale != 0f && lastUiScale != 1.0f) {
            deltaX /= lastUiScale;
            deltaY /= lastUiScale;
        }
        if (button == 0 && themeEditorDragging && selectedCategory == Category.Theme && (themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme)) {
            float sidebarX = x + 8f;
            float sidebarY = y + HEADER_H + 10f + contentOffsetY;
            float contentX = sidebarX + SIDEBAR_W + 10f;
            float contentY = sidebarY;
            float contentW = (x + width - 8f) - contentX;
            float contentH = height - (contentY - y) - 10f;

            float settingsW = 190f;
            float settingsGap = 10f;
            float modulesW = contentW - settingsW - settingsGap;

            float panelX = contentX + modulesW + settingsGap;
            float panelY = contentY;
            float panelW = settingsW;
            float panelH = contentH;

            float px = panelX + 10f;
            float py = panelY + 10f + 16f;
            float nameBoxY = py + 12f;
            float nameH = 18f;
            py = nameBoxY + nameH + 10f;
            float btnH = 18f;
            py += btnH + 10f;

            float reserveAfterWheel = 34f;
            float wheelS = Math.min(panelW - 20f, panelH - (py - panelY) - 10f - reserveAfterWheel);
            wheelS = Math.min(wheelS, 150f);
            float wheelY = py;

            float alphaBlockY = wheelY + wheelS + 10f;
            float trackX = px;
            float trackW = panelW - 20f;
            updateThemeEditorDrag(mouseX, trackX, trackW);
            return true;
        }
        if (button == 0 && wheelDragging && selectedCategory == Category.Theme && (themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme ct)) {
            float sidebarX = x + 8f;
            float sidebarY = y + HEADER_H + 10f + contentOffsetY;
            float contentX = sidebarX + SIDEBAR_W + 10f;
            float contentY = sidebarY;
            float contentW = (x + width - 8f) - contentX;
            float contentH = height - (contentY - y) - 10f;

            float settingsW = 190f;
            float settingsGap = 10f;
            float modulesW = contentW - settingsW - settingsGap;

            float panelX = contentX + modulesW + settingsGap;
            float panelY = contentY;
            float panelW = settingsW;
            float panelH = contentH;

            float px = panelX + 10f;
            float py = panelY + 10f + 16f;
            float nameBoxY = py + 12f;
            float nameH = 18f;
            py = nameBoxY + nameH + 10f;
            float btnH = 18f;
            py += btnH + 10f;

            float reserveAfterWheel = 34f;
            float wheelS = Math.min(panelW - 20f, panelH - (py - panelY) - 10f - reserveAfterWheel);
            wheelS = Math.min(wheelS, 150f);
            float wheelX = px + (panelW - 20f - wheelS) / 2f;
            float wheelY = py;
            applyWheelAt(ct, mouseX, mouseY, wheelX, wheelY, wheelS);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private int hitTestThemeEditorSlider(double mouseX, double mouseY, float px, float pyStart, float w) {
        // Layout mirrors render: header (16), then 3 groups:
        // group: label (12) + 3 sliders (22 each) + gap (8)
        float y = pyStart;
        // Background
        y += 12f;
        for (int i = 0; i < 3; i++) {
            float trackY = y + 10f;
            if (mouseX >= px && mouseX <= px + w && mouseY >= trackY - 2f && mouseY <= trackY + 10f) return 0 + i;
            y += 22f;
        }
        y += 8f;
        // Secondary
        y += 12f;
        for (int i = 0; i < 3; i++) {
            float trackY = y + 10f;
            if (mouseX >= px && mouseX <= px + w && mouseY >= trackY - 2f && mouseY <= trackY + 10f) return 100 + i;
            y += 22f;
        }
        y += 8f;
        // Accent
        y += 12f;
        for (int i = 0; i < 3; i++) {
            float trackY = y + 10f;
            if (mouseX >= px && mouseX <= px + w && mouseY >= trackY - 2f && mouseY <= trackY + 10f) return 200 + i;
            y += 22f;
        }
        return -1;
    }

    private void applyThemeNameEdit() {
        if (!(themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme ct)) {
            themeNameEditing = false;
            return;
        }
        String name = themeNameBuffer == null ? "" : themeNameBuffer;
        themeNameEditing = false;
        if (name.trim().isEmpty()) return;

        ct.setName(name);
        themeNameTarget = ct;
        themeNameBuffer = name;
        themeManager.setTheme(ct);
        try { NexusVisual.getInstance().getConfigManager().saveThemeStore(); } catch (Exception ignored) {}
    }

    private boolean isAllowedThemeNameChar(char c) {
        if (c >= 'a' && c <= 'z') return true;
        if (c >= 'A' && c <= 'Z') return true;
        if (c >= '0' && c <= '9') return true;
        return c == ' ' || c == '_' || c == '-';
    }

    private void updateThemeEditorDrag(double mouseX, float trackX, float trackW) {
        float t = (float) ((mouseX - trackX) / trackW);
        int v = (int) (clamp(t, 0f, 1f) * 255f);
        applyThemeEditorValue(themeEditorDragKey, v);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double[] um = unscaleMouse(mouseX, mouseY);
        mouseX = um[0];
        mouseY = um[1];
        themeEditorDragging = false;
        themeEditorDragKey = -1;
        wheelDragging = false;
        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                if (mcComp.isBindModeMenuOpen()) {
                    mcComp.mouseReleased(mouseX, mouseY, button);
                    return true;
                }
            }
        }
        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                mcComp.mouseReleased(mouseX, mouseY, button);
            }
        }
        if (activeSettings != null) {
            activeSettings.mouseReleasedExternal(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double[] um = unscaleMouse(mouseX, mouseY);
        mouseX = um[0];
        mouseY = um[1];
        float sidebarX = x + 8f;
        float sidebarY = y + HEADER_H + 10f + contentOffsetY;
        float contentX = sidebarX + SIDEBAR_W + 10f;
        float contentY = sidebarY;
        float contentW = (x + width - 8f) - contentX;
        float contentH = height - (contentY - y) - 10f;

        ModuleComponent target = activeSettings;
        boolean hasSettings = selectedCategory != Category.Theme && target != null && !target.getComponents().isEmpty();
        boolean hasThemeEditor = selectedCategory == Category.Theme && (themeManager.getCurrentTheme() instanceof ThemeManager.CustomTheme);
        float settingsW = 190f;
        float settingsGap = 10f;
        float modulesX = contentX;
        float modulesY = contentY;
        float modulesW = (hasSettings || hasThemeEditor) ? (contentW - settingsW - settingsGap) : contentW;
        float modulesH = contentH;

        float step = (float) (-vertical * 16f);
        if (mouseX >= modulesX && mouseX <= modulesX + modulesW && mouseY >= modulesY && mouseY <= modulesY + modulesH) {
            scrollYTarget = clamp(scrollYTarget + step, 0f, maxScroll);
            return true;
        }

        if (hasSettings) {
            float anim = settingsAnimation.getValue();
            if (anim > 0.2f) {
                float basePanelX = modulesX + modulesW + settingsGap;
                float panelY = modulesY;
                float panelW = settingsW;
                float panelH = modulesH;
                float slideOffset = (1f - anim) * 24f;
                float drawPanelX = basePanelX + slideOffset;
                float rContentX = drawPanelX + 8f;
                float rContentY = panelY + 8f;
                float rContentW = panelW - 16f;
                float rContentH = panelH - 16f;
                if (mouseX >= rContentX && mouseX <= rContentX + rContentW && mouseY >= rContentY && mouseY <= rContentY + rContentH) {
                    settingsScrollYTarget = clamp(settingsScrollYTarget + step, 0f, settingsMaxScroll);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (selectedCategory == Category.Theme && themeNameEditing) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyThemeNameEdit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (themeNameBuffer != null && !themeNameBuffer.isEmpty()) {
                    themeNameBuffer = themeNameBuffer.substring(0, themeNameBuffer.length() - 1);
                }
                return true;
            }
        }

        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                mcComp.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        if (activeSettings != null) {
            activeSettings.keyPressedExternal(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing) return false;

        if (selectedCategory == Category.Theme && themeNameEditing) {
            if (isAllowedThemeNameChar(chr)) {
                if (themeNameBuffer == null) themeNameBuffer = "";
                if (themeNameBuffer.length() < 24) {
                    themeNameBuffer += chr;
                }
                return true;
            }
        }

        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                mcComp.charTyped(chr, modifiers);
            }
        }
        if (activeSettings != null) {
            activeSettings.charTypedExternal(chr, modifiers);
        }

        return super.charTyped(chr, modifiers);
    }

    public void setDescription(String text) {
        this.description = text == null ? "" : text;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}