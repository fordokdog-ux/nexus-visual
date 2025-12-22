package dev.simplevisuals.client.managers;

import dev.simplevisuals.client.events.impl.EventGameShutdown;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.NexusVisual;
import meteordevelopment.orbit.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import dev.simplevisuals.client.events.impl.EventSettingChange;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import dev.simplevisuals.util.licensing.LicenseManager;

public class AutoSaveManager implements Wrapper {
    
    private static final Logger LOGGER = LogManager.getLogger(AutoSaveManager.class);
    private static final String AUTO_SAVE_CONFIG_NAME = "autocfg";
    
    // Экстренное сохранение при аварийном завершении
    private final java.util.concurrent.atomic.AtomicBoolean emergencySaving = new java.util.concurrent.atomic.AtomicBoolean(false);
    private Thread shutdownHook;
    
    // Дебаунс-автосохранение при любых изменениях
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "SV-AutoSaveScheduler"));
    private ScheduledFuture<?> pendingSave;
    private final Object saveLock = new Object();
    
    // Блокируем автосохранение до завершения автозагрузки на старте
    private volatile boolean allowAutoSave = false;
    
    public AutoSaveManager() {
        // Подписываемся на события
        NexusVisual.getInstance().getEventHandler().subscribe(this);
        
        // Регистрируем обработчики экстренного завершения
        registerShutdownHook();
        registerUncaughtExceptionHandler();
    }
    
    @EventHandler
    public void onGameShutdown(EventGameShutdown event) {
        LOGGER.info("Автоматическое сохранение конфигурации...");
        
        // Сохраняем текущую конфигурацию
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).thenAccept(success -> {
            if (success) {
                LOGGER.info("Конфигурация автоматически сохранена как '{}'", AUTO_SAVE_CONFIG_NAME);
            } else {
                LOGGER.error("Ошибка при автоматическом сохранении конфигурации");
            }
        });
    }
    
    /**
     * Реагируем на любое изменение настройки и планируем автосохранение с дебаунсом
     */
    @EventHandler
    public void onSettingChanged(EventSettingChange event) {
        scheduleAutoSave();
    }
    
    /**
     * Планирует автосохранение с небольшим дебаунсом, чтобы не писать файл слишком часто
     */
    public void scheduleAutoSave() {
        if (!allowAutoSave) return; // блокируем автосохранения до завершения автозагрузки
        synchronized (saveLock) {
            if (pendingSave != null && !pendingSave.isDone()) {
                pendingSave.cancel(false);
            }
            pendingSave = scheduler.schedule(this::doAutoSave, 500, TimeUnit.MILLISECONDS);
        }
    }
    
    private void doAutoSave() {
        try {
            ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
            configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).thenAccept(success -> {
                if (!success) {
                    LOGGER.warn("Автосохранение не удалось");
                }
            });
        } catch (Throwable t) {
            LOGGER.warn("Ошибка при выполнении автосохранения", t);
        }
    }
    
    /**
     * Загружает автоматически сохраненную конфигурацию при запуске
     */
    public void loadAutoSave() {
        // Откладываем автозагрузку до первого тика рендер-потока, когда клиент полностью инициализирован
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private boolean attempted = false;
            @Override
            public void onEndTick(MinecraftClient client) {
                if (attempted) return;
                attempted = true;
                
                ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
                if (configManager.configExists(AUTO_SAVE_CONFIG_NAME)) {
                    LOGGER.info("Загрузка автоматически сохраненной конфигурации '{}'...", AUTO_SAVE_CONFIG_NAME);
                    configManager.loadConfig(AUTO_SAVE_CONFIG_NAME).thenAccept(success -> {
                        if (success) {
                            LOGGER.info("Автоматически сохраненная конфигурация загружена");
                        } else {
                            LOGGER.error("Ошибка при загрузке автоматически сохраненной конфигурации");
                        }
                        // Разрешаем автосохранение после попытки загрузки
                        allowAutoSave = true;
                    });
                } else {
                    // Конфига нет — просто разрешаем автосохранение
                    allowAutoSave = true;
                }
            }
        });
    }
    
    /**
     * Удаляет автоматически сохраненную конфигурацию
     */
    public void clearAutoSave() {
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        if (configManager.configExists(AUTO_SAVE_CONFIG_NAME)) {
            configManager.deleteConfig(AUTO_SAVE_CONFIG_NAME);
            LOGGER.info("Автоматически сохраненная конфигурация удалена");
        }
    }
    
    /**
     * Принудительно сохраняет текущую конфигурацию
     */
    public void forceSave() {
        LOGGER.info("Принудительное сохранение конфигурации...");
        ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
        try {
            boolean success = configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).join();
            if (success) LOGGER.info("Конфигурация принудительно сохранена");
            else LOGGER.error("Ошибка при принудительном сохранении конфигурации");
        } catch (Throwable t) {
            LOGGER.error("Исключение при принудительном сохранении конфигурации", t);
        }
    }
    
    // ---- Экстренное сохранение ----
    private void registerShutdownHook() {
        if (shutdownHook != null) return;
        shutdownHook = new Thread(() -> {
            emergencySave("JVM shutdown hook");
        }, "SV-AutoCfg-ShutdownHook");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (Throwable t) {
            LOGGER.warn("Не удалось зарегистрировать shutdown hook для автосохранения", t);
        }
    }
    
    private void registerUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                LOGGER.error("Необработанное исключение в потоке {}: {}", thread.getName(), throwable.toString());
                // Пытаемся сохранить конфиг синхронно перед падением
                emergencySave("Uncaught exception");
            });
        } catch (Throwable t) {
            LOGGER.warn("Не удалось установить обработчик необработанных исключений для автосохранения", t);
        }
    }
    
    private void emergencySave(String reason) {
        if (!emergencySaving.compareAndSet(false, true)) return;
        try {
            LOGGER.info("Экстренное сохранение конфигурации ({}).", reason);
            ConfigManager configManager = NexusVisual.getInstance().getConfigManager();
            // Блокирующее сохранение, чтобы успеть до завершения процесса
            configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).join();
            LOGGER.info("Экстренное сохранение завершено");
        } catch (Throwable t) {
            LOGGER.error("Ошибка при экстренном сохранении конфигурации", t);
        }
    }
} 