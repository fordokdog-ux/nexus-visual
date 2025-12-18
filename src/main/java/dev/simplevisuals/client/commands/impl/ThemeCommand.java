package dev.simplevisuals.client.commands.impl;

import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.managers.ConfigManager;
import dev.simplevisuals.NexusVisual;
import net.minecraft.command.CommandSource;

public class ThemeCommand extends Command {

    public ThemeCommand() {
        super("theme");
    }

    @Override
    public void execute(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            showHelp();
            return 1;
        });

        builder.then(literal("dir")
                .executes(context -> {
                    showThemesDirectory();
                    return 1;
                }));
    }

    private void showHelp() {
        ChatUtils.sendMessage("§6=== Themes Help ===");
        ChatUtils.sendMessage("§e.theme dir §7- Открыть папку тем");
    }

    private void showThemesDirectory() {
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        String directory = configManager.getThemesDirectory();
        ChatUtils.sendMessage("§6Папка тем: §e" + directory);

        try {
            java.io.File dirFile = new java.io.File(directory);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            boolean opened = false;
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(dirFile);
                    opened = true;
                }
            }
            if (!opened) {
                String os = System.getProperty("os.name", "").toLowerCase();
                Process proc;
                if (os.contains("win")) {
                    proc = new ProcessBuilder("explorer.exe", dirFile.getAbsolutePath()).start();
                } else if (os.contains("mac")) {
                    proc = new ProcessBuilder("open", dirFile.getAbsolutePath()).start();
                } else {
                    proc = new ProcessBuilder("xdg-open", dirFile.getAbsolutePath()).start();
                }
                if (proc.isAlive() || proc.exitValue() == 0) {
                    opened = true;
                }
            }
            if (opened) {
                ChatUtils.sendMessage("§aПапка открыта в проводнике!");
            } else {
                ChatUtils.sendMessage("§cНе удалось открыть папку: Unknown error");
            }
        } catch (Exception e) {
            ChatUtils.sendMessage("§cНе удалось открыть папку: " + e.getMessage());
        }
    }
}
