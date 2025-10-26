package icu.takeneko.omms.crystal.crystalspi;

import icu.takeneko.omms.crystal.plugin.instance.PluginContainer;

import java.util.List;

public interface ICrystalPluginDiscovery extends ICrystalService {
    List<PluginContainer> collectPlugins();
}
