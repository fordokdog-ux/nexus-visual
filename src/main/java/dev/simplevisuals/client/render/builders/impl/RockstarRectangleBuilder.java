package dev.simplevisuals.client.render.builders.impl;

import dev.simplevisuals.client.render.builders.AbstractBuilder;
import dev.simplevisuals.client.render.builders.states.QuadColorState;
import dev.simplevisuals.client.render.builders.states.QuadRadiusState;
import dev.simplevisuals.client.render.builders.states.SizeState;
import dev.simplevisuals.client.render.renderers.impl.BuiltRectangle;
import dev.simplevisuals.client.render.renderers.impl.RockstarBuiltRectangle;

public final class RockstarRectangleBuilder extends AbstractBuilder<RockstarBuiltRectangle> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;

    public RockstarRectangleBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public RockstarRectangleBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public RockstarRectangleBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public RockstarRectangleBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    @Override
    protected RockstarBuiltRectangle _build() {
        return new RockstarBuiltRectangle(
            this.size,
            this.radius,
            this.color,
            this.smoothness
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.smoothness = 1.0f;
    }
}