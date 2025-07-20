package icu.takeneko.omms.crystal.text


import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

enum class Color {
    BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, RESET
}

enum class ClickAction {
    OPEN_URL, OPEN_FILE, RUN_COMMAND, SUGGEST_COMMAND, CHANGE_PAGE, COPY_TO_CLIPBOARD
}

enum class HoverAction {
    SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY
}

data class Entity(val name: String, val type: String, val uuid: String)

data class Item(val id: String, val count: Int, val tag: String)

data class ClickEvent(val action: ClickAction, val value: String)

data class HoverEvent(val action: HoverAction, val contents: Any? = null, val value: Any? = null)


@Deprecated("Replace Text with Adventure API", replaceWith = ReplaceWith("net.kyori.adventure.Component.text()"))
class Text constructor(
    private var text: String,
    private var extra: MutableList<Text>? = null,
    private var color: Color? = null,
    private var font: String? = null,
    private var bold: Boolean? = null,
    private var italic: Boolean? = null,
    private var underlined: Boolean? = null,
    private var strikethrough: Boolean? = null,
    private var obfuscated: Boolean? = null,
    private var insertion: String? = null,
    @SerializedName("clickEvent")
    private var clickEvent: ClickEvent? = null,
    @SerializedName("hoverEvent")
    private var hoverEvent: HoverEvent? = null,
) {
    constructor(text: String) : this(text, null)

    fun withColor(color: Color): Text {
        this.color = color
        return this
    }

    fun withFont(font: String?): Text {
        this.font = font
        return this
    }

    fun withBold(bold: Boolean?): Text {
        this.bold = bold
        return this
    }

    fun withItalic(italic: Boolean?): Text {
        this.italic = italic
        return this
    }

    fun withUnderlined(underlined: Boolean?): Text {
        this.underlined = underlined
        return this
    }

    fun withStrikethrough(strikethrough: Boolean?): Text {
        this.strikethrough = strikethrough
        return this
    }

    fun withObfuscated(obfuscated: Boolean?): Text {
        this.obfuscated = obfuscated
        return this
    }

    fun withInsertion(insertion: String?): Text {
        this.insertion = insertion
        return this
    }

    fun withClickEvent(clickEvent: ClickEvent?): Text {
        this.clickEvent = clickEvent
        return this
    }

    fun withHoverEvent(hoverEvent: HoverEvent?): Text {
        this.hoverEvent = hoverEvent
        return this
    }

    fun withExtra(extra: MutableList<Text>?): Text {
        this.extra = extra
        return this
    }

    fun toRawString(): String = text
}

@Deprecated("Replace TextGroup with Adventure API")
class TextGroup private constructor() {
    private var texts: MutableList<Text> = mutableListOf()
    fun getTexts(): MutableList<Text> = texts

    fun toRawString(): String {
        val res = StringBuilder()
        texts.forEach {
            res.append(it.toRawString())
        }

        return res.toString()
    }

    constructor(vararg text: Text) : this() {
        this.texts = mutableListOf(*text)
    }

}

object TextSerializer {
    private val gson = GsonBuilder().create()
    fun serialize(text: Text): String {
        return gson.toJson(text)
    }

    fun serialize(textGroup: TextGroup): String {
        return gson.toJson(textGroup.getTexts())
    }
}


