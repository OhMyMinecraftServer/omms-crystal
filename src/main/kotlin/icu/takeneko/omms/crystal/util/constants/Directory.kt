package icu.takeneko.omms.crystal.util.constants

import java.io.File
import kotlin.io.path.Path

object Directory {
    val workingDir by lazy {
        Path(File("").absolutePath)
    }
}
