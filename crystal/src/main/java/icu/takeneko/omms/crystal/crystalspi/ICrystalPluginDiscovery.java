package icu.takeneko.omms.crystal.crystalspi;

import icu.takeneko.omms.crystal.plugin.container.PluginContainer;

import java.util.List;

public interface ICrystalPluginDiscovery extends ICrystalService {
    List<PluginContainer> collectPlugins();

    default void bootstrap() {

    }
}
