package net.zhuruoling.omms.crystal.i18n

import net.zhuruoling.omms.crystal.main.SharedConstants

object TranslateManager {
    private val languageProviders = mutableMapOf<Identifier, LanguageProvider>()

    fun translate(key: TranslateKey): TranslatableString {
        val provider = languageProviders[key.lang] ?: return TranslatableString(key, key.key.toString())
        return provider.translate(key)
    }

    fun translateFormatString(key: TranslateKey, vararg element: Any): TranslatableString {
        val provider = languageProviders[key.lang] ?: return TranslatableString(key, key.key.toString())
        return provider.translateFormatString(key, *element)
    }

    fun addLanguageProvider(provider: LanguageProvider) {
        this.languageProviders[provider.getLanguageId()] = provider
    }
}

fun <R> withTranslateContext(namespace: String, func: TranslateContext.() -> R): R =
    func(TranslateContext(SharedConstants.language, namespace))

class TranslateContext(private val language: Identifier, private val namespace: String) {

    fun tr(t: String, vararg element: Any) = translate(t, element)
    fun translate(t: String, vararg element: Any): String =
        TranslateManager.translateFormatString(TranslateKey(language, Identifier(namespace, t)), *element).translate

}

