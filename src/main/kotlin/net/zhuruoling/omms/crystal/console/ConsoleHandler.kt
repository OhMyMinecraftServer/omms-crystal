package net.zhuruoling.omms.crystal.console

import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.zhuruoling.omms.crystal.command.CommandManager
import net.zhuruoling.omms.crystal.command.CommandSource
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.main.SharedConstants.serverController
import net.zhuruoling.omms.crystal.util.createLogger
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

private val logger = createLogger("ConsoleHandler")

class ConsoleHandler : Thread("ConsoleHandler") {
    private val terminal: Terminal = TerminalBuilder.builder().system(true).dumb(true).build()
    private lateinit var lineReader: LineReader

    @Synchronized
    fun reload() {
        lineReader = LineReaderBuilder.builder().terminal(terminal).completer(CommandManager.completer()).build()
    }

    override fun run() {
        while (true) {
            try {
                reload()
                val str = lineReader.readLine()
                if (str.startsWith(Config.commandPrefix)) {
                    try {
                        CommandManager.execute(str, CommandSourceStack(CommandSource.CONSOLE))
                    } catch (e: Exception) {
                        logger.error(e.message)
                    }
                } else {
                    serverController?.input(str)
                }
            } catch (e: Exception) {
                break
            }
        }
    }
}