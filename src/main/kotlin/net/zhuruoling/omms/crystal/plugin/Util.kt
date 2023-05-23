package net.zhuruoling.omms.crystal.plugin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.zhuruoling.omms.crystal.plugin.metadata.PluginMetadataExclusionStrategy
import java.util.regex.Pattern

val gsonForPluginMetadata: Gson = GsonBuilder()
    .addDeserializationExclusionStrategy(PluginMetadataExclusionStrategy)
    .addSerializationExclusionStrategy(PluginMetadataExclusionStrategy)
    .create()

val versionNamePattern: Pattern = Pattern.compile("([><=]=?)([0-9A-Za-z.]+)")
