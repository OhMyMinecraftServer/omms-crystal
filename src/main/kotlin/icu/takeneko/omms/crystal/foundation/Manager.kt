package icu.takeneko.omms.crystal.foundation

import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.joinFilePaths
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

abstract class Manager<K: Keyable>(private val scanFolder: String) {
    val map: MutableMap<String, K> = mutableMapOf()
    private val fileList = mutableListOf<String>()
    private val logger = createLogger("ManagerBase")

    fun init() {
        map.clear()
        fileList.clear()

        val folder = Path.of(joinFilePaths(scanFolder))
        if (!folder.isDirectory() || !folder.exists()) {
            Files.createDirectories(folder)
        }
        val files = Files.list(folder).filter(this::shouldAcceptDefinitionFile).toList()
        files.forEach {
            val instance = deserialize(it)
            map[instance.key()] = instance
        }
    }

    abstract fun shouldAcceptDefinitionFile(path: Path): Boolean

    abstract fun deserialize(path: Path):K

    abstract fun
}