package icu.takeneko.omms.crystal.i18n

data class TranslateKey(val lang: String, val namespace: String, val id: String) {
    override fun toString(): String = "$lang:$namespace:$id"
}
