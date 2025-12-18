package dev.simplevisuals.modules.settings.api;

import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class Bind {
	public enum Mode { TOGGLE, HOLD }

	private int key;
	private boolean mouse;
	private Mode mode = Mode.TOGGLE;

	public Bind(int key, boolean mouse) {
		this.key = key;
		this.mouse = mouse;
		this.mode = Mode.TOGGLE;
	}

	public Bind(int key, boolean mouse, Mode mode) {
		this.key = key;
		this.mouse = mouse;
		this.mode = mode == null ? Mode.TOGGLE : mode;
	}

	@Override
	public String toString() {
		if (key == -1) return "None";
		if (mouse) return "M" + (key + 1);

		String str = "";
		try {
			for (Field field : GLFW.class.getDeclaredFields()) {
				if (field.getName().startsWith("GLFW_KEY_")) {
					int a = (int) field.get(null);
					if (a == key) {
						String nb = field.getName().substring("GLFW_KEY_".length());
						str = nb.substring(0, 1).toUpperCase() + nb.substring(1).toLowerCase();
						break;
					}
				}
			}
		} catch (Exception ignored) {}

		return str.toUpperCase();
	}

	public boolean isEmpty() {
		return key < 0;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public boolean isMouse() {
		return mouse;
	}

	public void setMouse(boolean mouse) {
		this.mouse = mouse;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}
}