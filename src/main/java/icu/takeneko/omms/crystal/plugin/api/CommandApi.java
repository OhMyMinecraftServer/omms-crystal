package icu.takeneko.omms.crystal.plugin.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import icu.takeneko.omms.crystal.command.CommandManager;
import icu.takeneko.omms.crystal.util.command.CommandSourceStack;

public class CommandApi {
    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> command){
        CommandManager.INSTANCE.register(command);
    }
}
