package dev.simplevisuals.client.ui.clickgui.components.impl;

import java.awt.Color;

import org.lwjgl.glfw.GLFW;

import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.modules.settings.impl.BindSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.animations.infinity.InfinityAnimation;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.managers.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

public class BindComponent extends Component {
	
	private final BindSetting setting;
	private final InfinityAnimation animation = new InfinityAnimation(Easing.LINEAR);
	private final Animation bindingAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
	private boolean binding;

	public BindComponent(BindSetting setting) {
		super(setting.getName());
		this.setting = setting;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		bindingAnimation.update(binding);
		String text = binding ? "..." : setting.getValue().toString().replace("_", " ");
		float textWidth = Fonts.REGULAR.getWidth(text, 6.5f);
		float finalWidth = animation.animate(textWidth + 4f, 200);
		// theme colors
		Color themeText = ThemeManager.getInstance().getCurrentTheme().getTextColor();
		Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
		Color boxColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160);
		Color textColorNormal = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), 220);
		int alphaBind = (int) (255 * bindingAnimation.getReversedValue());
		int alphaDots = (int) (255 * bindingAnimation.getValue());
		Color textColorBind = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), alphaBind);
		Color textColorDots = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), alphaDots);
		Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(7.5f), I18n.translate(setting.getName()), x + 4f, y + 3f, textColorNormal);
		Render2D.drawRoundedRect(context.getMatrices(), x + width - finalWidth - 4f, y, finalWidth, height - 7f, 1.5f, boxColor);
		Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(6.5f), setting.getValue().toString().replace("_", " "), x + width - textWidth - 6f, y + 2.3f, textColorBind);
		Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(6.5f), "...", x + width - textWidth - 6f, y + 2f, textColorDots);
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		String text = binding ? "..." : setting.getValue().toString().replace("_", " ");
		float textWidth = Fonts.REGULAR.getWidth(text, 6.5f);
		if (MathUtils.isHovered(x + width - 8f - textWidth, y + 2f, textWidth + 4f, height - 4f, (float) mouseX, (float) mouseY) && !binding && button == 0) {
			binding = true;
			return;
		}
		
		if (binding) {
			Bind.Mode mode = setting.getValue() != null ? setting.getValue().getMode() : Bind.Mode.TOGGLE;
			setting.setValue(new Bind(button, true, mode));
			binding = false;
			return;
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {
		
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		if (binding) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) setting.setValue(new Bind(-1, false));
			else {
				Bind.Mode mode = setting.getValue() != null ? setting.getValue().getMode() : Bind.Mode.TOGGLE;
				setting.setValue(new Bind(keyCode, false, mode));
			}
			binding = false;
		}
	}

	@Override
	public void keyReleased(int keyCode, int scanCode, int modifiers) {
		
	}

	@Override
	public void charTyped(char chr, int modifiers) {
		
	}//
}