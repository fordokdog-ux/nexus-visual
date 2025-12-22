package dev.simplevisuals.client.managers;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.modules.impl.render.UI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class    ThemeManager {
    private static ThemeManager instance;
    private Theme currentTheme;
    private final List<ThemeChangeListener> listeners = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ThemeManager-Gradient");
        t.setDaemon(true);
        return t;
    });

    private final Theme[] builtInThemes;
    private final List<CustomTheme> customThemes = new ArrayList<>();

    // Private constructor for singleton
    private ThemeManager() {
        // Cache theme instances once (important for UI + performance)
        this.builtInThemes = new Theme[]{
            new LightTheme(),
            new DarkTheme(),
            new TwilightGradientTheme(),
            new EmeraldGlowTheme(),
            new AmethystFadeTheme(),
            new SunsetBlazeTheme(),
            new OceanBreezeTheme(),
            new AuroraBorealisTheme(),
            new CyberNeonTheme(),
            new LavaCoreTheme(),
            new RGBTheme()
        };
        // Default theme
        this.currentTheme = this.builtInThemes[0];
        startGradientUpdateTask();
    }

    // Get singleton instance
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        notifyListeners();
    }

    public void addThemeChangeListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    public void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(currentTheme);
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    private UI getUiModuleSafe() {
        try {
            NexusVisual nv = NexusVisual.getInstance();
            if (nv == null) return null;
            if (nv.getModuleManager() == null) return null;
            return nv.getModuleManager().getModule(UI.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isOverrideEnabled() {
        UI ui = getUiModuleSafe();
        return ui != null && Boolean.TRUE.equals(ui.overrideThemeColors.getValue());
    }

    public Color getBackgroundColor() {
        if (isOverrideEnabled()) {
            UI ui = getUiModuleSafe();
            if (ui != null) return ui.overrideBackgroundColor.getColor();
        }
        return currentTheme.getBackgroundColor();
    }

    public Color getSecondaryBackgroundColor() {
        if (isOverrideEnabled()) {
            UI ui = getUiModuleSafe();
            if (ui != null) return ui.overrideSecondaryBackgroundColor.getColor();
        }
        return currentTheme.getSecondaryBackgroundColor();
    }

    public Color getAccentColor() {
        if (isOverrideEnabled()) {
            UI ui = getUiModuleSafe();
            if (ui != null) return ui.overrideAccentColor.getColor();
        }
        return currentTheme.getAccentColor();
    }

    public Color getTextColor() {
        if (isOverrideEnabled()) {
            UI ui = getUiModuleSafe();
            if (ui != null) return ui.overrideTextColor.getColor();
        }
        return currentTheme.getTextColor();
    }

    public Color getBorderColor() {
        // If accent is overridden, make border consistent with accent.
        if (isOverrideEnabled()) {
            Color a = getAccentColor();
            return new Color(a.getRed(), a.getGreen(), a.getBlue(), 120);
        }
        return currentTheme.getBorderColor();
    }

    public Color getThemeColor() {
        return getBackgroundColor();
    }

    public Theme[] getAvailableThemes() {
        Theme[] all = new Theme[builtInThemes.length + customThemes.size()];
        System.arraycopy(builtInThemes, 0, all, 0, builtInThemes.length);
        for (int i = 0; i < customThemes.size(); i++) {
            all[builtInThemes.length + i] = customThemes.get(i);
        }
        return all;
    }

    public List<CustomTheme> getCustomThemes() {
        return Collections.unmodifiableList(customThemes);
    }

    public void replaceCustomThemes(List<CustomTheme> themes) {
        customThemes.clear();
        if (themes != null) customThemes.addAll(themes);
        // If current theme was deleted, fall back to first built-in
        if (currentTheme instanceof CustomTheme && !customThemes.contains(currentTheme)) {
            currentTheme = builtInThemes[0];
            notifyListeners();
        }
    }

    public Theme findThemeByName(String name) {
        if (name == null) return null;
        for (Theme t : getAvailableThemes()) {
            if (name.equals(t.getName())) return t;
        }
        return null;
    }

    public CustomTheme createCustomTheme(String name, Theme base) {
        String finalName = (name == null || name.isBlank()) ? "Custom" : name;
        Theme src = base != null ? base : currentTheme;
        CustomTheme ct = new CustomTheme(finalName,
                src.getBackgroundColor(),
                src.getSecondaryBackgroundColor(),
                src.getAccentColor());
        customThemes.add(ct);
        setTheme(ct);
        return ct;
    }

    public void removeCustomTheme(CustomTheme theme) {
        if (theme == null) return;
        boolean removed = customThemes.remove(theme);
        if (removed && currentTheme == theme) {
            currentTheme = builtInThemes[0];
            notifyListeners();
        }
    }

    private void startGradientUpdateTask() {
        scheduler.scheduleAtFixedRate(() -> {
            if (currentTheme instanceof GradientTheme) {
                notifyListeners();
            }
        }, 0, 25, TimeUnit.MILLISECONDS); // Update every 50ms for gradient themes
    }

    public interface Theme {
        Color getBackgroundColor();
        Color getBorderColor();
        Color getTextColor();
        Color getAccentColor();
        Color getSecondaryBackgroundColor();
        String getName();
    }

    public static class CustomTheme implements Theme {
        private String name;
        private Color background;
        private Color secondary;
        private Color accent;

        public CustomTheme(String name, Color background, Color secondary, Color accent) {
            this.name = name;
            this.background = background;
            this.secondary = secondary;
            this.accent = accent;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setBackground(Color background) {
            this.background = background;
        }

        public void setSecondary(Color secondary) {
            this.secondary = secondary;
        }

        public void setAccent(Color accent) {
            this.accent = accent;
        }

        @Override
        public Color getBackgroundColor() {
            return background;
        }

        @Override
        public Color getBorderColor() {
            Color a = getAccentColor();
            return new Color(a.getRed(), a.getGreen(), a.getBlue(), 120);
        }

        @Override
        public Color getTextColor() {
            // Simple contrast heuristic
            Color bg = getBackgroundColor();
            double l = (0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue()) / 255.0;
            return l > 0.55 ? new Color(30, 30, 30) : new Color(235, 235, 235);
        }

        @Override
        public Color getAccentColor() {
            return accent;
        }

        @Override
        public Color getSecondaryBackgroundColor() {
            return secondary;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public interface ThemeChangeListener {
        void onThemeChanged(Theme theme);
    }

    private static Color interpolateColor(Color start, Color end, float t) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)),
                Math.max(0, Math.min(255, a))
        );
    }

    private static abstract class GradientTheme implements Theme {
        protected final Color[] backgroundColors;
        protected final Color[] borderColors;
        protected final Color[] accentColors;
        protected final Color[] secondaryBackgroundColors;
        private static final long TRANSITION_DURATION = 1000; // Increased to 4000ms for slower transitions

        protected GradientTheme(Color[] backgroundColors, Color[] borderColors,
                                Color[] accentColors, Color[] secondaryBackgroundColors) {
            this.backgroundColors = backgroundColors;
            this.borderColors = borderColors;
            this.accentColors = accentColors;
            this.secondaryBackgroundColors = secondaryBackgroundColors;
        }

        protected float getInterpolationFactor() {
            long currentTime = System.currentTimeMillis();
            float phase = (float) (currentTime % (TRANSITION_DURATION * backgroundColors.length)) / TRANSITION_DURATION;
            int index = (int) phase;
            float t = phase - index;
            t = (float) (Math.sin(t * Math.PI / 2)); // Ease-in effect
            return t;
        }

        protected int getCurrentIndex() {
            long currentTime = System.currentTimeMillis();
            return (int) ((currentTime % (TRANSITION_DURATION * backgroundColors.length)) / TRANSITION_DURATION);
        }

        @Override
        public Color getBackgroundColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % backgroundColors.length;
            return interpolateColor(backgroundColors[currentIndex], backgroundColors[nextIndex], getInterpolationFactor());
        }

        @Override
        public Color getBorderColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % borderColors.length;
            return interpolateColor(borderColors[currentIndex], borderColors[nextIndex], getInterpolationFactor());
        }

        @Override
        public Color getAccentColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % accentColors.length;
            return interpolateColor(accentColors[currentIndex], accentColors[nextIndex], getInterpolationFactor());
        }

        @Override
        public Color getSecondaryBackgroundColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % secondaryBackgroundColors.length;
            return interpolateColor(secondaryBackgroundColors[currentIndex], secondaryBackgroundColors[nextIndex], getInterpolationFactor());
        }
    }

    public static class LightTheme implements Theme {
        @Override
        public Color getBackgroundColor() {
            // Soft translucent white surface
            return new Color(255, 255, 255, 130);
        }

        @Override
        public Color getBorderColor() {
            // Subtle dark border for definition on bright UI
            return new Color(0, 0, 0, 40);
        }

        @Override
        public Color getTextColor() {
            // White theme needs dark text for readability
            return new Color(25, 25, 25);
        }

        @Override
        public Color getAccentColor() {
            // Cool accent that stays readable on white
            return new Color(45, 120, 255, 210);
        }

        @Override
        public Color getSecondaryBackgroundColor() {
            // Slightly darker card surface
            return new Color(242, 244, 248, 190);
        }

        @Override
        public String getName() {
            return "White";
        }
    }

    public static class DarkTheme implements Theme {
        @Override
        public Color getBackgroundColor() {
            // Deep dark surface
            return new Color(18, 18, 18, 195);
        }

        @Override
        public Color getBorderColor() {
            // Subtle light border
            return new Color(255, 255, 255, 32);
        }

        @Override
        public Color getTextColor() {
            return new Color(235, 235, 235);
        }

        @Override
        public Color getAccentColor() {
            // Same family as light accent, tuned for dark
            return new Color(70, 145, 255, 220);
        }

        @Override
        public Color getSecondaryBackgroundColor() {
            return new Color(28, 28, 28, 215);
        }

        @Override
        public String getName() {
            return "Black";
        }
    }

    public static class TwilightGradientTheme extends GradientTheme {
        public TwilightGradientTheme() {
            super(
                    new Color[]{
                            new Color(80, 42, 195, 150),   // Deep navy
                            new Color(156, 69, 211, 150),  // Purple dusk
                            new Color(179, 103, 250, 150), // Light violet
                            new Color(148, 36, 255, 150)    // Dark purple
                    },
                    new Color[]{
                            new Color(80, 42, 195, 150),   // Deep navy
                            new Color(156, 69, 211, 150),  // Purple dusk
                            new Color(179, 103, 250, 150), // Light violet
                            new Color(148, 36, 255, 150)    // Deeper purple
                    },
                    new Color[]{
                            new Color(80, 42, 195, 150),   // Deep navy
                            new Color(156, 69, 211, 150),  // Purple dusk
                            new Color(179, 103, 250, 150), // Light violet
                            new Color(148, 36, 255, 150)     // Deepest purple
                    },
                    new Color[]{
                            new Color(80, 42, 195, 150),   // Deep navy
                            new Color(156, 69, 211, 150),  // Purple dusk
                            new Color(179, 103, 250, 150), // Light violet
                            new Color(148, 36, 255, 150)    // Lighter purple
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Purple";
        }
    }

    public static class EmeraldGlowTheme extends GradientTheme {
        public EmeraldGlowTheme() {
            super(
                    new Color[]{
                            new Color(0, 204, 160, 150),    // Deep emerald
                            new Color(2, 204, 73, 150),   // Bright emerald
                            new Color(15, 227, 0, 150),  // Light green
                            new Color(0, 170, 31, 150)     // Forest emerald
                    },
                    new Color[]{
                            new Color(0, 204, 160, 150),    // Deep emerald
                            new Color(2, 204, 73, 150),   // Bright emerald
                            new Color(15, 227, 0, 150),  // Light green
                            new Color(0, 170, 31, 150)     // Darker forest emerald
                    },
                    new Color[]{
                            new Color(0, 204, 160, 150),    // Deep emerald
                            new Color(2, 204, 73, 150),   // Bright emerald
                            new Color(15, 227, 0, 150),  // Light green
                            new Color(0, 170, 31, 150)      // Deepest forest emerald
                    },
                    new Color[]{
                            new Color(0, 204, 160, 150),    // Deep emerald
                            new Color(2, 204, 73, 150),   // Bright emerald
                            new Color(15, 227, 0, 150),  // Light green
                            new Color(0, 170, 31, 150)   // Lighter forest emerald
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Emerald";
        }
    }

    public static class AmethystFadeTheme extends GradientTheme {
        public AmethystFadeTheme() {
            super(
                    new Color[]{
                            new Color(100, 50, 150, 150),  // Deep amethyst
                            new Color(150, 80, 200, 150),  // Bright amethyst
                            new Color(180, 120, 220, 150), // Light amethyst
                            new Color(80, 40, 120, 150)    // Dark amethyst
                    },
                    new Color[]{
                            new Color(80, 40, 120, 150),   // Darker amethyst
                            new Color(120, 64, 160, 150),  // Darker bright amethyst
                            new Color(144, 96, 176, 150),  // Darker light amethyst
                            new Color(64, 32, 96, 150)     // Darker dark amethyst
                    },
                    new Color[]{
                            new Color(60, 30, 90, 150),    // Deepest amethyst
                            new Color(90, 48, 120, 150),   // Deepest bright amethyst
                            new Color(108, 72, 132, 150),  // Deepest light amethyst
                            new Color(48, 24, 72, 150)     // Deepest dark amethyst
                    },
                    new Color[]{
                            new Color(120, 70, 170, 150),  // Lighter amethyst
                            new Color(170, 100, 220, 150), // Lighter bright amethyst
                            new Color(200, 140, 240, 150), // Lighter light amethyst
                            new Color(100, 60, 140, 150)   // Lighter dark amethyst
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Amethyst";
        }
    }

    public static class SunsetBlazeTheme extends GradientTheme {
        public SunsetBlazeTheme() {
            super(
                    new Color[]{
                            new Color(200, 60, 20, 150),   // Deep sunset red
                            new Color(220, 100, 40, 150),  // Bright orange
                            new Color(240, 140, 60, 150),  // Light orange
                            new Color(180, 40, 10, 150)    // Dark red-orange
                    },
                    new Color[]{
                            new Color(160, 48, 16, 150),   // Darker sunset red
                            new Color(176, 80, 32, 150),   // Darker bright orange
                            new Color(192, 112, 48, 150),  // Darker light orange
                            new Color(144, 32, 8, 150)     // Darker dark red-orange
                    },
                    new Color[]{
                            new Color(120, 36, 12, 150),   // Deepest sunset red
                            new Color(132, 60, 24, 150),   // Deepest bright orange
                            new Color(144, 84, 36, 150),   // Deepest light orange
                            new Color(108, 24, 6, 150)     // Deepest dark red-orange
                    },
                    new Color[]{
                            new Color(220, 80, 40, 150),   // Lighter sunset red
                            new Color(240, 120, 60, 150),  // Lighter bright orange
                            new Color(255, 160, 80, 150),  // Lighter light orange
                            new Color(200, 60, 20, 150)    // Lighter dark red-orange
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Orange";
        }
    }

    public static class OceanBreezeTheme extends GradientTheme {
        public OceanBreezeTheme() {
            super(
                    new Color[]{
                            new Color(20, 80, 120, 150),   // Deep ocean blue
                            new Color(40, 120, 160, 150),  // Bright teal
                            new Color(60, 160, 200, 150),  // Light aqua
                            new Color(10, 60, 100, 150)    // Dark ocean blue
                    },
                    new Color[]{
                            new Color(16, 64, 96, 150),    // Darker ocean blue
                            new Color(32, 96, 128, 150),   // Darker bright teal
                            new Color(48, 128, 160, 150),  // Darker light aqua
                            new Color(8, 48, 80, 150)      // Darker dark ocean blue
                    },
                    new Color[]{
                            new Color(12, 48, 72, 150),    // Deepest ocean blue
                            new Color(24, 72, 96, 150),    // Deepest bright teal
                            new Color(36, 96, 120, 150),   // Deepest light aqua
                            new Color(6, 36, 60, 150)      // Deepest dark ocean blue
                    },
                    new Color[]{
                            new Color(40, 100, 140, 150),  // Lighter ocean blue
                            new Color(60, 140, 180, 150),  // Lighter bright teal
                            new Color(80, 180, 220, 150),  // Lighter light aqua
                            new Color(30, 80, 120, 150)    // Lighter dark ocean blue
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Blue";
        }
    }
    public static class AuroraBorealisTheme extends GradientTheme {
        public AuroraBorealisTheme() {
            super(
                    new Color[]{
                            new Color(10, 80, 60, 150),   // Deep teal
                            new Color(40, 180, 120, 150), // Emerald green
                            new Color(80, 220, 200, 150), // Aqua glow
                            new Color(120, 100, 220, 150) // Violet edge
                    },
                    new Color[]{
                            new Color(8, 64, 48, 150),    // Darker teal
                            new Color(32, 144, 96, 150),  // Darker emerald
                            new Color(64, 176, 160, 150), // Darker aqua
                            new Color(96, 80, 176, 150)   // Darker violet
                    },
                    new Color[]{
                            new Color(20, 120, 90, 150),  // Highlight teal
                            new Color(60, 200, 140, 150), // Highlight emerald
                            new Color(100, 240, 220, 150),// Highlight aqua
                            new Color(150, 130, 255, 150) // Highlight violet
                    },
                    new Color[]{
                            new Color(16, 100, 80, 150),  // Soft teal
                            new Color(50, 170, 130, 150), // Soft emerald
                            new Color(90, 210, 190, 150), // Soft aqua
                            new Color(130, 115, 240, 150) // Soft violet
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Aurora";
        }
    }

    public static class CyberNeonTheme extends GradientTheme {
        public CyberNeonTheme() {
            super(
                    new Color[]{
                            new Color(0, 255, 200, 150),   // Neon cyan
                            new Color(255, 0, 180, 150),   // Neon magenta
                            new Color(0, 140, 255, 150),   // Electric blue
                            new Color(255, 255, 0, 150)    // Neon yellow
                    },
                    new Color[]{
                            new Color(0, 210, 165, 150),   // Darker neon cyan
                            new Color(210, 0, 150, 150),   // Darker neon magenta
                            new Color(0, 110, 210, 150),   // Darker electric blue
                            new Color(210, 210, 0, 150)    // Darker neon yellow
                    },
                    new Color[]{
                            new Color(50, 255, 220, 150),  // Accent cyan
                            new Color(255, 50, 200, 150),  // Accent magenta
                            new Color(50, 170, 255, 150),  // Accent blue
                            new Color(255, 255, 50, 150)   // Accent yellow
                    },
                    new Color[]{
                            new Color(0, 235, 190, 150),   // Secondary cyan
                            new Color(235, 0, 170, 150),   // Secondary magenta
                            new Color(0, 130, 235, 150),   // Secondary blue
                            new Color(235, 235, 0, 150)    // Secondary yellow
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Neon";
        }
    }

    public static class LavaCoreTheme extends GradientTheme {
        public LavaCoreTheme() {
            super(
                    new Color[]{
                            new Color(120, 0, 0, 150),     // Dark lava red
                            new Color(200, 20, 0, 150),    // Hot red
                            new Color(255, 90, 0, 150),    // Molten orange
                            new Color(255, 180, 0, 150)    // Bright lava yellow
                    },
                    new Color[]{
                            new Color(90, 0, 0, 150),      // Darker red
                            new Color(160, 16, 0, 150),    // Darker hot red
                            new Color(210, 75, 0, 150),    // Darker orange
                            new Color(220, 150, 0, 150)    // Darker yellow
                    },
                    new Color[]{
                            new Color(160, 10, 0, 150),    // Accent red
                            new Color(230, 40, 0, 150),    // Accent hot red
                            new Color(255, 120, 0, 150),   // Accent orange
                            new Color(255, 210, 0, 150)    // Accent yellow
                    },
                    new Color[]{
                            new Color(140, 5, 0, 150),     // Secondary red
                            new Color(210, 30, 0, 150),    // Secondary hot red
                            new Color(240, 100, 0, 150),   // Secondary orange
                            new Color(245, 190, 0, 150)    // Secondary yellow
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Lava";
        }
    }
    public static class RGBTheme extends GradientTheme {
        public RGBTheme() {
            super(
                    new Color[]{
                            new Color(255, 0, 0, 150),   // Deep ocean blue
                            new Color(17, 255, 0, 150),  // Bright teal
                            new Color(0, 5, 255, 150),  // Light aqua
                            new Color(255, 0, 241, 150)    // Dark ocean blue
                    },
                    new Color[]{
                            new Color(255, 0, 0, 150),   // Deep ocean blue
                            new Color(17, 255, 0, 150),  // Bright teal
                            new Color(0, 5, 255, 150),  // Light aqua
                            new Color(255, 0, 241, 150)      // Darker dark ocean blue
                    },
                    new Color[]{
                            new Color(255, 0, 0, 150),   // Deep ocean blue
                            new Color(17, 255, 0, 150),  // Bright teal
                            new Color(0, 5, 255, 150),  // Light aqua
                            new Color(255, 0, 241, 150)      // Deepest dark ocean blue
                    },
                    new Color[]{
                            new Color(255, 0, 0, 150),   // Deep ocean blue
                            new Color(17, 255, 0, 150),  // Bright teal
                            new Color(0, 5, 255, 150),  // Light aqua
                            new Color(255, 0, 241, 150)    // Lighter dark ocean blue
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "RGB";
        }
    }
}