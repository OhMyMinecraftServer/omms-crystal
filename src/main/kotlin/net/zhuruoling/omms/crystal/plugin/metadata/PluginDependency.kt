package net.zhuruoling.omms.crystal.plugin.metadata

import java.lang.module.ModuleDescriptor

@JvmRecord
data class PluginDependency(val version: ModuleDescriptor.Version, val id: String)
