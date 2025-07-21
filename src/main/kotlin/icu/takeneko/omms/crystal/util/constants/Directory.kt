package icu.takeneko.omms.crystal.util.constants

import java.io.File
import kotlin.io.path.Path

object Directory {

    fun getWorkingDir(): String {
        val directory = File("")
        return directory.absolutePath
    }


    val WORKING_DIR = Path(getWorkingDir())
}