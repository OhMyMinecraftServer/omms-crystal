package icu.takeneko.omms.crystal.console

import icu.takeneko.omms.crystal.command.CommandManager
import icu.takeneko.omms.crystal.config.ConfigManager
import icu.takeneko.omms.crystal.main.CrystalServer.serverThreadDaemon
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import icu.takeneko.omms.crystal.util.command.CommandSource
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

class ConsoleHandler : Thread("ConsoleHandler") {
    private val terminal: Terminal = TerminalBuilder.builder().system(true).dumb(true).build()
    private lateinit var lineReader: LineReader

    @Synchronized
    fun reload() {
        lineReader = LineReaderBuilder.builder().terminal(terminal).completer(CommandManager.completer).build()
    }

    override fun run() {
        while (true) {
            try {
                val str = lineReader.readLine(">")
                if (str.startsWith(ConfigManager.config.commandPrefix)) {
                    try {
                        CommandManager.execute(str, CommandSourceStack(CommandSource.CONSOLE))
                    } catch (e: Exception) {
                        logger.error(e.message)
                    }
                } else {
                    serverThreadDaemon?.input(str)
                }
            } catch (_: Exception) {
                break
            }
        }
    }

    companion object {
        private val logger = createLogger("ConsoleHandler")
    }
}
