package net.zhuruoling.omms.crystal.i18n

data class Identifier(val namespace: String, val value: String) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Identifier) return false
        if (this === other) return true
        return this.namespace == other.namespace || this.value == other.value
    }

    constructor(combined: String) : this(combined.split(":")[0], combined.split(":")[1])

    override fun hashCode(): Int {
        return namespace.hashCode() + value.hashCode()
    }

    override fun toString(): String {
        return "$namespace:$value"
    }
}