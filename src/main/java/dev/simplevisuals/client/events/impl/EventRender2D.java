package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@AllArgsConstructor @Getter
public class EventRender2D extends Event {
    private DrawContext context;
    private RenderTickCounter tickCounter;

    public float getTickDelta() {
        return tickCounter.getTickDelta(true);
    }
}