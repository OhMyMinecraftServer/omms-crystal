package icu.takeneko.omms.crystal.server

import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.util.file.FileUtil.joinFilePaths
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.util.Properties

object ServerProperties {
    private val file = File(joinFilePaths(Config.config.workingDirectory, "server.properties"))

    private val lazyProps = lazy { loadFromDisk() }

    val properties: Properties
        get() = lazyProps.value

    fun reload() {
        lazyProps.takeIf { it.isInitialized() }?.let {
            // 已初始化过才需要重置
            synchronized(this) {
                lazyProps::class.java
                    .getDeclaredField("value")
                    .apply { isAccessible = true }
                    .set(lazyProps, null)
            }
        }
    }

    private fun loadFromDisk(): Properties {
        if (!file.exists()) throw FileNotFoundException("${file.absolutePath} does not exist")
        return Properties().apply { file.reader(StandardCharsets.UTF_8).use { load(it) } }
    }
}