package dev.simplevisuals.client.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.managers.ConfigManager;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.NexusVisual;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.client.resource.language.I18n;

import java.util.Arrays;

public class ConfigCommand extends Command {
    
    public ConfigCommand() {
        super("cfg");
    }
    
    @Override
    public void execute(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            showHelp();
            return 1;
        });
        
        // Команда save
        builder.then(literal("save")
                .then(arg("название", StringArgumentType.string())
                        .executes(context -> {
                            String configName = StringArgumentType.getString(context, "название");
                            saveConfig(configName);
                            return 1;
                        })));
        
        // Команда load
        builder.then(literal("load")
                .executes(context -> {
                    ChatUtils.sendMessage(I18n.translate("cfg.error.provideName"));
                    return 1;
                })
                .then(arg("название", StringArgumentType.string())
                        .suggests((ctx, suggestionsBuilder) -> {
                            suggestConfigs(suggestionsBuilder);
                            return suggestionsBuilder.buildFuture();
                        })
                        .executes(context -> {
                            String configName = StringArgumentType.getString(context, "название");
                            loadConfig(configName);
                            return 1;
                        })));
        
        // Команда list
        builder.then(literal("list")
                .executes(context -> {
                    listConfigs();
                    return 1;
                }));
        
        // Команда dir
        builder.then(literal("dir")
                .executes(context -> {
                    showConfigDirectory();
                    return 1;
                }));
        
        // Команда delete
        builder.then(literal("delete")
                .then(arg("название", StringArgumentType.string())
                        .executes(context -> {
                            String configName = StringArgumentType.getString(context, "название");
                            deleteConfig(configName);
                            return 1;
                        })));
        
        // Команда info
        builder.then(literal("info")
                .then(arg("название", StringArgumentType.string())
                        .executes(context -> {
                            String configName = StringArgumentType.getString(context, "название");
                            showConfigInfo(configName);
                            return 1;
                        })));
        
        
    }
    
    private void showHelp() {
        ChatUtils.sendMessage(I18n.translate("cfg.help.header"));
        ChatUtils.sendMessage(I18n.translate("cfg.help.save"));
        ChatUtils.sendMessage(I18n.translate("cfg.help.load"));
        ChatUtils.sendMessage(I18n.translate("cfg.help.list"));
        ChatUtils.sendMessage(I18n.translate("cfg.help.dir"));
        ChatUtils.sendMessage(I18n.translate("cfg.help.delete"));
        ChatUtils.sendMessage(I18n.translate("cfg.help.info"));
        ChatUtils.sendMessage(I18n.translate("cfg.help.aliases"));
    }
    
    private void saveConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("cfg.error.provideConfigName"));
            return;
        }
        
        ChatUtils.sendMessage(String.format(I18n.translate("cfg.saving"), configName));
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        configManager.saveConfig(configName).thenAccept(success -> {
            if (success) {
                ChatUtils.sendMessage(String.format(I18n.translate("cfg.saved"), configName));
            } else {
                ChatUtils.sendMessage(String.format(I18n.translate("cfg.saveError"), configName));
            }
        });
    }
    
    private void loadConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("cfg.error.provideName"));
            return;
        }
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        if (!configManager.configExists(configName)) {
            ChatUtils.sendMessage(String.format(I18n.translate("cfg.notFound"), configName));
            return;
        }
        
        ChatUtils.sendMessage(String.format(I18n.translate("cfg.loading"), configName));
        
        configManager.loadConfig(configName).thenAccept(success -> {
            if (success) {
                ChatUtils.sendMessage(String.format(I18n.translate("cfg.loaded"), configName));
            } else {
                ChatUtils.sendMessage(String.format(I18n.translate("cfg.loadError"), configName));
            }
        });
    }
    
    private void listConfigs() {
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        String[] configs = configManager.getConfigList();
        
        if (configs.length == 0) {
            ChatUtils.sendMessage(I18n.translate("cfg.list.empty"));
            return;
        }
        
        ChatUtils.sendMessage(I18n.translate("cfg.list.header"));
        for (String config : configs) {
            ChatUtils.sendMessage(String.format(I18n.translate("cfg.list.item"), config));
        }
        ChatUtils.sendMessage(String.format(I18n.translate("cfg.list.total"), configs.length));
    }
    
    private void showConfigDirectory() {
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        String directory = configManager.getConfigsDirectory();
        ChatUtils.sendMessage(String.format(I18n.translate("cfg.dir.path"), directory));
        
        try {
            // Открываем папку в проводнике
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
                ChatUtils.sendMessage(I18n.translate("cfg.dir.opened"));
            } else {
                ChatUtils.sendMessage(String.format(I18n.translate("cfg.dir.openError"), "Unknown error"));
            }
        } catch (Exception e) {
            ChatUtils.sendMessage(String.format(I18n.translate("cfg.dir.openError"), e.getMessage()));
        }
    }
    
    private void deleteConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("cfg.error.provideConfigName"));
            return;
        }
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        if (!configManager.configExists(configName)) {
            ChatUtils.sendMessage(String.format(I18n.translate("cfg.notFound"), configName));
            return;
        }
        
        boolean deleted = configManager.deleteConfig(configName);
        if (deleted) {
            ChatUtils.sendMessage(String.format(I18n.translate("cfg.deleted"), configName));
        } else {
            ChatUtils.sendMessage(String.format(I18n.translate("cfg.deleteError"), configName));
        }
    }
    
    private void showConfigInfo(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("cfg.error.provideConfigName"));
            return;
        }
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        if (!configManager.configExists(configName)) {
            ChatUtils.sendMessage(String.format(I18n.translate("cfg.notFound"), configName));
            return;
        }
        
        ChatUtils.sendMessage(String.format(I18n.translate("cfg.info.header"), configName));
        ChatUtils.sendMessage(String.format(I18n.translate("cfg.info.file"), configName));
        ChatUtils.sendMessage(String.format(I18n.translate("cfg.info.path"), configManager.getConfigsDirectory(), configName));
        ChatUtils.sendMessage(I18n.translate("cfg.info.exists"));
    }
    
    private void suggestConfigs(SuggestionsBuilder suggestionsBuilder) {
        String[] configs = NexusVisual.getInstance().getConfigManager().getConfigList();
        for (String cfg : configs) {
            suggestionsBuilder.suggest(cfg);
        }
    }
    
} 