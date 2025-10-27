package icu.takeneko.omms.crystal.crystalspi;

import icu.takeneko.omms.crystal.parser.*;
import org.jetbrains.annotations.Nullable;

public interface ICrystalServerInfoParser extends ICrystalService {
    @Nullable
    Info parseToBareInfo(String raw);

    @Nullable
    ServerStartedInfo parseServerStartedInfo(String raw);

    @Nullable
    PlayerInfo parsePlayerInfo(String raw);

    @Nullable
    RconInfo parseRconStartInfo(String raw);

    @Nullable
    ServerOverloadInfo parseServerOverloadInfo(String raw);

    @Nullable
    ServerStartingInfo parseServerStartingInfo(String raw);

    @Nullable
    PlayerJoinInfo parsePlayerJoinInfo(String raw);

    @Nullable
    PlayerLeftInfo parsePlayerLeftInfo(String raw);

    @Nullable
    ServerStoppingInfo parseServerStoppingInfo(String raw);
}
