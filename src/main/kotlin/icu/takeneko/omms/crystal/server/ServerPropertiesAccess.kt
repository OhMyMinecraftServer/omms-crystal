package icu.takeneko.omms.crystal.server

import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.util.joinFilePaths
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.util.Properties

object ServerPropertiesAccess {
    private var properties: Properties? = null

    private val file = File(joinFilePaths(Config.config.workingDir, "server.properties"))

    fun tryAccess(): Properties {
        return if (properties == null) {
            load()
            properties!!
        } else {
            properties!!
        }
    }

    fun load() {
        if (file.exists()) {
            file.reader(StandardCharsets.UTF_8).use {
                properties = Properties()
                synchronized(properties!!) {
                    properties!!.load(it)
                }
            }
        } else {
            throw FileNotFoundException("${file.absolutePath} not exist.")
        }
    }
}