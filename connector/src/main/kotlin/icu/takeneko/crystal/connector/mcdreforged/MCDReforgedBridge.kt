package icu.takeneko.crystal.connector.mcdreforged

import icu.takeneko.omms.crystal.util.LoggerUtil
import org.slf4j.MarkerFactory

object MCDReforgedBridge {
    internal val logger = LoggerUtil.createLoggerWithPattern(
        "[%cyan(%d{yyyy-MM-dd HH:mm:ss.SSS})] [%boldYellow(MCDReforged)/%highlight(%level)]: %green(\\(%marker\\)) %msg%n",
        "MCDReforged"
    )

    fun log(name: String, level: Int, message: String, traceback: String?) {
        val realMessage = if (traceback != null) message + "\n" + traceback else message
        when (level) {
            0 -> logger.trace(MarkerFactory.getMarker(name), realMessage)
            10 -> logger.debug(MarkerFactory.getMarker(name), realMessage)
            20 -> logger.info(MarkerFactory.getMarker(name), realMessage)
            30 -> logger.warn(MarkerFactory.getMarker(name), realMessage)
            40 -> logger.error(MarkerFactory.getMarker(name), realMessage)
        }
    }
}