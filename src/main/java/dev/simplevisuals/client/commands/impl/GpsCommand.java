package dev.simplevisuals.client.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.managers.WaypointManager;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.resource.language.I18n;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GpsCommand extends Command {

    public GpsCommand() {
        super("gps");
    }

    // Handler for: .gps add <name> <x> <z>
    private int addUnderAddNameFirst(CommandContext<CommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        double yFixed = 60.0;

        if (name == null || name.isBlank()) {
            name = String.format("%.0f %.0f %.0f", x, yFixed, z);
        }

        WaypointManager.add(name, new Vec3d(x + 0.5, yFixed, z + 0.5));
        ChatUtils.sendMessage(String.format(I18n.translate("gps.added"), name, (int) x, (int) yFixed, (int) z));
        return SINGLE_SUCCESS;
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .executes(ctx -> {
                    ChatUtils.sendMessage(I18n.translate("gps.help"));
                    return SINGLE_SUCCESS;
                })
                // New syntax: /gps <name> <x> <z>  (Y is forced to 60)
                .then(arg("name", StringArgumentType.word())
                        .then(arg("x", DoubleArgumentType.doubleArg())
                                .then(arg("z", DoubleArgumentType.doubleArg())
                                        .executes(this::addNameFirst)
                                )
                        )
                )
                .then(literal("add")
                        // New form: .gps add <name> <x> <z>  (Y forced to 60)
                        .then(arg("name", StringArgumentType.word())
                                .then(arg("x", DoubleArgumentType.doubleArg())
                                        .then(arg("z", DoubleArgumentType.doubleArg())
                                                .executes(this::addUnderAddNameFirst)
                                        )
                                )
                        )
                        .then(arg("x", DoubleArgumentType.doubleArg())
                                .then(arg("y", DoubleArgumentType.doubleArg())
                                        .then(arg("z", DoubleArgumentType.doubleArg())
                                                .executes(this::add)
                                                .then(arg("name", StringArgumentType.greedyString()).executes(this::add))
                                        )
                                )
                        )
                )
                .then(literal("list").executes(this::list))
                .then(literal("remove").then(arg("name", StringArgumentType.greedyString()).executes(this::remove)))
                .then(literal("clear").executes(this::clear));
    }

    private int add(CommandContext<CommandSource> ctx) {
        double x = DoubleArgumentType.getDouble(ctx, "x");
        // Parse Y to validate input, but we ignore its value and force to 60
        DoubleArgumentType.getDouble(ctx, "y");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        String name;
        try {
            name = StringArgumentType.getString(ctx, "name");
        } catch (IllegalArgumentException ignored) {
            name = null;
        }
        // Force Y to 60 regardless of input
        double yFixed = 60.0;
        if (name == null || name.isBlank()) name = String.format("%.0f %.0f %.0f", x, yFixed, z);

        WaypointManager.add(name, new Vec3d(x + 0.5, yFixed, z + 0.5));
        ChatUtils.sendMessage(String.format(I18n.translate("gps.added"), name, (int) x, (int) yFixed, (int) z));
        return SINGLE_SUCCESS;
    }

    // Handler for new syntax: /gps <name> <x> <z>
    private int addNameFirst(CommandContext<CommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        double yFixed = 60.0;

        if (name == null || name.isBlank()) {
            name = String.format("%.0f %.0f %.0f", x, yFixed, z);
        }

        WaypointManager.add(name, new Vec3d(x + 0.5, yFixed, z + 0.5));
        ChatUtils.sendMessage(String.format(I18n.translate("gps.added"), name, (int) x, (int) yFixed, (int) z));
        return SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSource> ctx) {
        var list = WaypointManager.list();
        if (list.isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("gps.empty"));
            return SINGLE_SUCCESS;
        }
        ChatUtils.sendMessage(I18n.translate("gps.header"));
        for (int i = 0; i < list.size(); i++) {
            var w = list.get(i);
            ChatUtils.sendMessage(String.format(I18n.translate("gps.item"), i + 1, w.name, w.pos.x, w.pos.y, w.pos.z));
        }
        return SINGLE_SUCCESS;
    }

    private int remove(CommandContext<CommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        boolean removed = WaypointManager.remove(name);
        if (removed) ChatUtils.sendMessage(String.format(I18n.translate("gps.removed"), name));
        else ChatUtils.sendMessage(String.format(I18n.translate("gps.notFound"), name));
        return SINGLE_SUCCESS;
    }

    private int clear(CommandContext<CommandSource> ctx) {
        WaypointManager.clear();
        ChatUtils.sendMessage(I18n.translate("gps.cleared"));
        return SINGLE_SUCCESS;
    }
}
