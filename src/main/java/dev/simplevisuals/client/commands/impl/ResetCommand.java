package dev.simplevisuals.client.commands.impl;

import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.Setting;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.client.ui.hud.HudElement;
import net.minecraft.command.CommandSource;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.client.managers.ThemeManager;

public class ResetCommand extends Command {
    private static long lastRequestMs = 0L;
    private static final long CONFIRM_WINDOW_MS = 10_000L;
    public ResetCommand() {
        super("reset");
    }

    @Override
    public void execute(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            long now = System.currentTimeMillis();
            if (now - lastRequestMs > CONFIRM_WINDOW_MS) {
                lastRequestMs = now;
                ChatUtils.sendMessage(I18n.translate("cmd.reset.confirm"));
                return 1;
            }

            // сбрасываем префикс на точку (команда .reset всегда доступна через точку)
            NexusVisual.getInstance().getCommandManager().setPrefix(".");

            // Сбрасываем все модули
            for (Module module : NexusVisual.getInstance().getModuleManager().getModules()) {
                if (module.isToggled()) module.setToggled(false);
                for (Setting<?> setting : module.getSettings()) {
                    setting.reset();
                }
            }

            // Сбрасываем бинды к изначальным
            NexusVisual.getInstance().getModuleManager().resetBindsToDefaults();

            // Сбрасываем HUD
            for (HudElement hud : NexusVisual.getInstance().getHudManager().getHudElements()) {
                for (Setting<?> setting : hud.getSettings()) {
                    setting.reset();
                }
                if (!hud.isToggled()) hud.setToggled(true);
            }

            // Сбрасываем тему на дефолтную
            try {
                ThemeManager.getInstance().setTheme(new ThemeManager.LightTheme());
            } catch (Throwable ignored) {}

            ChatUtils.sendMessage(I18n.translate("cmd.reset.done"));

            try {
                NexusVisual.getInstance().getAutoSaveManager().forceSave();
            } catch (Throwable ignored) {}
            lastRequestMs = 0L;
            return 1;
        });
    }
}
