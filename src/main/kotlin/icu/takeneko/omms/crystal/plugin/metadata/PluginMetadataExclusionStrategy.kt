package icu.takeneko.omms.crystal.plugin.metadata

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import java.lang.module.ModuleDescriptor

object PluginMetadataExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipField(fieldAttributes: FieldAttributes): Boolean =
        fieldAttributes.name == "symbol" || fieldAttributes.name == "parsedVersion" || fieldAttributes.declaredClass == ModuleDescriptor.Version::class.java

    override fun shouldSkipClass(aClass: Class<*>?): Boolean = false
}