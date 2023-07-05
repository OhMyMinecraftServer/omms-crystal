package net.zhuruoling.omms.crystal.i18n

import java.util.*

fun addBuiltinTranslations() {
    ResourceBundle.getBundle("en_us").run {
        for (key in this.keys) {
            val value = this.getString(key)
            val lang = Identifier("en:us")
            val trKey = TranslateKey(lang, Identifier("crystal", key))
            TranslateManager.getOrCreateLanguageProvider(
                lang,
                impl = LanguageProviderImpl::class.java,
                lang,
                linkedMapOf<Identifier, TranslatableString>())
                .addTranslateKey(trKey,
                    TranslatableString(trKey, value)
                )
        }
    }
    ResourceBundle.getBundle("zh_cn").run {
        for (key in this.keys) {
            val value = this.getString(key)
            val lang = Identifier("zh:cn")
            val trKey = TranslateKey(lang, Identifier("crystal", key))
            TranslateManager.getOrCreateLanguageProvider(
                lang,
                impl = LanguageProviderImpl::class.java,
                lang,
                linkedMapOf<Identifier, TranslatableString>())
                .addTranslateKey(trKey,
                    TranslatableString(trKey, value)
                )
        }

    }
}