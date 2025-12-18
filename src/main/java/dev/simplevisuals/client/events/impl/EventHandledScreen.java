package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

@Getter
public class EventHandledScreen extends Event {
	private final DrawContext drawContext;
	private final Slot slotHover;
	private final int backgroundWidth;
	private final int backgroundHeight;

	public EventHandledScreen(DrawContext drawContext, Slot slotHover, int backgroundWidth, int backgroundHeight) {
		this.drawContext = drawContext;
		this.slotHover = slotHover;
		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}
} 