package dev.simplevisuals.client.ui.colorgui;

import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ColorPickerScreen extends Screen {
    private final ColorSetting setting;
    private final Screen parent;
    private float hue, saturation, brightness;

    private TextFieldWidget hexField;
    private boolean updatingHexFromPicker = false;

    // состояние перетаскивания
    private boolean draggingWheel = false;
    private boolean draggingBrightness = false;

    // размеры
    private final int squareSize = 150;
    private final int hueWidth = 15;

    private static final int GAP = 12;
    private static final int PAD = 18;
    private static final int ROUND = 6;

    private static final int TITLE_GAP = 10;
    private static final int BELOW_SQUARE_GAP = 12;
    private static final int BETWEEN_PREVIEW_CLOSE_GAP = 10;

    // Color wheel texture (cached)
    private static NativeImageBackedTexture colorWheelTexture = null;
    private static boolean colorWheelReady = false;

    public ColorPickerScreen(ColorSetting setting) {
        this(setting, null);
    }

    public ColorPickerScreen(ColorSetting setting, Screen parent) {
        super(Text.of("Color Picker"));
        this.setting = setting;
        this.parent = parent;

        Color color = new Color(setting.getValue(), true);
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    @Override
    protected void init() {
        super.init();
        if (this.client == null) return;

        if (hexField == null) {
            hexField = new TextFieldWidget(this.textRenderer, 0, 0, 120, 16, Text.of("#RRGGBB"));
            hexField.setMaxLength(9); // # + 8
            hexField.setEditableColor(0xFFFFFF);
            try {
                hexField.setDrawsBackground(false);
            } catch (Throwable ignored) {
            }
            hexField.setText(toHexString(setting.getValue()));
            hexField.setChangedListener(this::onHexChanged);
            this.addDrawableChild(hexField);
        }
    }

    private void onHexChanged(String raw) {
        if (updatingHexFromPicker) return;
        if (hexField == null || !hexField.isFocused()) return;

        Integer parsed = tryParseHexToArgb(raw, (setting.getValue() >>> 24) & 0xFF);
        if (parsed == null) return;
        applyArgb(parsed);
    }

    private void applyArgb(int argb) {
        setting.set(argb);
        Color c = new Color(argb, true);
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    private static String toHexString(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = (argb) & 0xFF;
        if (a == 0xFF) {
            return String.format("#%02X%02X%02X", r, g, b);
        }
        return String.format("#%02X%02X%02X%02X", a, r, g, b);
    }

    private static Integer tryParseHexToArgb(String input, int currentAlpha) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("#")) s = s.substring(1);

        // keep only hex chars
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (hex) sb.append(c);
        }
        s = sb.toString();

        if (s.length() == 3) {
            // RGB -> RRGGBB
            char r = s.charAt(0);
            char g = s.charAt(1);
            char b = s.charAt(2);
            s = "" + r + r + g + g + b + b;
        }

        try {
            if (s.length() == 6) {
                int rgb = Integer.parseInt(s, 16) & 0x00FFFFFF;
                return ((currentAlpha & 0xFF) << 24) | rgb;
            }
            if (s.length() == 8) {
                // AARRGGBB
                long v = Long.parseLong(s, 16) & 0xFFFFFFFFL;
                return (int) v;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ensureColorWheelTexture();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int contentW = squareSize + GAP + hueWidth;

        String title = "Color Picker";
        float titleSize = 12f;
        float titleW = Fonts.SEMIBOLD.getWidth(title, titleSize);
        float titleH = Fonts.SEMIBOLD.getHeight(titleSize);

        int previewW = 60;
        int previewH = 20;
        int hexH = 16;
        int closeW = 60;
        int closeH = 16;

        float innerH = titleH + TITLE_GAP + squareSize + BELOW_SQUARE_GAP + previewH + 8 + hexH + BETWEEN_PREVIEW_CLOSE_GAP + closeH;
        int boxW = contentW + PAD * 2;
        int boxH = (int) Math.ceil(innerH) + PAD * 2;
        int boxX = centerX - boxW / 2;
        int boxY = centerY - boxH / 2;

        int contentX = centerX - contentW / 2;

        int titleX = (int) (centerX - titleW / 2f);
        int titleY = (int) (boxY + PAD + 2);

        int squareX = contentX;
        int squareY = (int) (titleY + titleH + TITLE_GAP);

        int hueX = squareX + squareSize + GAP;
        int hueY = squareY;

        int previewX = centerX - previewW / 2;
        int previewY = squareY + squareSize + BELOW_SQUARE_GAP;

        int closeX = centerX - closeW / 2;
        int hexX = contentX;
        int hexY = previewY + previewH + 8;
        int closeY = hexY + hexH + BETWEEN_PREVIEW_CLOSE_GAP;

        // рамка (без белого "свечения")
        Render2D.drawRoundedRect(context.getMatrices(),
            boxX, boxY,
            boxW, boxH,
            ROUND,
            new Color(0, 0, 0, 180));
        Render2D.drawBorder(context.getMatrices(),
            boxX, boxY,
            boxW, boxH,
            ROUND,
            0.6f, 0.6f,
            new Color(255, 255, 255, 28));

        // заголовок
        Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(titleSize),
            title, titleX, titleY, Color.WHITE);

        // Color wheel (Hue + Saturation)
        drawColorWheel(context, squareX, squareY, squareSize);

        // Brightness slider (replaces hue bar)
        drawBrightnessBar(context, hueX, hueY, hueWidth, squareSize);

        // превью цвета (большое)
        Color preview = Color.getHSBColor(hue, saturation, brightness);
        Render2D.drawRoundedRect(context.getMatrices(), previewX, previewY, previewW, previewH, 3f, preview);
        Render2D.drawBorder(context.getMatrices(), previewX, previewY, previewW, previewH, 3f, 0.6f, 0.6f, new Color(0, 0, 0, 110));

        // HEX input
        if (hexField != null) {
            int hexW = contentW;

            Render2D.drawRoundedRect(context.getMatrices(), hexX, hexY, hexW, hexH, 5f, new Color(0, 0, 0, 120));
            Render2D.drawBorder(context.getMatrices(), hexX, hexY, hexW, hexH, 5f, 0.6f, 0.6f, new Color(255, 255, 255, 30));

            hexField.setX(hexX + 6);
            hexField.setY(hexY + 3);
            hexField.setWidth(hexW - 12);
            hexField.setHeight(hexH);

            if (!hexField.isFocused()) {
                String desired = toHexString(setting.getValue());
                if (!desired.equalsIgnoreCase(hexField.getText())) {
                    updatingHexFromPicker = true;
                    hexField.setText(desired);
                    updatingHexFromPicker = false;
                }
            }
        }

        // кнопка закрыть
        Render2D.drawRoundedRect(context.getMatrices(), closeX, closeY, closeW, closeH, 3f,
            new Color(100, 20, 20, 180));
        Render2D.drawBorder(context.getMatrices(), closeX, closeY, closeW, closeH, 3f, 0.6f, 0.6f, new Color(0, 0, 0, 120));
        String closeText = "Close";
        float closeSize = 9f;
        float closeTextW = Fonts.MEDIUM.getWidth(closeText, closeSize);
        float closeTextH = Fonts.MEDIUM.getHeight(closeSize);
        Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(closeSize),
            closeText,
            closeX + (closeW - closeTextW) / 2f,
            closeY + (closeH - closeTextH) / 2f + 0.5f,
            Color.WHITE);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawColorWheel(DrawContext context, int x, int y, int size) {
        float s = size;
        if (colorWheelTexture != null) {
            Render2D.drawTexture(context.getMatrices(), x, y, s, s, s / 2f, colorWheelTexture, Color.WHITE);
            Render2D.drawBorder(context.getMatrices(), x, y, s, s, s / 2f, 0.6f, 0.6f, new Color(0, 0, 0, 80));
        } else {
            Render2D.drawRoundedRect(context.getMatrices(), x, y, s, s, s / 2f, new Color(0, 0, 0, 120));
            Render2D.drawBorder(context.getMatrices(), x, y, s, s, s / 2f, 0.6f, 0.6f, new Color(255, 255, 255, 18));
        }

        // Indicator dot
        float ang = (float) (hue * Math.PI * 2.0);
        float rr = (s / 2f) * saturation;
        float cx = x + s / 2f;
        float cy = y + s / 2f;
        float ix = cx + (float) Math.cos(ang) * rr;
        float iy = cy + (float) Math.sin(ang) * rr;
        float dot = 7f;
        Render2D.drawRoundedRect(context.getMatrices(), ix - dot / 2f, iy - dot / 2f, dot, dot, dot / 2f, new Color(0, 0, 0, 190));
        Render2D.drawRoundedRect(context.getMatrices(), ix - (dot - 2f) / 2f, iy - (dot - 2f) / 2f, dot - 2f, dot - 2f, (dot - 2f) / 2f, new Color(255, 255, 255, 235));
    }

    private void drawBrightnessBar(DrawContext context, int x, int y, int w, int h) {
        // Top = bright (1), bottom = dark (0)
        Color top = new Color(Color.HSBtoRGB(hue, saturation, 1f));
        Color bottom = new Color(0, 0, 0);
        Render2D.drawGradientRect(context.getMatrices(), x, y, w, h, top, bottom, false);

        // marker
        int markerY = (int) (y + (1f - brightness) * (h - 1));
        markerY = Math.max(y, Math.min(y + h - 1, markerY));
        Render2D.drawRoundedRect(context.getMatrices(), x - 2, markerY - 2, w + 4, 4, 2f, new Color(0, 0, 0, 200));
        Render2D.drawBorder(context.getMatrices(), x - 2, markerY - 2, w + 4, 4, 2f, 0.6f, 0.6f, new Color(255, 255, 255, 25));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int contentW = squareSize + GAP + hueWidth;

        float titleSize = 12f;
        float titleH = Fonts.SEMIBOLD.getHeight(titleSize);

        int previewW = 60;
        int previewH = 20;
        int hexH = 16;
        int closeW = 60;
        int closeH = 16;

        float innerH = titleH + TITLE_GAP + squareSize + BELOW_SQUARE_GAP + previewH + 8 + hexH + BETWEEN_PREVIEW_CLOSE_GAP + closeH;
        int boxW = contentW + PAD * 2;
        int boxH = (int) Math.ceil(innerH) + PAD * 2;
        int boxY = centerY - boxH / 2;

        int contentX = centerX - contentW / 2;
        int titleY = (int) (boxY + PAD + 2);

        int squareX = contentX;
        int squareY = (int) (titleY + titleH + TITLE_GAP);

        int hueX = squareX + squareSize + GAP;
        int hueY = squareY;

        int previewX = centerX - previewW / 2;
        int previewY = squareY + squareSize + BELOW_SQUARE_GAP;

        int hexX = contentX;
        int hexY = previewY + previewH + 8;

        int closeX = centerX - closeW / 2;
        int closeY = hexY + hexH + BETWEEN_PREVIEW_CLOSE_GAP;

        // hex focus
        if (hexField != null) {
            int hexW = contentW;
            if (isHovered(mouseX, mouseY, hexX, hexY, hexW, hexH)) {
                hexField.setFocused(true);
                if (hexField.mouseClicked(mouseX, mouseY, button)) return true;
                return true;
            } else {
                hexField.setFocused(false);
            }
        }

        // кнопка Close
        if (isHovered(mouseX, mouseY, closeX, closeY, closeW, closeH)) {
            this.close();
            return true;
        }

        // wheel
        if (isHovered(mouseX, mouseY, squareX, squareY, squareSize, squareSize)) {
            draggingWheel = true;
            updateWheel(mouseX, mouseY, squareX, squareY, squareSize);
            return true;
        }

        // brightness bar
        if (isHovered(mouseX, mouseY, hueX, hueY, hueWidth, squareSize)) {
            draggingBrightness = true;
            updateBrightness(mouseY, hueY, squareSize);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWheel = false;
        draggingBrightness = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int contentW = squareSize + GAP + hueWidth;
        float titleSize = 12f;
        float titleH = Fonts.SEMIBOLD.getHeight(titleSize);
        int previewH = 20;
        int closeH = 16;

        int hexH = 16;
        float innerH = titleH + TITLE_GAP + squareSize + BELOW_SQUARE_GAP + previewH + 8 + hexH + BETWEEN_PREVIEW_CLOSE_GAP + closeH;
        int boxH = (int) Math.ceil(innerH) + PAD * 2;
        int boxY = centerY - boxH / 2;

        int contentX = centerX - contentW / 2;
        int titleY = (int) (boxY + PAD + 2);

        int squareX = contentX;
        int squareY = (int) (titleY + titleH + TITLE_GAP);
        int hueY = squareY;

        if (draggingWheel) {
            updateWheel(mouseX, mouseY, squareX, squareY, squareSize);
            return true;
        } else if (draggingBrightness) {
            updateBrightness(mouseY, hueY, squareSize);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private void updateWheel(double mouseX, double mouseY, int x, int y, int size) {
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        float dx = (float) (mouseX - centerX);
        float dy = (float) (mouseY - centerY);
        float r = size / 2f;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > r) return;

        saturation = clamp(dist / r, 0f, 1f);
        float ang = (float) Math.atan2(dy, dx);
        float h = (ang / (float) (Math.PI * 2.0));
        if (h < 0f) h += 1f;
        hue = h;

        updateColor();
    }

    private void updateBrightness(double mouseY, int y, int h) {
        float value = 1f - (float) (mouseY - y) / (float) h;
        brightness = clamp(value, 0f, 1f);
        updateColor();
    }

    private void updateColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int a = (setting.getValue() >>> 24) & 0xFF;
        int argb = (a << 24) | (rgb & 0x00FFFFFF);
        setting.set(argb);

        if (hexField != null && !hexField.isFocused()) {
            String desired = toHexString(setting.getValue());
            if (!desired.equalsIgnoreCase(hexField.getText())) {
                updatingHexFromPicker = true;
                hexField.setText(desired);
                updatingHexFromPicker = false;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hexField != null && hexField.isFocused()) {
            // Apply on Enter
            if (keyCode == 257 || keyCode == 335) { // GLFW_ENTER / GLFW_KP_ENTER
                Integer parsed = tryParseHexToArgb(hexField.getText(), (setting.getValue() >>> 24) & 0xFF);
                if (parsed != null) {
                    applyArgb(parsed);
                    hexField.setFocused(false);
                    return true;
                }
            }
            if (hexField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (hexField != null && hexField.isFocused()) {
            return hexField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void close() {
        if (this.client == null) return;
        this.client.setScreen(parent);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // прозрачный фон
    }

    @Override
    public boolean shouldPause() {
        return false; // меню не ставит игру на паузу
    }
}
