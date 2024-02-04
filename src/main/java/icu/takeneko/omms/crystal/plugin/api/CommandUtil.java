package icu.takeneko.omms.crystal.plugin.api;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import icu.takeneko.omms.crystal.command.CommandSourceStack;

public class CommandUtil {
    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Integer> integerArgument(String name, int min, int max) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer(min, max));
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Integer> integerArgument(String name) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer());
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> wordArgument(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.word());
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> greedyStringArgument(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.greedyString());
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Double> doubleArgument(String name) {
        return RequiredArgumentBuilder.argument(name, DoubleArgumentType.doubleArg());
    }

    public static String getString(CommandContext<CommandSourceStack> context, String name) {
        return StringArgumentType.getString(context, name);
    }

    public static Integer getInteger(CommandContext<CommandSourceStack> context, String name) {
        return IntegerArgumentType.getInteger(context, name);
    }

    public static Double getDouble(CommandContext<CommandSourceStack> context, String name) {
        return DoubleArgumentType.getDouble(context, name);
    }
}
