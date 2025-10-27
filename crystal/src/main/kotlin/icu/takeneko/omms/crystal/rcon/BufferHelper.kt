package icu.takeneko.omms.crystal.rcon

import java.nio.charset.StandardCharsets
import kotlin.math.min

object BufferHelper {
    fun getString(buf: ByteArray, i: Int, j: Int): String {
        val k = j - 1
        var l: Int = min(i, k)
        while (0 != buf[l].toInt() && l < k) {
            ++l
        }
        return String(buf, i, l - i, StandardCharsets.UTF_8)
    }

    fun getIntLE(buf: ByteArray, start: Int): Int = getIntLE(buf, start, buf.size)

    @Suppress("UnnecessaryParentheses")
    fun getIntLE(buf: ByteArray, start: Int, limit: Int): Int =
        if (0 > limit - start - 4) {
            0
        } else {
            buf[start + 3].toInt() shl 24 or
                ((buf[start + 2].toInt() and 255) shl 16) or
                ((buf[start + 1].toInt() and 255) shl 8) or
                (buf[start].toInt() and 255)
        }
}
