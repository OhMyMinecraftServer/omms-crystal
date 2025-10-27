package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import net.kyori.adventure.text.TextComponent
import kotlin.reflect.KProperty
import com.mojang.brigadier.context.CommandContext as Ctx

typealias S = CommandSourceStack

fun CommandContext<S>.sendFeedback(component: TextComponent) {
    this.delegate.source.sendFeedback(component)
}

class LiteralCommand(root: String) {
    val node: LiteralArgumentBuilder<S> = LiteralArgumentBuilder.literal(root)

    operator fun String.invoke(function: LiteralCommand.() -> Unit) {
        node.then(LiteralCommand(this).apply(function).node)
    }
}

class ArgumentCommand<T>(name: String, argumentType: ArgumentType<T>) {
    val node: RequiredArgumentBuilder<S, T> =
        RequiredArgumentBuilder.argument(name, argumentType)
}


fun LiteralCommand(literal: String, function: LiteralCommand.() -> Unit): LiteralCommand {
    return LiteralCommand(literal).apply(function)
}

fun LiteralCommand.literal(literal: String, function: LiteralCommand.() -> Unit) {
    this.node.then(LiteralCommand(literal).apply(function).node)
}

fun LiteralCommand.requires(predicate: (S) -> Boolean, function: LiteralCommand.() -> Unit) {
    this.apply {
        node.requires(predicate)
        function(this)
    }
}

fun <T> ArgumentCommand<T>.requires(predicate: (S) -> Boolean, function: ArgumentCommand<T>.() -> Unit) {
    this.apply {
        node.requires(predicate)
        function(this)
    }
}

fun LiteralCommand.execute(function: CommandContext<S>.() -> Unit) {
    this.node.executes {
        CommandContext(it).function()
        return@executes Command.SINGLE_SUCCESS
    }
}

fun LiteralCommand.requires(function: S.() -> Boolean): LiteralCommand {
    this.node.requires(function)
    return this
}

fun <T> LiteralCommand.argument(name: String, argumentType: ArgumentType<T>, function: ArgumentCommand<T>.() -> Unit) {
    this.node.then(ArgumentCommand<T>(name, argumentType).apply(function).node)
}


fun <T> ArgumentCommand<T>.literal(literal: String, function: LiteralCommand.() -> Unit) {
    this.node.then(LiteralCommand(literal).apply(function).node)
}

fun <T> ArgumentCommand<T>.execute(function: CommandContext<S>.() -> Unit) {
    this.node.executes {
        CommandContext(it).function()
        return@executes Command.SINGLE_SUCCESS
    }
}

fun <T> ArgumentCommand<T>.requires(function: S.() -> Boolean): ArgumentCommand<T> {
    this.node.requires(function)
    return this
}

fun <T, K> ArgumentCommand<T>.argument(
    name: String,
    argumentType: ArgumentType<K>,
    function: ArgumentCommand<K>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, argumentType).apply(function).node)
}

fun <T> ArgumentCommand<T>.integerArgument(
    name: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    function: ArgumentCommand<Int>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, IntegerArgumentType.integer(min, max)).apply(function).node)
}

fun <T> ArgumentCommand<T>.floatArgument(
    name: String,
    min: Float = Float.MIN_VALUE,
    max: Float = Float.MAX_VALUE,
    function: ArgumentCommand<Float>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, FloatArgumentType.floatArg(min, max)).apply(function).node)
}

fun <T> ArgumentCommand<T>.wordArgument(
    name: String,
    function: ArgumentCommand<String>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, StringArgumentType.word()).apply(function).node)
}

fun <T> ArgumentCommand<T>.stringArgument(
    name: String,
    function: ArgumentCommand<String>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, StringArgumentType.string()).apply(function).node)
}

fun <T> ArgumentCommand<T>.greedyStringArgument(
    name: String,
    function: ArgumentCommand<String>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, StringArgumentType.greedyString()).apply(function).node)
}

fun <T> ArgumentCommand<T>.suggest(block: CommandContext<S>.(SuggestionsBuilder) -> Unit) {
    this.node.suggests { context, builder ->
        block(CommandContext(context), builder)
        builder.buildFuture()
    }
}

fun LiteralCommand.integerArgument(
    name: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    function: ArgumentCommand<Int>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, IntegerArgumentType.integer(min, max)).apply(function).node)
}

fun LiteralCommand.wordArgument(
    name: String,
    function: ArgumentCommand<String>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, StringArgumentType.word()).apply(function).node)
}

fun LiteralCommand.greedyStringArgument(
    name: String,
    function: ArgumentCommand<String>.() -> Unit
) {
    this.node.then(ArgumentCommand(name, StringArgumentType.greedyString()).apply(function).node)
}

class CommandContext<S>(
    val delegate: Ctx<S>
) {
    inline operator fun <reified T> getValue(thisRef: Any?, prop: KProperty<*>): T {
        if (T::class.java == Ctx::class.java) {
            return delegate as T
        }
        if (T::class.java == S::class.java) {
            return delegate.source as T
        }
        return delegate.getArgument<T>(prop.name, T::class.java)
    }
}
