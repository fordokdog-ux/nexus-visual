package dev.simplevisuals.client.ui.hud.windows;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import dev.simplevisuals.client.ui.hud.windows.components.impl.BooleanComponent;
import dev.simplevisuals.client.ui.hud.windows.components.WindowComponent;
import dev.simplevisuals.client.ui.hud.windows.components.impl.BooleanComponent;
import dev.simplevisuals.client.ui.hud.windows.components.impl.ListComponent;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.modules.settings.Setting;

import net.minecraft.client.gui.DrawContext;
import dev.simplevisuals.modules.settings.impl.*;
import lombok.*;

@Getter @Setter
public class Window {
	private float x, y, width, height;
	private final List<Setting<?>> settings;
	private final List<WindowComponent> components = new ArrayList<>();
	private final Animation animation = new Animation(300, 1f, true, Easing.BOTH_SINE);
	// Match theme background (respect theme alpha)
	private static Color resolveBgColor() {
		ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
		Color bg = theme == null ? null : theme.getBackgroundColor();
		return bg == null ? new Color(30, 30, 30, 240) : bg;
	}
	
	public Window(float x, float y, float width, float height, List<Setting<?>> settings) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.settings = settings;
		for (Setting<?> setting : settings) {
			if (setting instanceof BooleanSetting) components.add(new BooleanComponent(setting.getName(), ((BooleanSetting) setting)));
			else if (setting instanceof ListSetting) components.add(new ListComponent(setting.getName(), ((ListSetting) setting)));
		}
	}
	
	public void render(DrawContext context, int mouseX, int mouseY) {
		Render2D.startScissor(
				context,
				getX() + (getWidth() - getWidth() * animation.getValue()) / 2,
				getY() + (getFinalHeight() - getFinalHeight() * animation.getValue()) / 2,
				getWidth() * animation.getValue(),
				getFinalHeight() * animation.getValue()
		);

		// Window background same as Watermark
		// Window background
		Render2D.drawRoundedRect(
				context.getMatrices(),
				getX() + (getWidth() - getWidth() * animation.getValue()) / 2,
				getY() + (getFinalHeight() - getFinalHeight() * animation.getValue()) / 2,
				getWidth() * animation.getValue(),
				getFinalHeight() * animation.getValue(),
				3.5f,
				resolveBgColor()
		);

		float finalY = y;

		for (WindowComponent component : components) {
			if (!component.getVisible().get()) continue;
			component.setX(x);
			component.setY(finalY);
			component.setWidth(width);
			component.setHeight(height);
			component.setAnimation(animation);
			component.render(context, mouseX, mouseY, 0);
			finalY += component.getHeight() + component.getAddHeight().get() + 4.5f;
		}

		Render2D.stopScissor(context);
	}

	public void reset() {
		animation.update(false);
	}

	public boolean closed() {
		return animation.finished(false) && animation.getValue() <= 0.01f;
	}
	
	public void mouseClicked(double mouseX, double mouseY, int button) {
		for (WindowComponent component : components) component.mouseClicked(mouseX, mouseY, button);
	}
	
	public float getFinalHeight() {
		float height = 0;
		for (WindowComponent component : components) {
			if (!component.getVisible().get()) continue;
			height += component.getHeight() + component.getAddHeight().get() + 4.5f;
		}
		
		return height;
	}
}