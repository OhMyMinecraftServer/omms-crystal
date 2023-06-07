package net.zhuruoling.omms.crystal.i18n

interface LanguageProvider {
    fun getLanguageId(): Identifier
    fun translate(key: String): TranslatableString
    fun translate(key: Identifier): TranslatableString
    fun translate(key: TranslateKey): TranslatableString
    fun translateFormatString(key: String, vararg element: Any): TranslatableString
    fun translateFormatString(key: Identifier, vararg element: Any): TranslatableString
    fun translateFormatString(key: TranslateKey, vararg element: Any): TranslatableString
}