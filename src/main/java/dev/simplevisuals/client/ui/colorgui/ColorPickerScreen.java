package dev.simplevisuals.client.ui.colorgui;

import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

public class ColorPickerScreen extends Screen {
    private final ColorSetting setting;
    private float hue, saturation, brightness;

    // состояние перетаскивания
    private boolean draggingSquare = false;
    private boolean draggingHue = false;

    // размеры
    private final int squareSize = 150;
    private final int hueWidth = 15;

    // кеш для квадрата
    private int[] squarePixels;

    public ColorPickerScreen(ColorSetting setting) {
        super(Text.of("Color Picker"));
        this.setting = setting;

        Color color = new Color(setting.getValue());
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];

        generateSquareBuffer();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // рамка
        Render2D.drawStyledRect(context.getMatrices(),
                centerX - squareSize - hueWidth - 20, centerY - squareSize / 2 - 20,
                squareSize + hueWidth + 40, squareSize + 60, 5f,
                new Color(0, 0, 0, 180), 255);

        // заголовок
        Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(12f),
                "Color Picker", centerX - 30, centerY - squareSize / 2 - 10, Color.WHITE);

        // квадрат (Saturation + Brightness)
        drawColorSquare(context, centerX - squareSize / 2 - 20, centerY - squareSize / 2);

        // hue-слайдер справа
        drawHueBar(context, centerX + squareSize / 2, centerY - squareSize / 2, hueWidth, squareSize);

        // превью цвета (большое)
        Color preview = Color.getHSBColor(hue, saturation, brightness);
        Render2D.drawStyledRect(context.getMatrices(),
                centerX - 30, centerY + squareSize / 2 + 10, 60, 20, 3f, preview, 255);

        // кнопка закрыть
        Render2D.drawStyledRect(context.getMatrices(),
                centerX - 30, centerY + squareSize / 2 + 40, 60, 16, 3f,
                new Color(100, 20, 20, 180), 255);
        Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(9f),
                "Close", centerX - 15, centerY + squareSize / 2 + 43, Color.WHITE);

        super.render(context, mouseX, mouseY, delta);
    }

    private void generateSquareBuffer() {
        squarePixels = new int[squareSize * squareSize];
        for (int i = 0; i < squareSize; i++) {
            for (int j = 0; j < squareSize; j++) {
                float sat = (float) i / (float) squareSize;
                float bri = 1f - (float) j / (float) squareSize;
                squarePixels[j * squareSize + i] = Color.getHSBColor(hue, sat, bri).getRGB();
            }
        }
    }

    private void drawColorSquare(DrawContext context, int x, int y) {
        int step = 2; // размер блока (2x2)
        for (int i = 0; i < squareSize; i += step) {
            for (int j = 0; j < squareSize; j += step) {
                int rgb = squarePixels[j * squareSize + i];
                Render2D.drawRect(context.getMatrices(), x + i, y + j, step, step, new Color(rgb));
            }
        }

        // маркер позиции
        int markerX = x + (int) (saturation * squareSize);
        int markerY = y + (int) ((1 - brightness) * squareSize);
        Render2D.drawStyledRect(context.getMatrices(), markerX - 3, markerY - 3, 6, 6, 2f, Color.WHITE, 255);

        // маленький предпоказ рядом с маркером
        Color current = Color.getHSBColor(hue, saturation, brightness);
        Render2D.drawStyledRect(context.getMatrices(), markerX + 8, markerY - 3, 10, 10, 2f, current, 255);
    }

    private void drawHueBar(DrawContext context, int x, int y, int w, int h) {
        for (int j = 0; j < h; j++) {
            float hueVal = (float) j / (float) h;
            Color c = Color.getHSBColor(hueVal, 1f, 1f);
            Render2D.drawRect(context.getMatrices(), x, y + j, w, 1, c);
        }

        // маркер hue
        int markerY = y + (int) (hue * h);
        Render2D.drawStyledRect(context.getMatrices(), x - 2, markerY - 2, w + 4, 4, 2f, Color.BLACK, 255);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int squareX = centerX - squareSize / 2 - 20;
        int squareY = centerY - squareSize / 2;

        int hueX = centerX + squareSize / 2;
        int hueY = centerY - squareSize / 2;

        // кнопка Close
        if (isHovered(mouseX, mouseY, centerX - 30, centerY + squareSize / 2 + 40, 60, 16)) {
            this.close();
            return true;
        }

        // квадрат
        if (isHovered(mouseX, mouseY, squareX, squareY, squareSize, squareSize)) {
            draggingSquare = true;
            updateSquare(mouseX, mouseY, squareX, squareY, squareSize, squareSize);
            return true;
        }

        // hue бар
        if (isHovered(mouseX, mouseY, hueX, hueY, hueWidth, squareSize)) {
            draggingHue = true;
            updateHue(mouseY, hueY, squareSize);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSquare = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int squareX = centerX - squareSize / 2 - 20;
        int squareY = centerY - squareSize / 2;
        int hueY = centerY - squareSize / 2;

        if (draggingSquare) {
            updateSquare(mouseX, mouseY, squareX, squareY, squareSize, squareSize);
            return true;
        } else if (draggingHue) {
            updateHue(mouseY, hueY, squareSize);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private void updateSquare(double mouseX, double mouseY, int x, int y, int w, int h) {
        float sat = (float) (mouseX - x) / (float) w;
        float bri = 1f - (float) (mouseY - y) / (float) h;
        saturation = Math.max(0f, Math.min(1f, sat));
        brightness = Math.max(0f, Math.min(1f, bri));
        updateColor();
    }

    private void updateHue(double mouseY, int y, int h) {
        float value = (float) (mouseY - y) / (float) h;
        hue = Math.max(0f, Math.min(1f, value));
        generateSquareBuffer(); // пересчёт квадрата только при смене hue
        updateColor();
    }

    private void updateColor() {
        int rgb = Color.getHSBColor(hue, saturation, brightness).getRGB();
        setting.set(rgb);
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void close() {
        this.client.setScreen(null);
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
