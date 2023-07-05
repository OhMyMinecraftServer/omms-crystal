package net.zhuruoling.omms.crystal.plugin.api;

import net.zhuruoling.omms.crystal.main.SharedConstants;
import net.zhuruoling.omms.crystal.server.ServerThreadDaemonKt;
import net.zhuruoling.omms.crystal.server.ServerStatus;
import net.zhuruoling.omms.crystal.text.Text;
import net.zhuruoling.omms.crystal.text.TextGroup;
import net.zhuruoling.omms.crystal.text.TextSerializer;

public class ServerApi {
    public static void tell(String player, Text text){
        tellraw(player, TextSerializer.INSTANCE.serialize(text));
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
