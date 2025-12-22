package dev.simplevisuals.client.managers;

import dev.simplevisuals.client.events.impl.EventKey;
import dev.simplevisuals.client.events.impl.EventMouse;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.settings.Setting;
import dev.simplevisuals.modules.settings.impl.BindSetting;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.modules.impl.render.*;
import dev.simplevisuals.modules.impl.utility.*;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.NexusVisual;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ModuleManager implements Wrapper {

    private final List<Module> modules = new ArrayList<>();
    private final Map<Module, Bind> defaultBinds = new HashMap<>();

    public ModuleManager() {
        NexusVisual.getInstance().getEventHandler().subscribe(this);
        addModules(
                new NoRender(),
                new Fullbright(),
                new Crosshair(),
                new ViewModel(),
                new TargetEsp(),
                new AutoSprint(),
                new UI(),
                new AspectRatio(),
                new FTHelper(),
                new HitSound(),
                new AutoRespawn(),
                new CustomHitBox(),
                new NameProtect(),
                new ChinaHat(),
                new KillEffect(),
                new Optimization(),
                new TNTTimer(),
                new SkeletonEsp(),
                new PvpHelper(),
                new JumpCircle(),
                new ClientSound(),
                new TotemCounter(),
                new WorldParticles(),
                new DamageParticles(),
                new CustomFog(),
                new SwingAnimation(),
                new TimeChanger(),
                new ItemPhysic(),
                new FriendHelper(),
                new Predictions(),
                new FakePlayer(),
                new BlockOverlay(),
                new BetterMinecraft(),
                new Zoom(),
                new ShiftTap(),
                new Trails(),
                new HitBubbles(),
                new HitColor(),
                new DiscordRPCModule(),
                new Cape()
        );

        for (Module module : modules) {
            try {
                for (Field field : module.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Setting<?> setting = (Setting<?>) field.get(module);
                    if (setting != null && !module.getSettings().contains(setting)) module.getSettings().add(setting);
                }
            } catch (Exception ignored) {}
            // Capture default binds after module fully constructed
            defaultBinds.put(module, module.getBind());
        }
    }

    private void addModules(Module... module) {
        this.modules.addAll(List.of(module));
    }

    @EventHandler
    public void onKey(EventKey e) {
        // Do not toggle modules while any GUI screen is open
        if (mc.currentScreen != null) return;
        // Filter invalid key
        if (e.getKey() < 0) return;
        for (Module module : modules) {
            if (module.getBind().isMouse()) continue;
            if (module.getBind().getKey() < 0) continue;
            if (module.getBind().getKey() != e.getKey()) continue;
            switch (e.getAction()) {
                case GLFW.GLFW_PRESS -> {
                    if (module.getBind().getMode() == dev.simplevisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(true);
                    else module.toggle();
                }
                case GLFW.GLFW_RELEASE -> {
                    if (module.getBind().getMode() == dev.simplevisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(false);
                }
            }
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        // Do not toggle modules while any GUI screen is open
        if (mc.currentScreen != null) return;
        if (e.getButton() < 0) return;
        for (Module module : modules) {
            if (!module.getBind().isMouse()) continue;
            if (module.getBind().getKey() < 0) continue;
            if (module.getBind().getKey() != e.getButton()) continue;
            switch (e.getAction()) {
                case GLFW.GLFW_PRESS -> {
                    if (module.getBind().getMode() == dev.simplevisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(true);
                    else module.toggle();
                }
                case GLFW.GLFW_RELEASE -> {
                    if (module.getBind().getMode() == dev.simplevisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(false);
                }
            }
        }
    }

    public List<Module> getModules(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .sorted(java.util.Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Category> getCategories() {
        return Arrays.asList(Category.values());
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (!clazz.isInstance(module)) continue;
            return (T) module;
        }
        return null;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public void resetBindsToDefaults() {
        for (Module module : modules) {
            Bind def = defaultBinds.get(module);
            if (def == null) def = new Bind(-1, false);
            module.setBind(def);
        }
    }
}