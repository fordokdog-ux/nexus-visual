package dev.simplevisuals.client.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.managers.ModuleManager;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.NexusVisual;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            ChatUtils.sendMessage(I18n.translate("bind.help"));
            return 1;
        });

        builder.then(literal("list").executes(this::list));

        builder.then(literal("reset").executes(ctx -> {
            NexusVisual.getInstance().getModuleManager().resetBindsToDefaults();
            ChatUtils.sendMessage(I18n.translate("bind.reset"));
            return 1;
        }));

        builder.then(literal("clear")
                .then(arg("module", StringArgumentType.greedyString())
                        .executes(this::clear)));

        builder.then(literal("set")
                .then(arg("module", StringArgumentType.greedyString())
                        .then(arg("key", StringArgumentType.word())
                                .executes(this::set))));
    }

    private int list(CommandContext<CommandSource> ctx) {
        ModuleManager mm = NexusVisual.getInstance().getModuleManager();
        ChatUtils.sendMessage(I18n.translate("bind.list.header"));
        for (Module m : mm.getModules()) {
            Bind b = m.getBind();
            if (b == null || b.getKey() < 0) continue;
            String keyName = bindToString(b);
            ChatUtils.sendMessage(String.format(I18n.translate("bind.list.item"), m.getName(), keyName));
        }
        return 1;
    }

    private int clear(CommandContext<CommandSource> ctx) {
        String moduleName = StringArgumentType.getString(ctx, "module");
        Module m = resolveModule(moduleName);
        if (m == null) {
            ChatUtils.sendMessage(String.format(I18n.translate("bind.invalidModule"), moduleName));
            return 0;
        }
        m.setBind(new Bind(-1, false));
        ChatUtils.sendMessage(String.format(I18n.translate("bind.cleared"), m.getName()));
        scheduleSave();
        return 1;
    }

    private int set(CommandContext<CommandSource> ctx) {
        String moduleName = StringArgumentType.getString(ctx, "module");
        String keyToken = StringArgumentType.getString(ctx, "key");
        Module m = resolveModule(moduleName);
        if (m == null) {
            ChatUtils.sendMessage(String.format(I18n.translate("bind.invalidModule"), moduleName));
            return 0;
        }
        Bind newBind = parseBindToken(keyToken);
        if (newBind == null) {
            ChatUtils.sendMessage(String.format(I18n.translate("bind.invalidKey"), keyToken));
            return 0;
        }
        m.setBind(newBind);
        ChatUtils.sendMessage(String.format(I18n.translate("bind.set"), m.getName(), bindToString(newBind)));
        scheduleSave();
        return 1;
    }

    private Module resolveModule(String name) {
        ModuleManager mm = NexusVisual.getInstance().getModuleManager();
        Module m = mm.getModuleByName(name);
        if (m != null) return m;
        // Try case-insensitive match
        for (Module mod : mm.getModules()) {
            if (mod.getName().equalsIgnoreCase(name)) return mod;
        }
        return null;
    }

    private Bind parseBindToken(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        // mouse buttons: mouse0/mouse1/mouse2 or mb0/mb1
        if (t.startsWith("mouse") || t.startsWith("mb")) {
            int idx = -1;
            try {
                idx = Integer.parseInt(t.replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
            if (idx >= 0 && idx <= 7) {
                return new Bind(idx, true);
            }
            return null;
        }
        // special: none
        if (t.equals("none") || t.equals("clear")) return new Bind(-1, false);

        // try resolve as GLFW key by name, e.g., R, LEFT_SHIFT, F5
        int key = keyNameToGlfw(token);
        if (key >= 0) return new Bind(key, false);
        return null;
    }

    private int keyNameToGlfw(String name) {
        String n = name.toUpperCase(Locale.ROOT);
        // Common aliases
        if (n.equals("RSHIFT")) n = "RIGHT_SHIFT";
        if (n.equals("LSHIFT")) n = "LEFT_SHIFT";
        if (n.equals("RCTRL")) n = "RIGHT_CONTROL";
        if (n.equals("LCTRL")) n = "LEFT_CONTROL";
        if (n.equals("RALT")) n = "RIGHT_ALT";
        if (n.equals("LALT")) n = "LEFT_ALT";
        if (n.length() == 1) {
            char c = n.charAt(0);
            if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A');
            if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
        }
        try {
            java.lang.reflect.Field f = GLFW.class.getField("GLFW_KEY_" + n);
            return f.getInt(null);
        } catch (Exception ignored) {}
        return -1;
    }

    private String bindToString(Bind b) {
        if (b == null || b.getKey() < 0) return I18n.translate("bind.none");
        if (b.isMouse()) return "MOUSE" + b.getKey();
        // Try map back common names
        return keyToName(b.getKey());
    }

    private String keyToName(int key) {
        // Letters
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        }
        // Digits
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        }
        // F-keys
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) {
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        }
        // Try reflect back to name
        try {
            for (java.lang.reflect.Field f : GLFW.class.getFields()) {
                if (!f.getName().startsWith("GLFW_KEY_")) continue;
                if (f.getInt(null) == key) return f.getName().substring("GLFW_KEY_".length());
            }
        } catch (Exception ignored) {}
        return String.valueOf(key);
    }

    private void scheduleSave() {
        try {
            NexusVisual.getInstance().getAutoSaveManager().scheduleAutoSave();
        } catch (Throwable ignored) {}
    }
}
