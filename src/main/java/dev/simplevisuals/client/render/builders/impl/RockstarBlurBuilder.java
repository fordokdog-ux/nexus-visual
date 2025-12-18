package dev.simplevisuals.client.render.builders.impl;

import dev.simplevisuals.client.render.builders.AbstractBuilder;
import dev.simplevisuals.client.render.builders.states.QuadColorState;
import dev.simplevisuals.client.render.builders.states.QuadRadiusState;
import dev.simplevisuals.client.render.builders.states.SizeState;
import dev.simplevisuals.client.render.renderers.impl.BuiltBlur;
import dev.simplevisuals.client.render.renderers.impl.RockstarBuiltBlur;

public final class RockstarBlurBuilder extends AbstractBuilder<RockstarBuiltBlur> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private float blurRadius;

    public RockstarBlurBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public RockstarBlurBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public RockstarBlurBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public RockstarBlurBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public RockstarBlurBuilder blurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }

    @Override
    protected RockstarBuiltBlur _build() {
        return new RockstarBuiltBlur(
            this.size,
            this.radius,
            this.color,
            this.smoothness,
            this.blurRadius
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.smoothness = 1.0f;
        this.blurRadius = 0.0f;
    }
}