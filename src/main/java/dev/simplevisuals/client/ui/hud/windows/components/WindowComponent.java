package dev.simplevisuals.client.ui.hud.windows.components;

import dev.simplevisuals.client.ui.hud.windows.components.impl.BooleanComponent;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import lombok.*;

@Getter @Setter
public abstract class WindowComponent extends Component {
	protected Animation animation;

	public WindowComponent(String name) {
		super(name);
	}
}