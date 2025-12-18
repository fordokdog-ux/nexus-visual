package dev.simplevisuals.client.managers;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.Setting;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.modules.settings.impl.StringSetting;
import dev.simplevisuals.modules.settings.impl.EnumSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.BindSetting;
import dev.simplevisuals.client.util.Wrapper;
import com.google.gson.*;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import dev.simplevisuals.client.managers.ThemeManager;
import net.minecraft.client.MinecraftClient;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ConfigManager implements Wrapper {
    
    private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
    private final Gson gson;
    private final File configsDir;
    private final File themesDir;
    private final File themesStoreFile;
    private final Map<String, ConfigData> configCache = new HashMap<>();
    
    public ConfigManager() {
        this.configsDir = new File(NexusVisual.getInstance().getGlobalsDir(), "configs");
        if (!this.configsDir.exists()) {
            this.configsDir.mkdirs();
        }
        this.themesDir = new File(NexusVisual.getInstance().getGlobalsDir(), "themes");
        if (!this.themesDir.exists()) {
            this.themesDir.mkdirs();
        }
        this.themesStoreFile = new File(this.themesDir, "themes.simplethemes");
        LOGGER.info("Путь к папке конфигураций: {}", this.configsDir.getAbsolutePath());
        LOGGER.info("Путь к папке тем: {}", this.themesDir.getAbsolutePath());
        
        // Настройка Gson для красивого форматирования
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create();

        // Load global custom themes (stored in simplevisuals/themes)
        try {
            migrateThemeStoreIfNeeded();
            loadThemeStore();
        } catch (Exception ignored) {}
    }

    public String getThemesDirectory() {
        return themesDir.getAbsolutePath();
    }

    public synchronized void saveThemeStore() {
        try {
            ThemeStoreData store = new ThemeStoreData();
            store.themes = new ArrayList<>();
            for (ThemeManager.CustomTheme ct : ThemeManager.getInstance().getCustomThemes()) {
                ThemeData td = new ThemeData();
                td.setName(ct.getName());
                td.setBackground(toHexArgb(ct.getBackgroundColor()));
                td.setSecondary(toHexArgb(ct.getSecondaryBackgroundColor()));
                td.setAccent(toHexArgb(ct.getAccentColor()));
                store.themes.add(td);
            }
            String json = gson.toJson(store);
            Files.writeString(themesStoreFile.toPath(), json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("Не удалось сохранить themes store: {}", e.getMessage());
        }
    }

    public synchronized void loadThemeStore() {
        try {
            if (!themesStoreFile.exists()) return;
            String json = Files.readString(themesStoreFile.toPath());
            ThemeStoreData store = parseThemeStoreJson(json, 0);
            List<ThemeManager.CustomTheme> restored = new ArrayList<>();
            if (store != null && store.themes != null) {
                for (ThemeData td : store.themes) {
                    if (td == null || td.getName() == null) continue;
                    Color bg = fromHexArgb(td.getBackground(), new Color(30, 30, 30, 175));
                    Color sec = fromHexArgb(td.getSecondary(), new Color(24, 24, 24, 175));
                    Color acc = fromHexArgb(td.getAccent(), new Color(255, 255, 255, 140));
                    restored.add(new ThemeManager.CustomTheme(td.getName(), bg, sec, acc));
                }
            }
            ThemeManager.getInstance().replaceCustomThemes(restored);
        } catch (Exception e) {
            LOGGER.warn("Не удалось загрузить themes store: {}", e.getMessage());
        }
    }

    private void migrateThemeStoreIfNeeded() {
        try {
            File oldStore = new File(this.configsDir, "themes.simplethemes");
            if (!oldStore.exists()) return;
            if (this.themesStoreFile.exists()) return;

            Files.copy(oldStore.toPath(), this.themesStoreFile.toPath());
            LOGGER.info("themes store перемещён в папку тем: {}", this.themesStoreFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.warn("Не удалось мигрировать themes store: {}", e.getMessage());
        }
    }

    private ThemeStoreData parseThemeStoreJson(String json, int depth) {
        if (json == null) return null;
        String trimmed = json.trim();
        if (trimmed.isEmpty()) return null;
        if (depth > 2) return null;

        try {
            // New format: { "themes": [...] }
            ThemeStoreData store = gson.fromJson(trimmed, ThemeStoreData.class);
            if (store != null) return store;
        } catch (Exception ignored) {}

        try {
            JsonElement el = JsonParser.parseString(trimmed);
            if (el.isJsonObject()) {
                return gson.fromJson(el, ThemeStoreData.class);
            }
            if (el.isJsonArray()) {
                ThemeData[] arr = gson.fromJson(el, ThemeData[].class);
                ThemeStoreData store = new ThemeStoreData();
                store.themes = new ArrayList<>();
                if (arr != null) {
                    for (ThemeData td : arr) store.themes.add(td);
                }
                return store;
            }
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                String inner = el.getAsString();
                return parseThemeStoreJson(inner, depth + 1);
            }
        } catch (Exception ignored) {}

        return null;
    }

    private List<ThemeManager.CustomTheme> mergeCustomThemesByName(List<ThemeManager.CustomTheme> base, List<ThemeManager.CustomTheme> incoming) {
        List<ThemeManager.CustomTheme> out = new ArrayList<>();
        if (base != null) out.addAll(base);
        if (incoming == null) return out;

        for (ThemeManager.CustomTheme in : incoming) {
            if (in == null || in.getName() == null) continue;
            ThemeManager.CustomTheme existing = null;
            for (ThemeManager.CustomTheme ct : out) {
                if (ct != null && in.getName().equals(ct.getName())) { existing = ct; break; }
            }
            if (existing == null) {
                out.add(in);
            } else {
                existing.setBackground(in.getBackgroundColor());
                existing.setSecondary(in.getSecondaryBackgroundColor());
                existing.setAccent(in.getAccentColor());
            }
        }
        return out;
    }
    
    /**
     * Сохраняет текущую конфигурацию в файл
     */
    public CompletableFuture<Boolean> saveConfig(String configName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ConfigData configData = new ConfigData();
                
                // Persist command prefix
                try {
                    String pref = NexusVisual.getInstance().getCommandManager().getPrefix();
                    configData.setCommandPrefix(pref);
                } catch (Exception ignored) {}
                
                // Сохраняем состояние всех модулей
                for (Module module : NexusVisual.getInstance().getModuleManager().getModules()) {
                    ModuleData moduleData = new ModuleData();
                    moduleData.setToggled(module.isToggled());
                    moduleData.setBind(module.getBind());
                    
                    // Сохраняем все настройки модуля
                    Map<String, Object> settings = new HashMap<>();
                    for (Setting<?> setting : module.getSettings()) {
                        Object value = setting.getValue();
                        
                        // Специальная обработка для разных типов настроек
                        if (setting instanceof ColorSetting) {
                            // Сохраняем цвет как hex строку
                            value = String.format("%06X", (Integer) value);
                        } else if (setting instanceof BindSetting) {
                            // Сохраняем бинд как строку "key:isMouse:mode"
                            dev.simplevisuals.modules.settings.api.Bind bind = (dev.simplevisuals.modules.settings.api.Bind) value;
                            String modeName = bind.getMode() != null ? bind.getMode().name() : dev.simplevisuals.modules.settings.api.Bind.Mode.TOGGLE.name();
                            value = bind.getKey() + ":" + bind.isMouse() + ":" + modeName;
                        } else if (setting instanceof ListSetting) {
                            // Сохраняем ListSetting как Map с именами и значениями
                            ListSetting listSetting = (ListSetting) setting;
                            Map<String, Boolean> listValues = new HashMap<>();
                            for (BooleanSetting boolSetting : listSetting.getValue()) {
                                listValues.put(boolSetting.getName(), boolSetting.getValue());
                            }
                            value = listValues;
                        }
                        
                        settings.put(setting.getName(), value);
                    }
                    moduleData.setSettings(settings);
                    
                    configData.getModules().put(module.getName(), moduleData);
                }
                
                // Сохраняем выбранную тему
                configData.setCurrentTheme(ThemeManager.getInstance().getCurrentTheme().getName());

                // Custom themes are stored globally in themes.simplethemes (not per-config)
                
                // Сохраняем положение HUD элементов
                Map<String, HudPositionData> hudPositions = new HashMap<>();
                for (HudElement hudElement : NexusVisual.getInstance().getHudManager().getHudElements()) {
                    HudPositionData hudData = new HudPositionData();
                    hudData.setX(hudElement.getPosition().getValue().getX());
                    hudData.setY(hudElement.getPosition().getValue().getY());
                    try {
                        // Determine enabled state from HudManager.elements ListSetting if available
                        dev.simplevisuals.modules.settings.impl.ListSetting elementsList = NexusVisual.getInstance().getHudManager().getElements();
                        dev.simplevisuals.modules.settings.impl.BooleanSetting bs = elementsList.getName(hudElement.getName());
                        boolean enabled = bs != null ? bs.getValue() : hudElement.isToggled();
                        hudData.setEnabled(enabled);
                    } catch (Exception ignored) {
                        hudData.setEnabled(hudElement.isToggled());
                    }
                    hudPositions.put(hudElement.getName(), hudData);
                }
                configData.setHudPositions(hudPositions);
                
                // Сохраняем настройки HUD-элементов
                Map<String, Map<String, Object>> hudSettings = new HashMap<>();
                for (HudElement hudElement : NexusVisual.getInstance().getHudManager().getHudElements()) {
                    Map<String, Object> settings = new HashMap<>();
                    for (Setting<?> setting : hudElement.getSettings()) {
                        Object value = setting.getValue();
                        if (setting instanceof ColorSetting) {
                            value = String.format("%06X", (Integer) value);
                        } else if (setting instanceof ListSetting) {
                            ListSetting listSetting = (ListSetting) setting;
                            Map<String, Boolean> listValues = new HashMap<>();
                            for (BooleanSetting boolSetting : listSetting.getValue()) {
                                listValues.put(boolSetting.getName(), boolSetting.getValue());
                            }
                            value = listValues;
                        } else if (setting instanceof BindSetting) {
                            dev.simplevisuals.modules.settings.api.Bind bind = (dev.simplevisuals.modules.settings.api.Bind) value;
                            String modeName = bind.getMode() != null ? bind.getMode().name() : dev.simplevisuals.modules.settings.api.Bind.Mode.TOGGLE.name();
                            value = bind.getKey() + ":" + bind.isMouse() + ":" + modeName;
                        }
                        settings.put(setting.getName(), value);
                    }
                    hudSettings.put(hudElement.getName(), settings);
                }
                configData.setHudSettings(hudSettings);
                
                // Сохраняем в файл
                File configFile = new File(configsDir, configName + ".simple");
                String json = gson.toJson(configData);
                Files.write(configFile.toPath(), json.getBytes(StandardCharsets.UTF_8));

                // Keep global themes store in sync
                try { saveThemeStore(); } catch (Exception ignored) {}
                
                // Кэшируем конфигурацию
                configCache.put(configName, configData);
                
                LOGGER.info("Конфигурация '{}' успешно сохранена", configName);
                return true;
                
            } catch (Exception e) {
                LOGGER.error("Ошибка при сохранении конфигурации '{}': {}", configName, e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Загружает конфигурацию из файла
     */
    public CompletableFuture<Boolean> loadConfig(String configName) {
        // 1) Чтение файла и парсинг в фоне
        return CompletableFuture.supplyAsync(() -> {
            try {
                File configFile = new File(configsDir, configName + ".simple");
                if (!configFile.exists()) {
                    LOGGER.error("Конфигурация '{}' не найдена", configName);
                    return null;
                }
                String json = Files.readString(configFile.toPath());
                return gson.fromJson(json, ConfigData.class);
            } catch (Exception e) {
                LOGGER.error("Ошибка при чтении конфигурации '{}': {}", configName, e.getMessage());
                return null;
            }
        }).thenCompose(configData -> {
            // 2) Применение строго на рендер-потоке
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            if (configData == null) {
                result.complete(false);
                return result;
            }
            MinecraftClient.getInstance().execute(() -> {
                try {
                    // Apply command prefix first (if present)
                    try {
                        String pref = configData.getCommandPrefix();
                        if (pref != null && !pref.isEmpty()) {
                            NexusVisual.getInstance().getCommandManager().setPrefix(pref);
                        }
                    } catch (Exception ignored) {}
                    // Применяем конфигурацию к модулям
                    for (Map.Entry<String, ModuleData> entry : configData.getModules().entrySet()) {
                        String moduleName = entry.getKey();
                        ModuleData moduleData = entry.getValue();
                        
                        Module module = NexusVisual.getInstance().getModuleManager().getModuleByName(moduleName);
                        if (module != null) {
                            // Применяем состояние модуля
                            if (moduleData.isToggled() != module.isToggled()) {
                                module.setToggled(moduleData.isToggled());
                            }
                            
                            // Применяем бинд
                            if (moduleData.getBind() != null) {
                                module.setBind(moduleData.getBind());
                            }
                            
                            // Применяем настройки
                            for (Map.Entry<String, Object> settingEntry : moduleData.getSettings().entrySet()) {
                                String settingName = settingEntry.getKey();
                                Object value = settingEntry.getValue();
                                
                                Setting<?> setting = module.getSettings().stream()
                                        .filter(s -> s.getName().equals(settingName))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (setting != null) {
                                    try {
                                        // Безопасно устанавливаем значение
                                        setSettingValue(setting, value);
                                    } catch (Exception e) {
                                        LOGGER.warn("Не удалось применить настройку {} для модуля {}: {}", 
                                                settingName, moduleName, e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                    
                    // Применяем выбранную тему
                    try {
                        ThemeManager themeManager = ThemeManager.getInstance();

                        // 1) Load global themes store (source of truth)
                        boolean storeExisted = themesStoreFile.exists();
                        try { loadThemeStore(); } catch (Exception ignored) {}

                        // 2) Legacy migration: if the store file doesn't exist yet, import themes from config once.
                        if (!storeExisted) {
                            try {
                                List<ThemeManager.CustomTheme> fromConfig = new ArrayList<>();
                                if (configData.getCustomThemes() != null) {
                                    for (ThemeData td : configData.getCustomThemes()) {
                                        if (td == null || td.getName() == null) continue;
                                        Color bg = fromHexArgb(td.getBackground(), new Color(30, 30, 30, 175));
                                        Color sec = fromHexArgb(td.getSecondary(), new Color(24, 24, 24, 175));
                                        Color acc = fromHexArgb(td.getAccent(), new Color(255, 255, 255, 140));
                                        fromConfig.add(new ThemeManager.CustomTheme(td.getName(), bg, sec, acc));
                                    }
                                }
                                if (!fromConfig.isEmpty()) {
                                    themeManager.replaceCustomThemes(fromConfig);
                                    saveThemeStore();
                                }
                            } catch (Exception ignored) {}
                        }

                        ThemeManager.Theme theme = themeManager.findThemeByName(configData.getCurrentTheme());
                        if (theme != null) {
                            themeManager.setTheme(theme);
                            NexusVisual.getInstance().getEventHandler().post(new dev.simplevisuals.client.events.impl.EventThemeChanged(theme));
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Не удалось применить тему {}: {}", configData.getCurrentTheme(), e.getMessage());
                    }
                    
                    // Применяем положение HUD элементов
                    if (configData.getHudPositions() != null) {
                        try {
                            for (Map.Entry<String, HudPositionData> entry : configData.getHudPositions().entrySet()) {
                                String hudName = entry.getKey();
                                HudPositionData hudData = entry.getValue();
                                
                                HudElement hudElement = NexusVisual.getInstance().getHudManager().getHudElements().stream()
                                        .filter(element -> element.getName().equals(hudName))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (hudElement != null) {
                                    hudElement.getPosition().getValue().setX(hudData.getX());
                                    hudElement.getPosition().getValue().setY(hudData.getY());
                                    if (hudData.isEnabled() != hudElement.isToggled()) {
                                        hudElement.setToggled(hudData.isEnabled());
                                    }
                                    // Reflect enabled state back into HudManager.elements list for UI consistency
                                    try {
                                        dev.simplevisuals.modules.settings.impl.ListSetting elementsList = NexusVisual.getInstance().getHudManager().getElements();
                                        dev.simplevisuals.modules.settings.impl.BooleanSetting bs = elementsList.getName(hudName);
                                        if (bs != null && bs.getValue() != hudData.isEnabled()) {
                                            bs.setValue(hudData.isEnabled());
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Не удалось применить позиции HUD: {}", e.getMessage());
                        }
                    }
                    
                    // Применяем настройки HUD-элементов
                    if (configData.getHudSettings() != null) {
                        for (Map.Entry<String, Map<String, Object>> entry : configData.getHudSettings().entrySet()) {
                            String hudName = entry.getKey();
                            Map<String, Object> settings = entry.getValue();
                            HudElement hudElement = NexusVisual.getInstance().getHudManager().getHudElements().stream()
                                    .filter(element -> element.getName().equals(hudName))
                                    .findFirst()
                                    .orElse(null);
                            if (hudElement != null) {
                                for (Map.Entry<String, Object> settingEntry : settings.entrySet()) {
                                    String settingName = settingEntry.getKey();
                                    Object value = settingEntry.getValue();
                                    Setting<?> setting = hudElement.getSettings().stream()
                                            .filter(s -> s.getName().equals(settingName))
                                            .findFirst()
                                            .orElse(null);
                                    if (setting != null) {
                                        try {
                                            setSettingValue(setting, value);
                                        } catch (Exception e) {
                                            LOGGER.warn("Не удалось применить настройку {} для HUD {}: {}", settingName, hudName, e.getMessage());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Кэшируем конфигурацию
                    configCache.put(configName, configData);
                    LOGGER.info("Конфигурация '{}' успешно загружена", configName);
                    result.complete(true);
                } catch (Exception e) {
                    LOGGER.error("Ошибка при применении конфигурации '{}': {}", configName, e.getMessage());
                    result.complete(false);
                }
            });
            return result;
        });
    }
    
    /**
     * Получает список всех доступных конфигураций
     */
    public String[] getConfigList() {
        File[] files = configsDir.listFiles((dir, name) -> name.endsWith(".simple"));
        if (files == null) return new String[0];
        
        String[] configs = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            configs[i] = files[i].getName().replace(".simple", "");
        }
        return configs;
    }
    
    /**
     * Получает путь к директории конфигураций
     */
    public String getConfigsDirectory() {
        return configsDir.getAbsolutePath();
    }
    
    /**
     * Удаляет конфигурацию
     */
    public boolean deleteConfig(String configName) {
        File configFile = new File(configsDir, configName + ".simple");
        if (configFile.exists()) {
            boolean deleted = configFile.delete();
            if (deleted) {
                configCache.remove(configName);
                LOGGER.info("Конфигурация '{}' удалена", configName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Проверяет существование конфигурации
     */
    public boolean configExists(String configName) {
        return new File(configsDir, configName + ".simple").exists();
    }
    
    /**
     * Безопасно устанавливает значение настройки
     */
    @SuppressWarnings("unchecked")
    private void setSettingValue(Setting<?> setting, Object value) {
        if (setting instanceof BooleanSetting) {
            if (value instanceof Boolean) {
                ((BooleanSetting) setting).setValue((Boolean) value);
            }
        } else if (setting instanceof NumberSetting) {
            if (value instanceof Number) {
                NumberSetting numberSetting = (NumberSetting) setting;
                float floatValue = ((Number) value).floatValue();
                if (floatValue >= numberSetting.getMin() && floatValue <= numberSetting.getMax()) {
                    numberSetting.setValue(floatValue);
                }
            }
        } else if (setting instanceof StringSetting) {
            if (value instanceof String) {
                ((StringSetting) setting).setValue((String) value);
            }
        } else if (setting instanceof EnumSetting) {
            if (value instanceof String) {
                EnumSetting<?> enumSetting = (EnumSetting<?>) setting;
                try {
                    // Используем метод setEnumValue для установки значения по строке
                    enumSetting.setEnumValue((String) value);
                } catch (Exception e) {
                    LOGGER.warn("Неверное значение enum: {}", value);
                }
            }
        } else if (setting instanceof ColorSetting) {
            if (value instanceof String) {
                try {
                    int color = Integer.parseInt((String) value, 16);
                    ((ColorSetting) setting).setValue(color);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Неверный формат цвета: {}", value);
                }
            } else if (value instanceof Number) {
                ((ColorSetting) setting).setValue(((Number) value).intValue());
            }
        } else if (setting instanceof ListSetting) {
            if (value instanceof Map) {
                ListSetting listSetting = (ListSetting) setting;
                @SuppressWarnings("unchecked")
                Map<String, Object> listValues = (Map<String, Object>) value;
                
                for (BooleanSetting boolSetting : listSetting.getValue()) {
                    Object savedValue = listValues.get(boolSetting.getName());
                    if (savedValue instanceof Boolean) {
                        boolSetting.setValue((Boolean) savedValue);
                    }
                }
            }
        } else if (setting instanceof BindSetting) {
            if (value instanceof String) {
                try {
                    String[] parts = ((String) value).split(":");
                    int key = Integer.parseInt(parts[0]);
                    boolean isMouse = parts.length > 1 && Boolean.parseBoolean(parts[1]);
                    dev.simplevisuals.modules.settings.api.Bind.Mode mode = dev.simplevisuals.modules.settings.api.Bind.Mode.TOGGLE;
                    if (parts.length > 2) {
                        try {
                            mode = dev.simplevisuals.modules.settings.api.Bind.Mode.valueOf(parts[2]);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    ((BindSetting) setting).setValue(new dev.simplevisuals.modules.settings.api.Bind(key, isMouse, mode));
                } catch (Exception e) {
                    LOGGER.warn("Неверный формат бинда: {}", value);
                }
            }
        }
    }
    
    /**
     * Классы для сериализации/десериализации
     */
    public static class ConfigData {
        private Map<String, ModuleData> modules = new HashMap<>();
        private String currentTheme;
        private List<ThemeData> customThemes = new ArrayList<>();
        private Map<String, HudPositionData> hudPositions = new HashMap<>();
        private Map<String, Map<String, Object>> hudSettings = new HashMap<>();
        private String commandPrefix;
        
        public Map<String, ModuleData> getModules() {
            return modules;
        }
        
        public void setModules(Map<String, ModuleData> modules) {
            this.modules = modules;
        }
        
        public String getCurrentTheme() {
            return currentTheme;
        }
        
        public void setCurrentTheme(String currentTheme) {
            this.currentTheme = currentTheme;
        }

        public List<ThemeData> getCustomThemes() {
            return customThemes;
        }

        public void setCustomThemes(List<ThemeData> customThemes) {
            this.customThemes = customThemes;
        }
        
        public Map<String, HudPositionData> getHudPositions() {
            return hudPositions;
        }
        
        public void setHudPositions(Map<String, HudPositionData> hudPositions) {
            this.hudPositions = hudPositions;
        }
        
        public Map<String, Map<String, Object>> getHudSettings() {
            return hudSettings;
        }
        
        public void setHudSettings(Map<String, Map<String, Object>> hudSettings) {
            this.hudSettings = hudSettings;
        }

        public String getCommandPrefix() {
            return commandPrefix;
        }

        public void setCommandPrefix(String commandPrefix) {
            this.commandPrefix = commandPrefix;
        }
    }

    public static class ThemeData {
        private String name;
        private String background;
        private String secondary;
        private String accent;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public String getSecondary() {
            return secondary;
        }

        public void setSecondary(String secondary) {
            this.secondary = secondary;
        }

        public String getAccent() {
            return accent;
        }

        public void setAccent(String accent) {
            this.accent = accent;
        }
    }

    /**
     * Global themes store file format (saved in simplevisuals/themes).
     */
    private static class ThemeStoreData {
        private List<ThemeData> themes = new ArrayList<>();
    }

    private static String toHexArgb(Color c) {
        if (c == null) return null;
        return String.format("%08X", c.getRGB());
    }

    private static Color fromHexArgb(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        try {
            int argb = (int) Long.parseLong(hex.trim(), 16);
            return new Color(argb, true);
        } catch (Exception ignored) {
            return fallback;
        }
    }
    
    public static class ModuleData {
        private boolean toggled;
        private dev.simplevisuals.modules.settings.api.Bind bind;
        private Map<String, Object> settings = new HashMap<>();
        
        public boolean isToggled() {
            return toggled;
        }
        
        public void setToggled(boolean toggled) {
            this.toggled = toggled;
        }
        
        public dev.simplevisuals.modules.settings.api.Bind getBind() {
            return bind;
        }
        
        public void setBind(dev.simplevisuals.modules.settings.api.Bind bind) {
            this.bind = bind;
        }
        
        public Map<String, Object> getSettings() {
            return settings;
        }
        
        public void setSettings(Map<String, Object> settings) {
            this.settings = settings;
        }
    }
    
    public static class HudPositionData {
        private float x;
        private float y;
        private boolean enabled;
        
        public float getX() {
            return x;
        }
        
        public void setX(float x) {
            this.x = x;
        }
        
        public float getY() {
            return y;
        }
        
        public void setY(float y) {
            this.y = y;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
} 