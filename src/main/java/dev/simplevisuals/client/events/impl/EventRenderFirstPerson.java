package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public class EventRenderFirstPerson extends Event {
    private final MatrixStack matrices;
    private final float swingProgress;
    private final float equipProgress;
    private final Arm arm;
    private boolean cancelled;

    public EventRenderFirstPerson(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        this.matrices = matrices;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
        this.arm = arm;
        this.cancelled = false;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public float getSwingProgress() {
        return swingProgress;
    }

    public float getEquipProgress() {
        return equipProgress;
    }

    public Arm getArm() {
        return arm;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}