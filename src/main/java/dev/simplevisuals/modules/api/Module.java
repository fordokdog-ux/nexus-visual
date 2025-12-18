package dev.simplevisuals.modules.api;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.modules.settings.Setting;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.notify.Notify;
import dev.simplevisuals.client.util.notify.NotifyIcons;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.util.licensing.LicenseManager;

import java.util.ArrayList;
import java.util.List;

public abstract class Module implements Wrapper {
    private final String name, description;
    private final Category category;
    protected boolean toggled;
    private Bind bind = new Bind(-1, false);
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    // Temporary backward compatibility; prefer using the 3-arg ctor with explicit description
    public Module(String name, Category category) {
        this(name, category, name);
    }

    public void onEnable() {
        toggled = true;
        NexusVisual.getInstance().getEventHandler().subscribe(this);
        if (!fullNullCheck() && !name.equals("UI")) {
            String translatedName = I18n.translate(name);
            String msg = I18n.translate("notify.featureEnabled", translatedName);
            NexusVisual.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, msg, 1000));
        }
    }

    public void onDisable() {
        toggled = false;
        NexusVisual.getInstance().getEventHandler().unsubscribe(this);
        if (!fullNullCheck() && !name.equals("UI")) {
            String translatedName = I18n.translate(name);
            String msg = I18n.translate("notify.featureDisabled", translatedName);
            NexusVisual.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, msg, 1000));
        }
    }

    public void setToggled(boolean toggled) {
        if (toggled) {
            // License gate: allow only UI without activation
            if (!name.equals("UI") && !LicenseManager.isLicensed()) {
                try {
                    NexusVisual.getInstance().getNotifyManager().add(
                            new Notify(NotifyIcons.failIcon,
                                    "Нужна активация (" + LicenseManager.getStatus() + ")",
                                    2500)
                    );
                } catch (Throwable ignored) {}
                return;
            }
            onEnable();
        }
        else onDisable();
        // Планируем автосохранение после изменения состояния модуля
        try {
            dev.simplevisuals.client.managers.AutoSaveManager asm = NexusVisual.getInstance().getAutoSaveManager();
            if (asm != null) asm.scheduleAutoSave();
        } catch (Throwable ignored) {}
    }

    public void toggle() {
        setToggled(!toggled);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isToggled() {
        return toggled;
    }

    public Bind getBind() {
        return bind;
    }

    public void setBind(Bind bind) {
        this.bind = bind;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public static boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }
}