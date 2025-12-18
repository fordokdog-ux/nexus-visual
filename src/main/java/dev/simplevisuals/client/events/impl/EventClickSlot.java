package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.screen.slot.SlotActionType;

@Getter
public class EventClickSlot extends Event {
	private final SlotActionType slotActionType;
	private final int slot;
	private final int button;
	private final int id;
	@Setter private boolean cancel;

	public EventClickSlot(SlotActionType slotActionType, int slot, int button, int id) {
		this.slotActionType = slotActionType;
		this.slot = slot;
		this.button = button;
		this.id = id;
	}
} 