package net.zhuruoling.omms.crystal.i18n

data class TranslateKey(val lang:Identifier, val key: Identifier)
data class TranslatableString(val translateKey: TranslateKey, val translate: String)
