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
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.managers.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.client.ui.clickgui.render.ClickGuiDraw;

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

		float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
		boolean lightUi = ClickGui.isGuiLightMode();

		String valueText = binding ? "Press..." : setting.getValue().toString().replace("_", " ");
		float valueFont = 6.5f;
		float textWidth = Fonts.BOLD.getWidth(valueText, valueFont);
		float finalWidth = animation.animate(Math.max(22f, textWidth + 12f), 200);

		Color themeText = ThemeManager.getInstance().getTextColor();
		Color accent = ThemeManager.getInstance().getAccentColor();
		int textA = (int) (Math.max(0f, Math.min(1f, ga)) * themeText.getAlpha());
		Color labelCol = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), textA);

		boolean hovered = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);
		if (hovered) {
			Color hoverBg = lightUi
					? new Color(0, 0, 0, (int) (18f * ga))
					: new Color(255, 255, 255, (int) (12f * ga));
			ClickGuiDraw.roundedRect(x + 2f, y + 1f, width - 4f, height - 2f, 6f, hoverBg);
		}

		ClickGuiDraw.text(Fonts.BOLD.getFont(7.5f), I18n.translate(setting.getName()), x + 4f, y + 3f, labelCol);

		float pillX = x + width - finalWidth - 4f;
		float pillY = y + 3.5f;
		float pillH = 11f;
		float pillR = pillH / 2f;

		Color pillBg;
		if (binding) {
			pillBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (150f * ga));
		} else {
			pillBg = lightUi
					? new Color(0, 0, 0, (int) (18f * ga))
					: new Color(255, 255, 255, (int) (14f * ga));
		}
		ClickGuiDraw.roundedRect(pillX, pillY, finalWidth, pillH, pillR, pillBg);

		if (hovered || binding) {
			int borderA = (int) ((binding ? 160f : 90f) * ga);
			ClickGuiDraw.roundedBorder(pillX, pillY, finalWidth, pillH, pillR, 1.0f,
					new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderA));
		}

		Color valueCol = binding
				? new Color(255, 255, 255, (int) (240f * ga))
				: (lightUi
						? new Color(25, 25, 25, (int) (220f * ga))
						: new Color(235, 235, 235, (int) (220f * ga)));
		float valueH = Fonts.BOLD.getHeight(valueFont);
		ClickGuiDraw.text(Fonts.BOLD.getFont(valueFont), valueText,
				pillX + (finalWidth - textWidth) / 2f,
				pillY + (pillH - valueH) / 2f + 0.25f,
				valueCol);
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		String text = binding ? "..." : setting.getValue().toString().replace("_", " ");
		float valueFont = 6.5f;
		float textWidth = Fonts.BOLD.getWidth(text, valueFont);
		float pillW = Math.max(22f, textWidth + 12f);
		float pillX = x + width - pillW - 4f;
		float pillY = y + 2f;
		float pillH = height - 6f;
		if (MathUtils.isHovered(pillX, pillY, pillW, pillH, (float) mouseX, (float) mouseY) && !binding && button == 0) {
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