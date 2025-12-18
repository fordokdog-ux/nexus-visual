package dev.simplevisuals.modules.settings.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import dev.simplevisuals.modules.settings.Setting;

public class ListSetting extends Setting<List<BooleanSetting>> {

	private boolean singleSelect = false;

	public ListSetting(String name, BooleanSetting... values) {
		super(name, Arrays.asList(values));
	}

	public ListSetting(String name, Supplier<Boolean> visible, BooleanSetting... values) {
		super(name, Arrays.asList(values), visible);
	}

	// New: allow specifying single-select behavior via constructor
	public ListSetting(String name, boolean singleSelect, BooleanSetting... values) {
		super(name, Arrays.asList(values));
		this.singleSelect = singleSelect;
	}

	// New: allow specifying visibility and single-select behavior via constructor
	public ListSetting(String name, Supplier<Boolean> visible, boolean singleSelect, BooleanSetting... values) {
		super(name, Arrays.asList(values), visible);
		this.singleSelect = singleSelect;
	}

	public BooleanSetting getName(String name) {
		return getValue().stream().filter(setting -> setting.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	public List<BooleanSetting> getToggled() {
		return getValue().stream().filter(Setting::getValue).toList();
	}

	public boolean isSingleSelect() {
		return singleSelect;
	}

	public ListSetting setSingleSelect(boolean singleSelect) {
		this.singleSelect = singleSelect;
		return this;
	}
}