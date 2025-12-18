package dev.simplevisuals.util;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import net.minecraft.client.MinecraftClient;

public class DiscordRichPresenceUtil {
    private static final String DEFAULT_APP_ID = "1410931036136669206";

    private static volatile boolean enabled = false;
    private static volatile boolean running = false;
    private static volatile Thread rpcThread;
    private static volatile DiscordRpc rpc;

    private static volatile String lastDetails;
    private static volatile long lastUpdateMs;
    private static final long UPDATE_INTERVAL_MS = 5_000;

    public static String state;

    public static synchronized void discordrpc() {
        startDiscord(null);
    }

    public static synchronized void startDiscord(String applicationId) {
        if (enabled) return;
        enabled = true;
        String appId = applicationId;
        if (appId == null || appId.isEmpty()) {
            appId = System.getProperty("discord.app.id", System.getenv("DISCORD_APP_ID"));
        }
        if (appId == null || appId.isEmpty()) appId = DEFAULT_APP_ID;

        if (rpcThread != null && rpcThread.isAlive()) return;
        final String finalAppId = appId;
        rpcThread = new Thread(() -> runRpcLoop(finalAppId), "Discord-RPC-FirstDark-Thread");
        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    private static void runRpcLoop(String appId) {
        final DiscordRpc localRpc = new DiscordRpc();
        try {
            localRpc.setDebugMode(false);
        } catch (Throwable ignored) {}

        RPCEventHandler handler = new RPCEventHandler() {
            @Override
            public void ready(User user) {
                running = true;
                lastDetails = null;
                lastUpdateMs = 0L;
                pushPresence(localRpc);
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                running = false;
            }

            @Override
            public void errored(ErrorCode errorCode, String message) {
            }
        };

        try {
            // IMPORTANT: init() can be slow/blocking; keep it off the render thread.
            localRpc.init(appId, handler, false);
        } catch (Throwable t) {
            enabled = false;
            running = false;
            return;
        }

        rpc = localRpc;
        try {
            while (enabled && !Thread.currentThread().isInterrupted()) {
                try { localRpc.runCallbacks(); } catch (Throwable ignored) {}
                if (running) pushPresence(localRpc);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } finally {
            try { localRpc.shutdown(); } catch (Throwable ignored) {}
            if (rpc == localRpc) rpc = null;
            running = false;
            enabled = false;
        }
    }

    public static synchronized void shutdownDiscord() {
        enabled = false;
        running = false;

        Thread thread = rpcThread;
        rpcThread = null;
        if (thread != null) {
            thread.interrupt();
        }

        DiscordRpc localRpc = rpc;
        rpc = null;
        try { if (localRpc != null) localRpc.shutdown(); } catch (Throwable ignored) {}
    }

    private static void pushPresence(DiscordRpc localRpc) {
        if (localRpc == null) return;

        String details = computeDetails();
        long now = System.currentTimeMillis();
        if (details != null) {
            if (details.equals(lastDetails) && (now - lastUpdateMs) < UPDATE_INTERVAL_MS) return;
            lastDetails = details;
            lastUpdateMs = now;
        }

        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details(details)
                .largeImageText("Самый лучший пвп мод на 1.21.4 fabric")
                .smallImageText("Playing")
                .activityType(ActivityType.PLAYING)
            .button(DiscordRichPresence.RPCButton.of("Скачать", "https://discord.gg/PUYd4P9jdV"))
                .build();
        try { localRpc.updatePresence(presence); } catch (Throwable ignored) {}
    }

    private static String computeDetails() {
        try {
            if (state != null && !state.isEmpty()) return state;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) return "В меню";
            if (client.getCurrentServerEntry() != null) return "Сервер: " + client.getCurrentServerEntry().address;
            return "Одиночная игра";
        } catch (Throwable ignored) {
            return "В игре";
        }
    }
}
