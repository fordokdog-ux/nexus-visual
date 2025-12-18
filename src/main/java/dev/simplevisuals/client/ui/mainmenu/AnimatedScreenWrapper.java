package dev.simplevisuals.client.ui.mainmenu;

import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class AnimatedScreenWrapper extends Screen {
    private final Screen wrappedScreen;
    private final Screen parentScreen;
    private Animation alphaAnimation;
    private boolean isEntering = true;

    public AnimatedScreenWrapper(Screen wrappedScreen, Screen parentScreen) {
        super(wrappedScreen != null ? wrappedScreen.getTitle() : Text.empty());
        this.wrappedScreen = wrappedScreen;
        this.parentScreen = parentScreen;
        this.alphaAnimation = new Animation(500, 1.0, true, Easing.OUT_EXPO);
    }

    @Override
    protected void init() {
        super.init();
        if (wrappedScreen != null) wrappedScreen.init(client, width, height);
        alphaAnimation = new Animation(500, 1.0, true, Easing.OUT_EXPO); // Reinitialize for entry
        isEntering = true;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // Render the wrapped screen
        if (wrappedScreen != null) wrappedScreen.render(drawContext, mouseX, mouseY, delta);

        // Apply fade overlay
        float alpha = alphaAnimation.getValue();
        if (isEntering) {
            drawContext.fill(0, 0, width, height, (int)(255 * (1.0f - alpha)) << 24);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return wrappedScreen != null && wrappedScreen.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return wrappedScreen != null && wrappedScreen.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return wrappedScreen != null && wrappedScreen.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return wrappedScreen != null && wrappedScreen.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return wrappedScreen != null && wrappedScreen.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return wrappedScreen != null && wrappedScreen.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return wrappedScreen != null && wrappedScreen.charTyped(chr, modifiers);
    }

    @Override
    public void tick() {
        if (wrappedScreen != null) wrappedScreen.tick();
        if (isEntering && alphaAnimation.finished()) { // Changed from isFinished() to finished()
            isEntering = false;
        }
    }

    @Override
    public void close() {
        // When closing, transition back to parent without wrapping null
        if (client != null) {
            if (parentScreen != null) {
                client.setScreen(parentScreen);
            } else {
                client.setScreen(null);
            }
        }
    }
}