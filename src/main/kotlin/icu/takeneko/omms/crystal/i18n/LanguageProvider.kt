package icu.takeneko.omms.crystal.i18n

abstract class LanguageProvider(val lang: String) {
    fun getLanguageId() = lang
    abstract fun translate(key: TranslateKey): String
    abstract fun translateFormatString(key: TranslateKey, vararg element: Any): String
    abstract fun addTranslateKey(key: TranslateKey, value: String)

    abstract fun getAllTranslates(): Map<TranslateKey, String>
}