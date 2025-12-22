package dev.simplevisuals.client.ui.hud.windows.components.impl;

import java.awt.Color;


import dev.simplevisuals.client.ui.hud.windows.components.WindowComponent;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.NexusVisual;

public class BooleanComponent extends WindowComponent {
	
	private final BooleanSetting setting;
	private final Animation toggleAnimation = new Animation(300, 1, false, Easing.simplevisuals);
	
	public BooleanComponent(String name, BooleanSetting setting) {
		super(name);
		this.setting = setting;
	}

	private static boolean hoveredInclusive(float x, float y, float w, float h, float mx, float my, float pad) {
		float x0 = x - pad;
		float y0 = y - pad;
		float x1 = x + w + pad;
		float y1 = y + h + pad;
		return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		toggleAnimation.update(setting.getValue());
		// Text stays readable; use theme text color and translate
		Color textColor = ThemeManager.getInstance().getTextColor();
		Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(8f), I18n.translate(getName()), x + 5f, y + 4f, new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), (int) (255 * animation.getValue())));

		// Toggle background track
		Render2D.drawRoundedRect(context.getMatrices(), x + width - 20f, y + 4.5f, 16f, 8f, 2.5f, new Color(23, 23, 23, 100));
		// Toggle fill in theme accent color
		Color accent = ThemeManager.getInstance().getAccentColor();
		Render2D.drawRoundedRect(context.getMatrices(), x + width - 20f, y + 4.5f, 16f * toggleAnimation.getValue(), 8f, 2.5f, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (255 * toggleAnimation.getLinear())));
		// Knob stays white
		Render2D.drawRoundedRect(context.getMatrices(), x + width - 19.5f + (8f * toggleAnimation.getValue()), y + 5f, 7f, 7f, 2.5f, Color.WHITE);
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0 && button != 1) return;

		float mx = (float) mouseX;
		float my = (float) mouseY;

		float switchX = x + width - 20f;
		float switchY = y + 4.5f;
		float switchW = 16f;
		float switchH = 8f;

		boolean inRow = hoveredInclusive(x, y, width, height, mx, my, 1.5f);
		boolean inSwitch = hoveredInclusive(switchX, switchY, switchW, switchH, mx, my, 2.0f);
		if (!(inRow || inSwitch)) return;

		setting.setValue(!setting.getValue());
		// Ensure immediate persistence
		try {
			NexusVisual.getInstance().getAutoSaveManager().forceSave();
		} catch (Throwable ignored) {}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		
	}

	@Override
	public void keyReleased(int keyCode, int scanCode, int modifiers) {
		
	}

	@Override
	public void charTyped(char chr, int modifiers) {
		
	}
}