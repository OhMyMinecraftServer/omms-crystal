package net.zhuruoling.omms.crystal.i18n

import net.zhuruoling.omms.crystal.main.SharedConstants
import java.util.*
import java.util.function.Function

val builtinTranslationLanguages = listOf("en_us","zh_cn")

object TranslateManager {
    private val languageProviders = mutableMapOf<String, LanguageProvider>()
    fun init(){
        addBuiltinTranslations()
    }
    fun translate(key: TranslateKey): String {
        val provider = languageProviders[key.lang] ?: return key.toString()
        return provider.translate(key)
    }

    fun translateFormatString(key: TranslateKey, vararg element: Any): String {
        val provider = languageProviders[key.lang] ?: return key.toString()
        return provider.translateFormatString(key, *element)
    }

    fun addLanguageProvider(provider: LanguageProvider) {
        this.languageProviders[provider.getLanguageId()] = provider
    }

    fun getOrCreateDefaultLanguageProvider(lang: String): LanguageProvider {
        if (lang in languageProviders) return languageProviders[lang]!!
        val provider = LanguageProviderImpl(lang)
        languageProviders += lang to provider
        return provider
    }

    fun addBuiltinTranslations() {
        for (lang in builtinTranslationLanguages){
            ResourceBundle.getBundle(lang).run {
                for (key in this.keys) {
                    val value = this.getString(key)
                    val trKey = TranslateKey(lang,"crystal", key)
                    getOrCreateDefaultLanguageProvider(lang).addTranslateKey(trKey, value)
                }
            }
        }
    }
}

fun <R> withTranslateContext(namespace: String, func: Function<TranslateContext,R>): R =
    func.apply(TranslateContext(SharedConstants.language, namespace))

fun <R> withTranslateContext(namespace: String, func: TranslateContext.() -> R): R =
    func(TranslateContext(SharedConstants.language, namespace))

class TranslateContext(private val language: String, private val namespace: String) {
    fun tr(t: String, vararg element: Any) = translate(t, *element)
    fun translate(k: String, vararg element: Any) =
        TranslateManager.translateFormatString(TranslateKey(language, namespace, k), *element)
}