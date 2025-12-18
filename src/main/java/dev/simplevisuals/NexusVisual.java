package dev.simplevisuals;


import dev.simplevisuals.client.managers.*;
import dev.simplevisuals.client.ui.mainmenu.MainMenu;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import lombok.Getter;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.lang.invoke.MethodHandles;

import dev.simplevisuals.client.ui.activation.LicenseActivateScreen;
import dev.simplevisuals.util.licensing.LicenseManager;


@Getter
public class NexusVisual implements ModInitializer, Wrapper {

    public static final String MOD_ID = "simplevisuals";

    @Getter private static NexusVisual instance;

    private IEventBus eventHandler;
    private long initTime;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private AutoSaveManager autoSaveManager;
    private NotifyManager notifyManager;
    private PerformanceManager performanceManager;
    private ClickGui clickGui;
    private HudManager hudManager;
    private dev.simplevisuals.client.managers.AltManager altManager;
    private MainMenu mainmenu;
    private dev.simplevisuals.client.ui.hud.impl.WaypointOverlay waypointOverlay;

    public static Logger LOGGER = LogManager.getLogger(NexusVisual.class);

    // Keep the existing folder name to not break existing configs.
    private final File globalsDir = new File(mc.runDirectory, MOD_ID);
    private final File configsDir = new File(globalsDir, "configs");

    @Override
    public void onInitialize() {
        LOGGER.info("[Nexus Visual] Starting initialization.");
        initTime = System.currentTimeMillis();
        instance = this;

        createDirs(globalsDir, configsDir);

        // Licensing: local license.json verification + activation UI flow
        LicenseManager.init(globalsDir);

        eventHandler = new EventBus();

        eventHandler.registerLambdaFactory("dev.simplevisuals",
                (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
        );

        FriendsManager.init(globalsDir);
        AltManager.init(globalsDir);
        String lastAlt = AltManager.getLastUsedNickname();
        if (lastAlt != null && !lastAlt.isEmpty()) {
            AltManager.applyNickname(lastAlt);
        }

        notifyManager = new NotifyManager();
        performanceManager = new PerformanceManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        autoSaveManager = new AutoSaveManager();
        clickGui = new ClickGui();
        hudManager = new HudManager();
        mainmenu = new MainMenu();

        // Always-on waypoint overlay
        waypointOverlay = new dev.simplevisuals.client.ui.hud.impl.WaypointOverlay();
        eventHandler.subscribe(waypointOverlay);

        // Загружаем автоматически сохраненную конфигурацию
        autoSaveManager.loadAutoSave();

        // Регистрация события для замены TitleScreen на MainMenu
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Force activation flow until license becomes VALID.
            // This must also work while in-game (when currentScreen == null).
            try {
                var status = LicenseManager.getStatus();
                boolean licensed = status == LicenseManager.LicenseStatus.VALID;
                boolean onActivation = client.currentScreen instanceof LicenseActivateScreen;

                if (!licensed && !onActivation) {
                    client.setScreen(new LicenseActivateScreen(client.currentScreen));
                    return;
                }
            } catch (Throwable ignored) {}

            if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof MainMenu)) {
                client.setScreen(mainmenu);
            }
        });

        LOGGER.info("[Nexus Visual] Successfully initialized for {} ms.", System.currentTimeMillis() - initTime);
    }

    public static NexusVisual getInstance() {
        return instance;
    }

    public IEventBus getEventHandler() {
        return eventHandler;
    }

    public long getInitTime() {
        return initTime;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AutoSaveManager getAutoSaveManager() {
        return autoSaveManager;
    }

    public NotifyManager getNotifyManager() {
        return notifyManager;
    }

    public PerformanceManager getPerformanceManager() {
        return performanceManager;
    }

    public ClickGui getClickGui() {
        return clickGui;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public dev.simplevisuals.client.managers.AltManager getAltManager() {
        return altManager;
    }

    public MainMenu getMainmenu() {
        return mainmenu;
    }

    public dev.simplevisuals.client.ui.hud.impl.WaypointOverlay getWaypointOverlay() {
        return waypointOverlay;
    }

    private void createDirs(File... file) {
        for (File f : file) f.mkdirs();
    }

    public static Identifier id(String texture) {
        return Identifier.of(MOD_ID, texture);
    }
}
