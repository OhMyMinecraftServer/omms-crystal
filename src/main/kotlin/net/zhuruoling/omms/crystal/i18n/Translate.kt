package net.zhuruoling.omms.crystal.i18n

data class TranslateKey(val lang:String, val namespace: String, val id: String){
    override fun toString(): String {
        return "$lang:$namespace:$id"
    }
}
