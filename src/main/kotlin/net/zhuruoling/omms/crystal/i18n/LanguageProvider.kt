package net.zhuruoling.omms.crystal.i18n

abstract class LanguageProvider(val lang: Identifier) {

    fun getLanguageId() = lang
    abstract fun translate(key: String): TranslatableString
    abstract fun translate(key: Identifier): TranslatableString
    abstract fun translate(key: TranslateKey): TranslatableString
    abstract fun translateFormatString(key: String, vararg element: Any): TranslatableString
    abstract fun translateFormatString(key: Identifier, vararg element: Any): TranslatableString
    abstract fun translateFormatString(key: TranslateKey, vararg element: Any): TranslatableString

    abstract fun addTranslateKey(key: TranslateKey, value: TranslatableString)
}