package icu.takeneko.omms.crystal.util.file

import com.charleskorn.kaml.AmbiguousQuoteStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import icu.takeneko.omms.crystal.util.constants.Directory
import kotlinx.serialization.json.Json
import java.io.File

object FileUtil {
    val YAML = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = true,
            strictMode = false,
            ambiguousQuoteStyle = AmbiguousQuoteStyle.DoubleQuoted
        )
    )

    val JSON = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun joinFilePaths(vararg pathComponent: String): String =
        buildString {
            append(Directory.workingDir)
            pathComponent.forEach {
                append(File.separator)
                append(it)
            }
        }
}
