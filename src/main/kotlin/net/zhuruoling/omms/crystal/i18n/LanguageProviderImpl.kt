package net.zhuruoling.omms.crystal.i18n

import java.lang.IllegalArgumentException
import java.text.MessageFormat

class LanguageProviderImpl(
    private val languageId: Identifier,
    private val translates: LinkedHashMap<Identifier, TranslatableString>
) : LanguageProvider(languageId) {

    override fun translate(key: String) =
        Identifier(key).run {
            translates[this] ?: TranslatableString(TranslateKey(getLanguageId(), this), this.toString())
        }

    override fun translate(key: Identifier) =
        translates[key] ?: TranslatableString(TranslateKey(getLanguageId(), key), key.toString())

    override fun translate(key: TranslateKey): TranslatableString {
        if (this.languageId != key.lang) throw IllegalArgumentException("languageId not match!")
        return translates[key.key] ?: TranslatableString(TranslateKey(getLanguageId(), key.key), key.key.toString())
    }

    override fun translateFormatString(key: String, vararg element: Any) = translate(key).run {
        TranslatableString(this.translateKey, MessageFormat.format(translate, *element))
    }

    override fun translateFormatString(key: Identifier, vararg element: Any) = translate(key).run {
        TranslatableString(this.translateKey, MessageFormat.format(translate, *element))
    }

    override fun translateFormatString(key: TranslateKey, vararg element: Any) = translate(key).run {
        TranslatableString(this.translateKey, MessageFormat.format(translate, *element))
    }

    override fun addTranslateKey(key: TranslateKey, value: TranslatableString) {
        this.translates[key.key] = value
    }
}