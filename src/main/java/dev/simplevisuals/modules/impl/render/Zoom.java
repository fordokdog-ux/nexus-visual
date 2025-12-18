package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import lombok.Getter;
import net.minecraft.client.resource.language.I18n;

public class Zoom extends Module {
	@Getter private final NumberSetting zoomStrength = new NumberSetting(
			"setting.zoomStrength",
			3.0f,
			1.0f,
			15.0f,
			0.1f
	);

	@Getter private final BooleanSetting smoothZoom = new BooleanSetting(
			"setting.smoothZoom",
			true
	);

	@Getter private final NumberSetting smoothSpeed = new NumberSetting(
			"setting.smoothSpeed",
			10.0f,
			1.0f,
			15.0f,
			1.0f,
			() -> smoothZoom.getValue()
	);

	private float currentFov = -1.0f;
	private float originalFov = -1.0f;

	public Zoom() {
		super("Zoom", Category.Render, I18n.translate("module.zoom.description"));
	}

	@Override
	public void onEnable() {
		super.onEnable();
		currentFov = -1.0f;
		originalFov = -1.0f;
	}

	@Override
	public void onDisable() {
		super.onDisable();
		// Keep state to allow smooth return handled in applyZoom
	}

	public float applyZoom(float baseFov, float tickDelta) {
		// Capture the unzoomed FOV once to avoid feedback-induced jitter
		if (originalFov < 0.0f) originalFov = baseFov;

		boolean active = isToggled();
		float strength = Math.max(1.0f, zoomStrength.getValue());
		float targetFov = active ? (originalFov / strength) : originalFov;

		if (!smoothZoom.getValue()) {
			// Instant mode
			currentFov = targetFov;
			// If returned, clear state
			if (!active && Math.abs(currentFov - originalFov) <= 0.0001f) {
				currentFov = -1.0f;
				originalFov = -1.0f;
				return baseFov;
			}
			return currentFov;
		}

		if (currentFov < 0.0f) currentFov = baseFov;

		float speedPerSecond = smoothSpeed.getValue();
		float dtSeconds = tickDelta / 20.0f;
		if (dtSeconds < 0.0f) dtSeconds = 0.0f;
		float alpha = 1.0f - (float) Math.exp(-speedPerSecond * dtSeconds);
		if (alpha > 1.0f) alpha = 1.0f;

		currentFov += (targetFov - currentFov) * alpha;

		// Snap when close enough to prevent micro-oscillation
		float epsilon = 0.0005f;
		if (Math.abs(targetFov - currentFov) <= epsilon) {
			currentFov = targetFov;
		}

		// When return finished, clear state next frames but keep this frame value to avoid step
		if (!active && currentFov == originalFov) {
			currentFov = -1.0f;
			originalFov = -1.0f;
			return baseFov;
		}

		return currentFov;
	}
} 