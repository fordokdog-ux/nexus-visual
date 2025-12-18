package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class EventRender3D extends Event {

    @AllArgsConstructor
    @Getter
    public static class PreEntities extends Event {
        private final RenderTickCounter tickCounter;
        private final MatrixStack matrixStack;

        /** Удобный доступ к tickDelta */
        public float getTickDelta() {
            return tickCounter.getTickDelta(true);
        }

        /** Доступ к RenderTickCounter для совместимости */
        public RenderTickCounter getTickCounter() {
            return tickCounter;
        }

        /** Удобный доступ к MatrixStack */
        public MatrixStack getMatrices() {
            return matrixStack;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Game extends Event {
        private final RenderTickCounter tickCounter;
        private final MatrixStack matrixStack;

        /** Удобный доступ к tickDelta */
        public float getTickDelta() {
            return tickCounter.getTickDelta(true);
        }

        /** Доступ к RenderTickCounter для совместимости */
        public RenderTickCounter getTickCounter() {
            return tickCounter;
        }

        /** Удобный доступ к MatrixStack */
        public MatrixStack getMatrices() {
            return matrixStack;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class World extends Event {
        private final Camera camera;
        private final Matrix4f positionMatrix;
        private final RenderTickCounter tickCounter;

        /** Удобный доступ к tickDelta */
        public float getTickDelta() {
            return tickCounter.getTickDelta(true);
        }

        /** Доступ к RenderTickCounter для совместимости */
        public RenderTickCounter getTickCounter() {
            return tickCounter;
        }
    }
}