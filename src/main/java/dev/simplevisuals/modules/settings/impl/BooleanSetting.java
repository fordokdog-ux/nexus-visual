package dev.simplevisuals.modules.settings.impl;

import dev.simplevisuals.modules.settings.Setting;

import java.util.function.Supplier;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, Boolean defaultValue, Supplier<Boolean> visible) {
        super(name, defaultValue, visible);
    }

    public BooleanSetting(String name, Boolean defaultValue) {
        super(name, defaultValue);
    }
}