package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.event.ServerStartEvent
import icu.takeneko.omms.crystal.event.ServerStartEventArgs
import icu.takeneko.omms.crystal.event.ServerStopEvent
import icu.takeneko.omms.crystal.event.ServerStopEventArgs
import icu.takeneko.omms.crystal.i18n.withTranslateContext
import icu.takeneko.omms.crystal.main.SharedConstants
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.permission.comparePermission
import icu.takeneko.omms.crystal.permission.resolvePermissionLevel
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.text.Color
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.util.createLogger

private val logger = createLogger("Command")

fun literal(literal: String): LiteralArgumentBuilder<CommandSourceStack> {
    return LiteralArgumentBuilder.literal(literal)
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

val helpCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.config.commandPrefix + "help").then(
    greedyStringArgument("filter")
        .executes {
            val filter = getWord(it, "filter")
            CommandHelpManager.displayFiltered(it.source) {
                filter in this
            }
            1
        }
).executes {
    CommandHelpManager.displayAll(it.source)
    1
}


val permissionCommand: LiteralArgumentBuilder<CommandSourceStack> =
    literal(Config.config.commandPrefix + "permission").then(
        literal("set").then(
            wordArgument("player")
                .then(
                    wordArgument("permissionLevel").requires {
                        if (it.from == CommandSource.PLAYER)
                            comparePermission(it.permissionLevel!!, Permission.OWNER)
                        else
                            true
                    }.executes {
                        PermissionManager.setPermission(
                            getWord(it, "player"),
                            resolvePermissionLevel(getWord(it, "permissionLevel"))
                        )
                        1
                    }
                )
                .requires {
                    if (it.from == CommandSource.PLAYER)
                        comparePermission(it.permissionLevel!!, Permission.OWNER)
                    else
                        true
                }.executes {
                    PermissionManager.setPermission(getWord(it, "player"))
                    1
                }
        )
    ).then(
        literal("delete").then(
            wordArgument("player").requires {
                if (it.from == CommandSource.PLAYER)
                    comparePermission(it.permissionLevel!!, Permission.OWNER)
                else
                    true
            }.executes {
                PermissionManager.deletePlayer(getWord(it, "player"))

                1
            }
        )
    ).then(
        literal("list").executes {
            val permissionStorage = PermissionManager.convertToPermissionStorage()

            val textOwner = TextGroup(
                Text("  Owners: ").withColor(Color.LIGHT_PURPLE),
                Text(permissionStorage.owner.joinToString(separator = ", ")).withColor(Color.RESET)
            )
            val textAdmin = TextGroup(
                Text("  Admins: ").withColor(Color.YELLOW),
                Text(permissionStorage.admin.joinToString(separator = ", ")).withColor(Color.RESET)
            )
            val textUser = TextGroup(
                Text("  Users: ").withColor(Color.AQUA),
                Text(permissionStorage.user.joinToString(separator = ", ")).withColor(Color.RESET)
            )
            val textGuest = TextGroup(
                Text("  Guests: ").withColor(Color.BLUE),
                Text(permissionStorage.guest.joinToString(separator = ", ")).withColor(Color.RESET)
            )
            it.source.sendFeedback(Text("Permissions:"))
            it.source.sendFeedback(textOwner)
            it.source.sendFeedback(textAdmin)
            it.source.sendFeedback(textUser)
            it.source.sendFeedback(textGuest)
            1
        }
    ).then(
        literal("save").requires {
            if (it.from == CommandSource.PLAYER)
                comparePermission(it.permissionLevel!!, Permission.OWNER)
            else
                true
        }.executes {
            PermissionManager.writePermission()
            1
        }
    )

val startCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.config.commandPrefix + "start").requires {
    if (it.from == CommandSource.PLAYER)
        comparePermission(it.permissionLevel!!, Permission.ADMIN)
    else
        true
}.executes {
    SharedConstants.eventLoop.dispatch(
        ServerStartEvent,
        ServerStartEventArgs(Config.config.launchCommand, Config.config.workingDir)
    )
    1
}

val stopCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.config.commandPrefix + "stop").then(
    literal("force").requires {
        if (it.from == CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.ADMIN)
        else
            true
    }.executes {
        SharedConstants.eventLoop.dispatch(
            ServerStopEvent,
            ServerStopEventArgs(
                when (it.source.from) {
                    CommandSource.PLAYER -> it.source.player!!
                    CommandSource.PLUGIN -> "plugin"
                    CommandSource.REMOTE -> "central"
                    CommandSource.CONSOLE -> "console"
                },
                true
            )
        )
        1
    }
).requires {
    if (it.from == CommandSource.PLAYER)
        comparePermission(it.permissionLevel!!, Permission.ADMIN)
    else
        true
}.executes {
    SharedConstants.eventLoop.dispatch(
        ServerStopEvent,
        ServerStopEventArgs(
            when (it.source.from) {
                CommandSource.PLAYER -> it.source.player!!
                CommandSource.PLUGIN -> "plugin"
                CommandSource.REMOTE -> "central"
                CommandSource.CONSOLE -> "console"
            },
            false
        )
    )
    1
}

val executeCommand: LiteralArgumentBuilder<CommandSourceStack> =
    literal(Config.config.commandPrefix + "execute")
        .requires { it.from == CommandSource.CONSOLE || it.from == CommandSource.REMOTE }
        .then(
            literal("as").then(
                wordArgument("player")
            )
        )

val pluginCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.config.commandPrefix + "plugin")
//    .then(literal("load").then(wordArgument("plugin").requires {
//        if (it.from == CommandSource.PLAYER)
//            comparePermission(it.permissionLevel!!, Permission.ADMIN)
//        else
//            true
//    }.executes {
//        PluginManager.load(getWord(it,"plugin"))
//        1
//    }))
//    .then(literal("unload").then(wordArgument("plugin").requires {
//        if (it.from == CommandSource.PLAYER)
//            comparePermission(it.permissionLevel!!, Permission.ADMIN)
//        else
//            true
//    }.executes {
//        PluginManager.unload(getWord(it,"plugin"))
//        1
//    }))
    .then(literal("reload").then(wordArgument("plugin").requires {
        if (it.from == CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.ADMIN)
        else
            true
    }.executes {
        logger.warn("Plugin reloading is highly experimental, in some cases it can cause severe problems.")
        logger.warn("Reloading plugin ${getWord(it, "plugin")}!")
        PluginManager.reload(getWord(it, "plugin"))
        1
    }))
    .then(literal("reloadAll").requires {
        if (it.from == CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.ADMIN)
        else
            true
    }.executes {
        logger.warn("Plugin reloading is highly experimental, in some cases it can cause severe problems.")
        logger.warn("Reloading all plugins!")
        PluginManager.reloadAllPlugins()
        1
    })
//
//

private val commands = listOf(helpCommand, permissionCommand, startCommand, stopCommand, executeCommand, pluginCommand)


fun registerBuiltinCommandHelp() {
    val dispatcher = CommandDispatcher<CommandSourceStack>()
    commands.forEach(dispatcher::register)
    val usage = dispatcher.getAllUsage(
        dispatcher.root,
        CommandSourceStack(from = CommandSource.PLAYER, player = "", permissionLevel = Permission.OWNER),
        false
    )
    val help = usage.map {
        it to it.removePrefix(Config.config.commandPrefix).split(" ")
            .joinToString(separator = ".")
            .run { "help.$this" }
    }
    help.forEach { (cmd, help) ->
        CommandHelpManager.registerHelpMessage(cmd) {
            withTranslateContext("crystal") {
                tr(help)
            }
        }
    }
}