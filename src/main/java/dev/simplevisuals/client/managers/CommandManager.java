package dev.simplevisuals.client.managers;

import com.mojang.brigadier.CommandDispatcher;
import dev.simplevisuals.client.commands.Command;
import dev.simplevisuals.client.commands.impl.*;
import dev.simplevisuals.client.util.Wrapper;
import lombok.*;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager implements Wrapper {

    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
    private final CommandSource source = new ClientCommandSource(null, mc);
    private final List<Command> commands = new ArrayList<>();
    @Setter private String prefix = ".";

    public CommandManager() {
        addCommands(
                new ConfigCommand(),
                new ConfigAliasCommand(),
                new ThemeCommand(),
                new FriendsCommand(),
                new GpsCommand(),
                new PrefixCommand(),
                new BindCommand(),
                new ResetCommand()
        );
    }

    private void addCommands(Command... command) {
        for (Command cmd : command) {
            cmd.register(dispatcher);
            commands.add(cmd);
        }
    }
}