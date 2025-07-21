package icu.takeneko.omms.crystal.foundation

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

abstract class Manager<K: Keyable>(private val scanFolder: String) {
    val map: MutableMap<String, K> = mutableMapOf()
    private val fileList = mutableListOf<String>()

    fun init() {
        map.clear()
        fileList.clear()

        val folder = Path.of(scanFolder)
        if (!folder.isDirectory() || !folder.exists()) {
            Files.createDirectories(folder)
        }
        val files = Files.list(folder).filter(this::shouldAcceptFile).toList()
        files.forEach {
            val instance = createInstance(it)
            map[instance.key()] = instance
        }
    }

    abstract fun shouldAcceptFile(path: Path): Boolean

    abstract fun createInstance(path: Path):K
}