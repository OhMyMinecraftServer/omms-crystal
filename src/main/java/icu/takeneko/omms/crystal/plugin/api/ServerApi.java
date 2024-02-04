package icu.takeneko.omms.crystal.plugin.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import icu.takeneko.omms.crystal.main.SharedConstants;
import icu.takeneko.omms.crystal.server.ServerThreadDaemonKt;
import icu.takeneko.omms.crystal.server.ServerStatus;
import icu.takeneko.omms.crystal.text.Text;
import icu.takeneko.omms.crystal.text.TextGroup;
import icu.takeneko.omms.crystal.text.TextSerializer;

public class ServerApi {
    public static void tell(String player, Text text){
        tellraw(player, TextSerializer.INSTANCE.serialize(text));
    }

    public static void tell(String player, Component component){
        tellraw(player, GsonComponentSerializer.gson().serialize(component));
    }

    private static void tellraw(String player, String serialize) {
        if (ServerThreadDaemonKt.getServerStatus() != ServerStatus.RUNNING){
            throw new IllegalStateException("Server is not running!");
        }
        if (SharedConstants.INSTANCE.getServerThreadDaemon() == null){
            throw new IllegalStateException("Server Controller is null.");
        }
        SharedConstants.INSTANCE.getServerThreadDaemon().input("tellraw %s %s".formatted(player, serialize));
    }

    public static void tell(String player, TextGroup textGroup){
        tellraw(player, TextSerializer.INSTANCE.serialize(textGroup));
    }

    public static void executeCommand(String command){
        if (SharedConstants.INSTANCE.getServerThreadDaemon() == null){
            throw new IllegalStateException("Server Controller is null.");
        }
        SharedConstants.INSTANCE.getServerThreadDaemon().input(command);
    }
}
