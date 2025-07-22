package icu.takeneko.omms.crystal.plugin.api;

import icu.takeneko.omms.crystal.main.CrystalServer;
import icu.takeneko.omms.crystal.server.ServerStatus;
import icu.takeneko.omms.crystal.server.ServerThreadDaemonKt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class ServerApi {
    public static void tell(String player, TextComponent text) {
        tellraw(player, GsonComponentSerializer.gson().serialize(text));
    }

    public static void tell(String player, Component component) {
        tellraw(player, GsonComponentSerializer.gson().serialize(component));
    }

    private static void tellraw(String player, String serialize) {
        if (ServerThreadDaemonKt.getServerStatus() != ServerStatus.RUNNING) {
            throw new IllegalStateException("Server is not running!");
        }
        if (CrystalServer.INSTANCE.getServerThreadDaemon() == null) {
            throw new IllegalStateException("Server Controller is null.");
        }
        CrystalServer.INSTANCE.getServerThreadDaemon().input("tellraw %s %s".formatted(player, serialize));
    }

    public static void executeCommand(String command) {
        if (CrystalServer.INSTANCE.getServerThreadDaemon() == null) {
            throw new IllegalStateException("Server Controller is null.");
        }
        CrystalServer.INSTANCE.getServerThreadDaemon().input(command);
    }
}
