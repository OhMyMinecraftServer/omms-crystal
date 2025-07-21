package icu.takeneko.omms.crystal.util.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext

fun literal(literal: String): LiteralArgumentBuilder<CommandSourceStack> {
    return literal(literal)
}

private fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSourceStack, T> {
    return RequiredArgumentBuilder.argument(name, type)
}

fun integerArgument(name: String): RequiredArgumentBuilder<CommandSourceStack, Int> {
    return argument(name, IntegerArgumentType.integer())
}

fun wordArgument(name: String): RequiredArgumentBuilder<CommandSourceStack, String> {
    return argument(name, StringArgumentType.word())
}

fun greedyStringArgument(name: String): RequiredArgumentBuilder<CommandSourceStack, String> {
    return argument(name, StringArgumentType.greedyString())
}

fun getWord(context: CommandContext<CommandSourceStack>, name: String): String {
    return StringArgumentType.getString(context, name)
}

fun getInteger(context: CommandContext<CommandSourceStack>, name: String): Int {
    return IntegerArgumentType.getInteger(context, name)
}