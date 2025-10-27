package icu.takeneko.omms.crystal.util

import java.util.*

object BuildProperties {
    val map = mutableMapOf<String, String>()

    init {
        val bundle = ResourceBundle.getBundle("build")
        for (key in bundle.keys) {
            map[key] = bundle.getString(key)
        }
    }

    operator fun get(key: String): String? = map[key]

    fun forEach(function: (Map.Entry<String, String>) -> Unit) = map.forEach(function)
}
