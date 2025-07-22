package icu.takeneko.omms.crystal.i18n

import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap

class LanguageProviderImpl(
    private val languageId: String,
    private val translates: ConcurrentHashMap<TranslateKey, String>
) : LanguageProvider(languageId) {

    constructor(languageId: String) : this(languageId, ConcurrentHashMap())

    override fun translate(key: TranslateKey): String {
        if (!translates.containsKey(key)) return key.toString()
        return translates[key]!!
    }

    override fun translateFormatString(key: TranslateKey, vararg element: Any): String {
        if (!translates.containsKey(key)) return key.toString()
        return MessageFormat.format(translates[key]!!, *element)
    }

    override fun addTranslateKey(key: TranslateKey, value: String) {
        translates[key] = value
    }

    override fun getAllTranslates(): Map<TranslateKey, String> = translates

    override fun toString(): String = "LanguageProviderImpl(languageId=$languageId, translates=$translates)"
}
