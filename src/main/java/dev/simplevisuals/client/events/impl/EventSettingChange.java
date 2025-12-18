package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import dev.simplevisuals.modules.settings.Setting;

public class EventSettingChange extends Event {
    private final Setting<?> setting;

    public EventSettingChange(Setting<?> setting) {
        this.setting = setting;
    }

    public Setting<?> getSetting() {
        return setting;
    }
}