package icu.takeneko.omms.crystal.plugin.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Plugin(
    val id: String
)
