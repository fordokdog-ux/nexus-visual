package dev.simplevisuals.client.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.managers.FriendsManager;
import net.minecraft.command.CommandSource;

import java.awt.Desktop;
import java.io.File;
import dev.simplevisuals.client.ChatUtils;
import net.minecraft.client.resource.language.I18n;

public class FriendsCommand extends Command {

    public FriendsCommand() {
        super("friends");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .executes(ctx -> {
                    ChatUtils.sendMessage(I18n.translate("friends.help"));
                    return 1;
                })
                .then(literal("add").then(arg("nickname", com.mojang.brigadier.arguments.StringArgumentType.string()).executes(this::add)))
                .then(literal("remove").then(arg("nickname", com.mojang.brigadier.arguments.StringArgumentType.string()).executes(this::remove)))
                .then(literal("clear").executes(this::clear))
                .then(literal("list").executes(this::list))
                .then(literal("dir").executes(this::dir));
    }

    private int add(CommandContext<CommandSource> ctx) {
        String nickname = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nickname");
        if (FriendsManager.checkFriend(nickname)) {
            ChatUtils.sendMessage(I18n.translate("friends.alreadyFriend"));
            return 0;
        }
        FriendsManager.addFriend(nickname);
        ChatUtils.sendMessage(String.format(I18n.translate("friends.added"), nickname));
        return 1;
    }

    private int remove(CommandContext<CommandSource> ctx) {
        String nickname = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nickname");
        if (!FriendsManager.checkFriend(nickname)) {
            ChatUtils.sendMessage(I18n.translate("friends.notFound"));
            return 0;
        }
        FriendsManager.removeFriend(nickname);
        ChatUtils.sendMessage(String.format(I18n.translate("friends.removed"), nickname));
        return 1;
    }

    private int clear(CommandContext<CommandSource> ctx) {
        FriendsManager.clear();
        ChatUtils.sendMessage(I18n.translate("friends.cleared"));
        return 1;
    }

    private int list(CommandContext<CommandSource> ctx) {
        if (FriendsManager.getFriends().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("friends.empty"));
            return 1;
        }
        ChatUtils.sendMessage(I18n.translate("friends.header"));
        for (String f : FriendsManager.getFriends()) {
            ChatUtils.sendMessage(String.format(I18n.translate("friends.item"), f));
        }
        return 1;
    }

    private int dir(CommandContext<CommandSource> ctx) {
        File f = FriendsManager.getFriendsFile();
        if (f != null) {
            try {
                Desktop.getDesktop().open(f.getParentFile());
            } catch (Exception ignored) {}
        }
        return 1;
    }
} 