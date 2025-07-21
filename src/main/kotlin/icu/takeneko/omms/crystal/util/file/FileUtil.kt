package icu.takeneko.omms.crystal.util.file

import com.charleskorn.kaml.AmbiguousQuoteStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json

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
}