package icu.takeneko.omms.crystal.crystalspi;

import icu.takeneko.omms.crystal.foundation.ActionHost;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface ICrystalServerLauncher extends ICrystalService, ActionHost {
    void launchServer(@NotNull Path workingDir, @NotNull String launchCommand, @NotNull ICrystalServerInfoParser parser);

    void input(String line);

    void stopServer(ActionHost actionHost, boolean force);

    default void terminate(ActionHost actionHost) {
        this.stopServer(actionHost, true);
    }

    void destroy();

    boolean isServerRunning();
}
