package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.event.server.StartServerEvent
import icu.takeneko.omms.crystal.event.server.StopServerEvent
import icu.takeneko.omms.crystal.i18n.withTranslateContext
import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.permission.isAtLeast
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.text.Color
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.util.command.CommandSource
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import icu.takeneko.omms.crystal.util.command.getWord
import icu.takeneko.omms.crystal.util.command.greedyStringArgument
import icu.takeneko.omms.crystal.util.command.literal
import icu.takeneko.omms.crystal.util.command.wordArgument
import icu.takeneko.omms.crystal.util.createLogger
import kotlin.io.path.Path

private val logger = createLogger("Command")

val helpCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.config.commandPrefix + "help").then(
    greedyStringArgument("filter")
        .executes {
            CommandHelpManager.displayFiltered(it.source) {
                getWord(it, "filter") in this
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
                        if (it.from == CommandSource.PLAYER) {
                            it.permissionLevel!!.isAtLeast(Permission.OWNER)
                        } else
                            true
                    }.executes {
                        PermissionManager[getWord(it, "player")] =
                            Permission.from(getWord(it, "permissionLevel"))
                        1
                    }
                )
                .requires {
                    if (it.from == CommandSource.PLAYER)
                        it.permissionLevel!!.isAtLeast(Permission.OWNER)
                    else
                        true
                }.executes {
                    PermissionManager[getWord(it, "player")] = PermissionManager.defaultLevel
                    1
                }
        )
    ).then(
        literal("delete").then(
            wordArgument("player").requires {
                if (it.from == CommandSource.PLAYER)
                    it.permissionLevel!!.isAtLeast(Permission.OWNER)
                else
                    true
            }.executes {
                PermissionManager.remove(getWord(it, "player"))

                1
            }
        )
    ).then(
        literal("list").executes {
            val permissionStorage = PermissionManager.getPermissionStorage()

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
                it.permissionLevel!!.isAtLeast(Permission.OWNER)
            else
                true
        }.executes {
            PermissionManager.save()
            1
        }
    )

val startCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.config.commandPrefix + "start").requires {
    if (it.from == CommandSource.PLAYER)
        it.permissionLevel!!.isAtLeast(Permission.ADMIN)
    else
        true
}.executes {
    CrystalServer.postEvent(
        StartServerEvent(Config.config.launchCommand, Path(Config.config.workingDirectory))
    )
    1
}

val stopCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.config.commandPrefix + "stop").then(
    literal("force").requires {
        if (it.from == CommandSource.PLAYER)
            it.permissionLevel!!.isAtLeast(Permission.ADMIN)
        else
            true
    }.executes {
        CrystalServer.postEvent(
            StopServerEvent(
                CrystalServer,
                true
            )
        )
        1
    }
).requires {
    if (it.from == CommandSource.PLAYER)
        it.permissionLevel!!.isAtLeast(Permission.ADMIN)
    else
        true
}.executes {
    CrystalServer.postEvent(
        StopServerEvent(
            CrystalServer,
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
            it.permissionLevel!!.isAtLeast(Permission.ADMIN)
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
            it.permissionLevel!!.isAtLeast(Permission.ADMIN)
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