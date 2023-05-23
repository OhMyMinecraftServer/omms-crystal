package net.zhuruoling.omms.crystal.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name


open class Manager<T, K>(
    private val scanFolder: String,
    private val fileNameFilter: (String) -> Boolean,
    private val beforeInit: (() -> Unit) = {},
    private val initializer: (String) -> Pair<T, K>,
    private val afterInit: ((Manager<T, K>) -> Unit)?
) {
    val map: MutableMap<T, K> = mutableMapOf()
    private val fileList = mutableListOf<String>()
    private val logger = createLogger("ManagerBase")
    fun init() {
        map.clear()
        fileList.clear()

        val folder = File(joinFilePaths(scanFolder))
        if (!folder.isDirectory || !folder.exists()) {
            Files.createDirectories(folder.toPath())
        }
        val files = (Files.list(Path.of(joinFilePaths(scanFolder)))).filter { fileNameFilter(it.name) }
        beforeInit()
        files.forEach {
            try {
                val pair = initializer(it.absolutePathString())
                map[pair.first] = pair.second
            } catch (e: Exception) {
                logger.error("Cannot execute `initializer` because an exception occurred.")
                e.printStackTrace()
            }
        }
        afterInit?.invoke(this)
    }
}