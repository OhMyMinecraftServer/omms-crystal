package icu.takeneko.omms.crystal.server

import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.util.joinFilePaths
import java.io.File
import java.io.FileNotFoundException
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import java.util.Properties

object ServerPropertiesAccess {
    private var properties: Properties? = null

    fun tryAccess(): Properties {
        return if (properties == null) {
            load()
            properties!!
        } else {
            properties!!
        }
    }

    fun load(){
        File(joinFilePaths(Config.serverWorkingDirectory, "server.properties")).run {
            if (exists()) {
                reader(StandardCharsets.UTF_8).use {
                    properties = Properties()
                    synchronized(properties!!) {
                        properties!!.load(it)
                    }
                }
            } else {
                throw FileNotFoundException("${this.absolutePath} not exist.")
            }
        }
    }
}