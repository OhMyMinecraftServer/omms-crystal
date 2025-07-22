package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import icu.takeneko.omms.crystal.config.ConfigManager
import icu.takeneko.omms.crystal.event.server.StartServerEvent
import icu.takeneko.omms.crystal.event.server.StopServerEvent
import icu.takeneko.omms.crystal.i18n.withTranslateContext
import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.permission.isAtLeast
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import icu.takeneko.omms.crystal.util.command.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import kotlin.io.path.Path

@Suppress("LoggingSimilarMessage")
object BuiltinCommand {

    private val logger = createLogger("Command")

    val helpCommand: LiteralArgumentBuilder<CommandSourceStack> =
        literal(ConfigManager.config.commandPrefix + "help").then(
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
        literal(ConfigManager.config.commandPrefix + "permission").then(
            literal("set").then(
                wordArgument("player")
                    .then(
                        wordArgument("permissionLevel").requires {
                            if (it.from == CommandSource.PLAYER) {
                                it.permissionLevel!!.isAtLeast(Permission.OWNER)
                            } else {
                                true
                            }
                        }.executes {
                            PermissionManager[getWord(it, "player")] =
                                Permission.from(getWord(it, "permissionLevel"))
                            1
                        }
                    )
                    .requires {
                        if (it.from == CommandSource.PLAYER) {
                            it.permissionLevel!!.isAtLeast(Permission.OWNER)
                        } else {
                            true
                        }
                    }.executes {
                        PermissionManager[getWord(it, "player")] = PermissionManager.defaultLevel
                        1
                    }
            )
        ).then(
            literal("delete").then(
                wordArgument("player").requires {
                    if (it.from == CommandSource.PLAYER) {
                        it.permissionLevel!!.isAtLeast(Permission.OWNER)
                    } else {
                        true
                    }
                }.executes {
                    PermissionManager.remove(getWord(it, "player"))

                    1
                }
            )
        ).then(
            literal("list").executes {
                val permissionStorage = PermissionManager.getPermissionStorage()

                val owner = Component.text().append(
                    Component.text("  Owners: ").style(Style.style(NamedTextColor.LIGHT_PURPLE)),
                    Component.text(permissionStorage.owner.joinToString(separator = ", "))
                ).build()

                val admin = Component.text().append(
                    Component.text("  Admins: ").style(Style.style(NamedTextColor.YELLOW)),
                    Component.text(permissionStorage.admin.joinToString(separator = ", "))
                ).build()

                val user = Component.text().append(
                    Component.text("  Users: ").style(Style.style(NamedTextColor.AQUA)),
                    Component.text(permissionStorage.user.joinToString(separator = ", "))
                ).build()

                val guest = Component.text().append(
                    Component.text("  Guests: ").style(Style.style(NamedTextColor.BLUE)),
                    Component.text(permissionStorage.guest.joinToString(separator = ", "))
                ).build()

                it.source.sendFeedback(Component.text("Permissions:"))
                it.source.sendFeedback(owner)
                it.source.sendFeedback(admin)
                it.source.sendFeedback(user)
                it.source.sendFeedback(guest)
                1
            }
        ).then(
            literal("save").requires {
                if (it.from == CommandSource.PLAYER) {
                    it.permissionLevel!!.isAtLeast(Permission.OWNER)
                } else {
                    true
                }
            }.executes {
                PermissionManager.save()
                1
            }
        )

    val startCommand: LiteralArgumentBuilder<CommandSourceStack> =
        literal(ConfigManager.config.commandPrefix + "start").requires {
            if (it.from == CommandSource.PLAYER) {
                it.permissionLevel!!.isAtLeast(Permission.ADMIN)
            } else {
                true
            }
        }.executes {
            CrystalServer.postEvent(
                StartServerEvent(ConfigManager.config.launchCommand, Path(ConfigManager.config.workingDirectory))
            )
            1
        }

    val stopCommand: LiteralArgumentBuilder<CommandSourceStack> =
        literal(ConfigManager.config.commandPrefix + "stop").then(
            literal("force").requires {
                if (it.from == CommandSource.PLAYER) {
                    it.permissionLevel!!.isAtLeast(Permission.ADMIN)
                } else {
                    true
                }
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
            if (it.from == CommandSource.PLAYER) {
                it.permissionLevel!!.isAtLeast(Permission.ADMIN)
            } else {
                true
            }
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
        literal(ConfigManager.config.commandPrefix + "execute")
            .requires { it.from == CommandSource.CONSOLE || it.from == CommandSource.REMOTE }
            .then(
                literal("as").then(
                    wordArgument("player")
                )
            )

    val pluginCommand: LiteralArgumentBuilder<CommandSourceStack> =
        literal(ConfigManager.config.commandPrefix + "plugin")
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
            .then(
                literal("reload").then(
                    wordArgument("plugin").requires {
                        if (it.from == CommandSource.PLAYER) {
                            it.permissionLevel!!.isAtLeast(Permission.ADMIN)
                        } else {
                            true
                        }
                    }.executes {
                        logger.warn("Plugin reloading is highly experimental, in some cases it can cause severe problems.")
                        logger.warn("Reloading plugin ${getWord(it, "plugin")}!")
                        PluginManager.reload(getWord(it, "plugin"))
                        1
                    }
                )
            )
            .then(
                literal("reloadAll").requires {
                    if (it.from == CommandSource.PLAYER) {
                        it.permissionLevel!!.isAtLeast(Permission.ADMIN)
                    } else {
                        true
                    }
                }.executes {
                    logger.warn("Plugin reloading is highly experimental, in some cases it can cause severe problems.")
                    logger.warn("Reloading all plugins!")
                    PluginManager.reloadAll()
                    1
                }
            )

    private val commands = listOf(
        helpCommand,
        permissionCommand,
        startCommand,
        stopCommand,
        executeCommand,
        pluginCommand
    )

    fun registerBuiltinCommands() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        commands.forEach(dispatcher::register)
        val usage = dispatcher.getAllUsage(
            dispatcher.root,
            CommandSourceStack(from = CommandSource.PLAYER, player = "", permissionLevel = Permission.OWNER),
            false
        )
        val help = usage.map {
            it to it.removePrefix(ConfigManager.config.commandPrefix).split(" ")
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
}
