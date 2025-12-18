package dev.simplevisuals.client.managers;

import net.minecraft.client.MinecraftClient;

/**
 * Centralized performance guard. Provides lightweight FPS-based throttling hints
 * that render utilities can use to reduce work on heavy frames.
 */
public final class PerformanceManager {

    private boolean globalThrottleEnabled = true;
    private int targetFps = 1000; // user baseline; modules will adapt downwards

    // Upper bound on how many collections Render3D should process per draw call.
    private int maxCollectionsPerPass = 4096;

    public boolean isGlobalThrottleEnabled() {
        return globalThrottleEnabled;
    }

    public void setGlobalThrottleEnabled(boolean enabled) {
        this.globalThrottleEnabled = enabled;
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setTargetFps(int targetFps) {
        this.targetFps = Math.max(30, targetFps);
    }

    public int getMaxCollectionsPerPass() {
        return maxCollectionsPerPass;
    }

    public void setMaxCollectionsPerPass(int maxCollectionsPerPass) {
        this.maxCollectionsPerPass = Math.max(512, maxCollectionsPerPass);
    }

    /**
     * Returns a sampling stride for collection rendering based on FPS.
     * 1 means render everything, 2 means every second element, etc.
     */
    public int getCollectionStride() {
        if (!globalThrottleEnabled) return 1;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return 1;
        int fps = mc.getCurrentFps();
        if (fps >= targetFps) return 1;
        if (fps >= targetFps * 0.85) return 2;
        if (fps >= targetFps * 0.7) return 3;
        if (fps >= targetFps * 0.5) return 4;
        return 6;
    }
}


