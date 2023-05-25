package net.zhuruoling.omms.crystal.plugin.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.zhuruoling.omms.crystal.command.CommandManager;
import net.zhuruoling.omms.crystal.command.CommandSourceStack;

public class CommandApi {
    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> command){
        CommandManager.INSTANCE.register(command);
    }
}
