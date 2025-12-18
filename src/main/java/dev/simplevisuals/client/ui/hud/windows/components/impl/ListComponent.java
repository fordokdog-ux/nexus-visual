package dev.simplevisuals.client.ui.hud.windows.components.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import dev.simplevisuals.client.ui.hud.windows.components.WindowComponent;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.NexusVisual;

public class ListComponent extends WindowComponent {
	
	private final ListSetting setting;
	private final Map<BooleanSetting, Animation> pickAnimations = new HashMap<>();

	public ListComponent(String name, ListSetting setting) {
		super(name);
		this.setting = setting;
		for (BooleanSetting setting1 : setting.getValue()) pickAnimations.put(setting1, new Animation(300, 1f, false, Easing.BOTH_SINE));
		this.addHeight = () -> setting.getValue().size() * 14f;
		this.visible = setting::isVisible;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
		Render2D.drawFont(context.getMatrices(),
				Fonts.REGULAR.getFont(7.5f),
				I18n.translate(setting.getName()),
				x + 5f,
				y + 3.5f,
				new Color(255, 255, 255, (int) (255 * ga))
		);
		
		Render2D.drawFont(context.getMatrices(),
				Fonts.REGULAR.getFont(7.5f),
		            "(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")",
		            x + width - Fonts.REGULAR.getWidth("(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")", 7.5f) - 5f,
				y + 3.5f,
				new Color(255, 255, 255, (int) (255 * ga))
		);
		
		float yOffset = height;
		for (BooleanSetting setting : setting.getValue()) {
			Animation anim = pickAnimations.get(setting);
			anim.update(setting.getValue());
			Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", x + width - 14f, y + yOffset + 3.5f, new Color(0, 0, 0, (int) (255 * anim.getValue() * ga)));
			Render2D.drawFont(context.getMatrices(),
					Fonts.REGULAR.getFont(7.5f),
					I18n.translate(setting.getName()),
					x + 6f,
					y + yOffset + 3.5f,
					new Color(255, 255, 255, (int) (255 * ga))
			);
			yOffset += 14f;
		}
	}
	
	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			float yOffset = height;
			for (BooleanSetting s : setting.getValue()) {
				if (MathUtils.isHovered(x, y + yOffset, width, 14f, (float) mouseX, (float) mouseY)) {
					if (setting.isSingleSelect()) {
						for (BooleanSetting all : setting.getValue()) all.setValue(false);
						s.setValue(true);
					} else {
						s.setValue(!s.getValue());
					}
					// Persist immediately so HUD layout survives abrupt exit
					try {
						NexusVisual.getInstance().getAutoSaveManager().forceSave();
					} catch (Throwable ignored) {}
					break;
				}
				yOffset += 14f;
			}
		}
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
