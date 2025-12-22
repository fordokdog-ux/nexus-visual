package dev.simplevisuals.client.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.managers.ConfigManager;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.NexusVisual;
import net.minecraft.command.CommandSource;

public class ConfigAliasCommand extends Command {
    
    public ConfigAliasCommand() {
        super("config");
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
                    ChatUtils.sendMessage("§cУкажите название");
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
        ChatUtils.sendMessage("§6=== Config Manager Help ===");
        ChatUtils.sendMessage("§e.config save <название> §7- Сохранить текущую конфигурацию");
        ChatUtils.sendMessage("§e.config load <название> §7- Загрузить конфигурацию");
        ChatUtils.sendMessage("§e.config list §7- Показать список всех конфигураций");
        ChatUtils.sendMessage("§e.config dir §7- Показать путь к папке конфигураций");
        ChatUtils.sendMessage("§e.config delete <название> §7- Удалить конфигурацию");
        ChatUtils.sendMessage("§e.config info <название> §7- Показать информацию о конфигурации");
        ChatUtils.sendMessage("§7Алиасы: §e.cfg §7(вместо .config)");
    }
    
    private void saveConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage("§cОшибка: Укажите название конфигурации");
            return;
        }
        
        ChatUtils.sendMessage("§aСохранение конфигурации '" + configName + "'...");
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        configManager.saveConfig(configName).thenAccept(success -> {
            if (success) {
                ChatUtils.sendMessage("§aКонфигурация '" + configName + "' успешно сохранена!");
            } else {
                ChatUtils.sendMessage("§cОшибка при сохранении конфигурации '" + configName + "'");
            }
        });
    }
    
    private void loadConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage("§cУкажите название");
            return;
        }
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        if (!configManager.configExists(configName)) {
            ChatUtils.sendMessage("§cКонфигурация '" + configName + "' не найдена");
            return;
        }
        
        ChatUtils.sendMessage("§aЗагрузка конфигурации '" + configName + "'...");
        
        configManager.loadConfig(configName).thenAccept(success -> {
            if (success) {
                ChatUtils.sendMessage("§aКонфигурация '" + configName + "' успешно загружена!");
            } else {
                ChatUtils.sendMessage("§cОшибка при загрузке конфигурации '" + configName + "'");
            }
        });
    }
    
    private void listConfigs() {
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        String[] configs = configManager.getConfigList();
        
        if (configs.length == 0) {
            ChatUtils.sendMessage("§eСписок конфигураций пуст");
            return;
        }
        
        ChatUtils.sendMessage("§6=== Доступные конфигурации ===");
        for (String config : configs) {
            ChatUtils.sendMessage("§7- §e" + config);
        }
        ChatUtils.sendMessage("§7Всего: §e" + configs.length + " §7конфигураций");
    }
    
    private void showConfigDirectory() {
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        String directory = configManager.getConfigsDirectory();
        ChatUtils.sendMessage("§6Папка конфигураций: §e" + directory);
        
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
                ChatUtils.sendMessage("§aПапка открыта в проводнике!");
            } else {
                ChatUtils.sendMessage("§cНе удалось открыть папку: Unknown error");
            }
        } catch (Exception e) {
            ChatUtils.sendMessage("§cНе удалось открыть папку: " + e.getMessage());
        }
    }
    
    private void deleteConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage("§cОшибка: Укажите название конфигурации");
            return;
        }
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        if (!configManager.configExists(configName)) {
            ChatUtils.sendMessage("§cКонфигурация '" + configName + "' не найдена");
            return;
        }
        
        boolean deleted = configManager.deleteConfig(configName);
        if (deleted) {
            ChatUtils.sendMessage("§aКонфигурация '" + configName + "' успешно удалена!");
        } else {
            ChatUtils.sendMessage("§cОшибка при удалении конфигурации '" + configName + "'");
        }
    }
    
    private void showConfigInfo(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            ChatUtils.sendMessage("§cОшибка: Укажите название конфигурации");
            return;
        }
        
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        if (!configManager.configExists(configName)) {
            ChatUtils.sendMessage("§cКонфигурация '" + configName + "' не найдена");
            return;
        }

        java.io.File newFile = new java.io.File(configManager.getConfigsDirectory(), configName + ".nexus");
        java.io.File oldFile = new java.io.File(configManager.getConfigsDirectory(), configName + ".simple");
        java.io.File actual = newFile.exists() ? newFile : (oldFile.exists() ? oldFile : newFile);
        
        ChatUtils.sendMessage("§6=== Информация о конфигурации '" + configName + "' ===");
        ChatUtils.sendMessage("§7Файл: §e" + actual.getName());
        ChatUtils.sendMessage("§7Путь: §e" + actual.getAbsolutePath());
        ChatUtils.sendMessage("§7Статус: §aСуществует");
    }
    
    private void suggestConfigs(SuggestionsBuilder suggestionsBuilder) {
        String[] configs = NexusVisual.getInstance().getConfigManager().getConfigList();
        for (String cfg : configs) {
            suggestionsBuilder.suggest(cfg);
        }
    }
    
} 