package net.zhuruoling.omms.crystal.i18n

import java.util.*

fun addBuiltinTranslations() {
    ResourceBundle.getBundle("lang.en_us").run {
        for (key in this.keys) {
            val value = this.getString(key)
            val trKey = TranslateKey(Identifier("en", "us"), Identifier("crystal", key))
            TranslateManager.getOrCreateLanguageProvider(Identifier("en", "us"))
                .addTranslateKey(trKey,
                    TranslatableString(trKey, value)
                )
        }
    }
    ResourceBundle.getBundle("lang.zh_cn").run {
        for (key in this.keys) {
            val value = this.getString(key)
            val trKey = TranslateKey(Identifier("zh", "cn"), Identifier("crystal", key))
            TranslateManager.getOrCreateLanguageProvider(Identifier("zh", "cn"))
                .addTranslateKey(trKey,
                    TranslatableString(trKey, value)
                )
        }

    }
}